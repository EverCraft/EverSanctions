/*
 * This file is part of EverSanctions.
 *
 * EverSanctions is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EverSanctions is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EverSanctions.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.evercraft.eversanctions.service;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.Ban.Ip;
import org.spongepowered.api.util.ban.Ban.Profile;
import org.spongepowered.api.util.ban.BanTypes;

import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.java.UtilsMap;
import fr.evercraft.everapi.sponge.UtilsNetwork;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.subject.EIpSubject;
import fr.evercraft.eversanctions.service.subject.EUserSubject;

public class EBanService extends ESanctionService {
	
	private static final String UNKNOWN = "unknown";
	private static final Comparator<Ban> COMPARATOR_BAN = 
			(Ban o1, Ban o2) -> {
				if (o1.isIndefinite() && o2.isIndefinite()) {
					return 0;
				}
				
				if (o1.isIndefinite() && !o2.isIndefinite()) {
					return -1;
				}
				
				if (!o1.isIndefinite() && o2.isIndefinite()) {
					return 1;
				}
				return o2.getExpirationDate().get().compareTo(o1.getExpirationDate().get());
			};
	
	// Ordre décroissant
	private final ConcurrentSkipListSet<Ban.Profile> bans_profile;
	private final ConcurrentSkipListMap<Ban.Ip, Optional<UUID>> bans_ip;
	
	public EBanService(final EverSanctions plugin) {
		super(plugin);
		
		this.bans_profile = new ConcurrentSkipListSet<Ban.Profile>(EBanService.COMPARATOR_BAN);
		this.bans_ip = new ConcurrentSkipListMap<Ban.Ip, Optional<UUID>>(EBanService.COMPARATOR_BAN);
		
		this.reload();
	}
	
	public void reload() {
		this.bans_profile.clear();
		this.bans_ip.clear();
		
		Connection connection = null;
		try {
			connection = this.plugin.getDataBase().getConnection();
			
			this.bans_profile.addAll(this.plugin.getDataBase().getBansProfile(connection));
			this.bans_ip.putAll(this.plugin.getDataBase().getBansIp(connection));
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try { if (connection != null) connection.close(); } catch (SQLException e) {}
	    }
		
		super.reload();
	}
	
	@Override
	public Collection<? extends Ban> getBans() {
		return null;
	}


	@Override
	@SuppressWarnings("unchecked")
	public Collection<Profile> getProfileBans() {
		this.removeExpired();
		return (Collection<Profile>) this.bans_profile.stream().filter(ban -> ban.getType().equals(BanTypes.PROFILE));
	}


	@Override
	@SuppressWarnings("unchecked")
	public Collection<Ban.Ip> getIpBans() {
		this.removeExpired();
		return (Collection<Ban.Ip>) this.bans_ip.keySet().stream().filter(ban -> ban.getType().equals(BanTypes.IP));
	}

	@Override
	public Optional<Ban.Profile> getBanFor(GameProfile profile) {
		this.removeExpired();
		return this.bans_profile.stream().filter(ban -> ban.getType().equals(BanTypes.PROFILE) && ((Ban.Profile)ban).getProfile().equals(profile)).findFirst();
	}


	@Override
	public Optional<Ban.Ip> getBanFor(InetAddress address) {
		this.removeExpired();
		return this.bans_ip.keySet().stream().filter(ban -> ban.getType().equals(BanTypes.IP) && ((Ip)ban).getAddress().equals(address)).findFirst();
	}


	@Override
	public boolean isBanned(GameProfile profile) {
		return this.getBanFor(profile).isPresent();
	}


	@Override
	public boolean isBanned(InetAddress address) {
		return this.getBanFor(address).isPresent();
	}


	@Override
	public boolean pardon(GameProfile profile) {
		Optional<EUserSubject> subject = this.getSubject(profile.getUniqueId());
		if(subject.isPresent()) {
			return subject.get().pardonBan(Text.EMPTY, EBanService.UNKNOWN);
		} else {
        	throw new IllegalArgumentException(String.format("UserSubject not found : %s", profile.getUniqueId()));
        }
	}


	@Override
	public boolean pardon(InetAddress address) {
		Optional<EIpSubject> subject = this.getSubject(address);
		if(subject.isPresent()) {
			return subject.get().pardon(System.currentTimeMillis(), Text.EMPTY, EBanService.UNKNOWN);
		} else {
        	throw new IllegalArgumentException(String.format("IPSubject not found : %s", address.toString()));
        }
	}


	@Override
	public boolean removeBan(Ban ban) {
		if (ban.getType().equals(BanTypes.PROFILE)) {
			if(this.bans_profile.contains(ban)) {
				return this.pardon(((Ban.Profile) ban).getProfile());
			}
        } else if (ban.getType().equals(BanTypes.IP)) {
        	if(this.bans_ip.containsKey(ban)) {
        		return this.pardon(((Ban.Ip) ban).getAddress());
        	}
        } else {
        	throw new IllegalArgumentException(String.format("Ban %s had unrecognized BanType %s!", ban, ban.getType()));
        }
		return false;
	}


	@Override
	public Optional<? extends Ban> addBan(Ban ban) {
		Optional<? extends Ban> before;
		long time = ban.getCreationDate().toEpochMilli();
    	Text reason = ban.getReason().orElse(Text.EMPTY);
    	String source = ban.getBanSource().orElse(Text.of(EBanService.UNKNOWN)).toPlain();
    	Optional<Long> duration = Optional.empty();
    	if(ban.getExpirationDate().isPresent()) {
    		duration = Optional.of(ban.getCreationDate().toEpochMilli() - ban.getExpirationDate().get().toEpochMilli());
    	}

        if (ban.getType().equals(BanTypes.PROFILE)) {
        	Ban.Profile profile = (Ban.Profile) ban;
        	before = this.getBanFor(profile.getProfile());

            Optional<EUserSubject> subject = this.getSubject(profile.getProfile().getUniqueId());
            if(subject.isPresent()) {
            	subject.get().ban(time, duration, reason, source);
            } else {
            	throw new IllegalArgumentException(String.format("User not found : %s", profile.getProfile().getUniqueId()));
            }
        } else if (ban.getType().equals(BanTypes.IP)) {
        	Ban.Ip ip = (Ban.Ip) ban;
        	before = this.getBanFor(ip.getAddress());
        	
        	Optional<EIpSubject> subject = this.getSubject(ip.getAddress());
    		if(subject.isPresent()) {
    			subject.get().add(time, duration, reason, source);
    		} else {
            	throw new IllegalArgumentException(String.format("IPSubject not found : %s", UtilsNetwork.getHostString(ip.getAddress())));
            }
        } else {
            throw new IllegalArgumentException(String.format("Ban %s had unrecognized BanType %s!", ban, ban.getType()));
        }
        return before;
	}


	@Override
	public boolean hasBan(Ban ban) {
		return this.bans_profile.contains(ban) || this.bans_ip.containsKey(ban);
	}
	
	public void removeExpired() {
		long time = System.currentTimeMillis();
		this.bans_profile.removeIf(ban -> ban.getExpirationDate().isPresent() && ban.getExpirationDate().get().toEpochMilli() < time);
		UtilsMap.removeIf(this.bans_ip, (ban,  uuid) -> ban.getExpirationDate().isPresent() && ban.getExpirationDate().get().toEpochMilli() < time);
	}

	public void add(Ban.Profile ban) {
		this.bans_profile.add(ban);
	}
}
