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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiPredicate;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.BanTypes;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.java.UtilsMap;
import fr.evercraft.everapi.sponge.UtilsNetwork;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.subject.EIpSubject;
import fr.evercraft.eversanctions.service.subject.EUserSubject;

public class EBanService extends ESanctionService {
	
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
	private final ConcurrentSkipListMap<Ban.Profile, UUID> bans_profile;
	private final ConcurrentSkipListMap<Ban.Ip, String> bans_ip;
	
	public EBanService(final EverSanctions plugin) {
		super(plugin);
		
		this.bans_profile = new ConcurrentSkipListMap<Ban.Profile, UUID>(EBanService.COMPARATOR_BAN);
		this.bans_ip = new ConcurrentSkipListMap<Ban.Ip, String>(EBanService.COMPARATOR_BAN);
	}
	
	public void reload() {
		this.bans_profile.clear();
		this.bans_ip.clear();
		
		Connection connection = null;
		try {
			connection = this.plugin.getDataBase().getConnection();
			
			this.bans_profile.putAll(this.plugin.getDataBase().getBansProfile(connection));
			this.bans_ip.putAll(this.plugin.getDataBase().getBansIp(connection));
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try { if (connection != null) connection.close(); } catch (SQLException e) {}
	    }
		
		super.reload();
	}
	
	@Override
	public Collection<Ban> getBans() {
		this.removeExpired();
		
		Builder<Ban> builder = ImmutableSet.builder();
		builder.addAll(this.bans_profile.keySet());
		builder.addAll(this.bans_ip.keySet());
		return builder.build();
	}


	@Override
	public Collection<Ban.Profile> getProfileBans() {
		this.removeExpired();
		
		Builder<Ban.Profile> builder = ImmutableSet.builder();
		builder.addAll(this.bans_profile.keySet());
		return builder.build();
	}


	@Override
	public Collection<Ban.Ip> getIpBans() {
		this.removeExpired();
		
		Builder<Ban.Ip> builder = ImmutableSet.builder();
		builder.addAll(this.bans_ip.keySet());
		return builder.build();
	}

	@Override
	public Optional<Ban.Profile> getBanFor(GameProfile profile) {
		this.removeExpired();
		
		Optional<Ban.Profile> ban = Optional.empty();
		Iterator<Entry<Ban.Profile, UUID>> iterator = this.bans_profile.entrySet().iterator();
		
		while(!ban.isPresent() && iterator.hasNext()) {
			Entry<Ban.Profile, UUID> element = iterator.next();
			if(element.getValue().equals(profile.getUniqueId())) {
				ban = Optional.of(element.getKey());
			}
		}
		return ban;
	}


	@Override
	public Optional<Ban.Ip> getBanFor(InetAddress address) {
		this.removeExpired();
		
		String address_string = UtilsNetwork.getHostString(address);
		Optional<Ban.Ip> ban = Optional.empty();
		Iterator<Entry<Ban.Ip, String>> iterator = this.bans_ip.entrySet().iterator();
		
		this.plugin.getLogger().warn("size : " + this.bans_ip.size());
		while(!ban.isPresent() && iterator.hasNext()) {
			Entry<Ban.Ip, String> element = iterator.next();
			this.plugin.getLogger().warn(address_string + " : " + element.getValue());
			if(element.getValue().equalsIgnoreCase(address_string)) {
				ban = Optional.of(element.getKey());
				this.plugin.getLogger().warn("true");
			}
		}
		return ban;
	}


	@Override
	public boolean isBanned(GameProfile profile) {
		this.removeExpired();
		return this.bans_profile.containsValue(profile.getUniqueId());
	}


	@Override
	public boolean isBanned(InetAddress address) {
		this.removeExpired();
		return this.bans_ip.containsValue(UtilsNetwork.getHostString(address));
	}


	@Override
	public boolean pardon(GameProfile profile) {
		Optional<EUserSubject> subject = this.getSubject(profile.getUniqueId());
		if(subject.isPresent()) {
			return subject.get().pardonBan(System.currentTimeMillis(), Text.EMPTY, this.plugin.getEServer().getConsole()).isPresent();
		} else {
        	throw new IllegalArgumentException(String.format("UserSubject not found : %s", profile.getUniqueId()));
        }
	}


	@Override
	public boolean pardon(InetAddress address) {
		Optional<EIpSubject> subject = this.getSubject(address);
		if(subject.isPresent()) {
			return !subject.get().pardonBan(System.currentTimeMillis(), Text.EMPTY, this.plugin.getEServer().getConsole()).isEmpty();
		} else {
        	throw new IllegalArgumentException(String.format("IPSubject not found : %s", address.toString()));
        }
	}


	@Override
	public boolean removeBan(Ban ban) {
		if (ban.getType().equals(BanTypes.PROFILE)) {
			if(this.bans_profile.containsKey(ban)) {
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
    	
    	Optional<CommandSource> source = ban.getBanCommandSource();
    	if (ban.getBanSource().isPresent()) {
    		source = Optional.ofNullable(this.plugin.getEServer().getEPlayer(ban.getBanSource().get().toPlain()).orElse(null));
    	}
    	
    	Optional<Long> duration = Optional.empty();
    	if(ban.getExpirationDate().isPresent()) {
    		duration = Optional.of(ban.getCreationDate().toEpochMilli() - ban.getExpirationDate().get().toEpochMilli());
    	}

        if (ban.getType().equals(BanTypes.PROFILE)) {
        	Ban.Profile profile = (Ban.Profile) ban;
        	before = this.getBanFor(profile.getProfile());

            Optional<EUserSubject> subject = this.getSubject(profile.getProfile().getUniqueId());
            if(subject.isPresent()) {
            	subject.get().ban(time, duration, reason, source.orElse(this.plugin.getEServer().getConsole()));
            } else {
            	throw new IllegalArgumentException(String.format("User not found : %s", profile.getProfile().getUniqueId()));
            }
        } else if (ban.getType().equals(BanTypes.IP)) {
        	Ban.Ip ip = (Ban.Ip) ban;
        	before = this.getBanFor(ip.getAddress());
        	
        	Optional<EIpSubject> subject = this.getSubject(ip.getAddress());
    		if(subject.isPresent()) {
    			subject.get().ban(time, duration, reason, source.orElse(this.plugin.getEServer().getConsole()));
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
		return this.bans_profile.containsKey(ban) || this.bans_ip.containsKey(ban);
	}
	
	public void removeExpired() {
		long time = System.currentTimeMillis();
		UtilsMap.removeIf(this.bans_profile, (ban,  uuid) -> ban.getExpirationDate().isPresent() && ban.getExpirationDate().get().toEpochMilli() < time);
		UtilsMap.removeIf(this.bans_ip, (ban,  address) -> ban.getExpirationDate().isPresent() && ban.getExpirationDate().get().toEpochMilli() < time);
	}

	public void add(Ban.Profile ban) {
		this.bans_profile.put(ban, ban.getProfile().getUniqueId());
	}
	
	public void add(Ban.Ip ban) {
		this.bans_ip.put(ban, UtilsNetwork.getHostString(ban.getAddress()));
	}

	public void remove(Ban.Profile profile) {
		BiPredicate<Ban.Profile, UUID> predicate = (ban, uuid) -> {
			if (!ban.getProfile().getUniqueId().equals(profile.getProfile().getUniqueId())) {
				return false;
			}
			if (ban.getCreationDate().toEpochMilli() != profile.getCreationDate().toEpochMilli()) {
				return false;
			}
			if(ban.getExpirationDate().isPresent() || profile.getExpirationDate().isPresent()) { 
				if (!ban.getExpirationDate().isPresent() || !profile.getExpirationDate().isPresent() ||
						ban.getExpirationDate().get().toEpochMilli() != profile.getExpirationDate().get().toEpochMilli()) {
					return false;
				}
			}
			if(!ban.getReason().orElse(Text.EMPTY).toPlain().equals(profile.getReason().orElse(Text.EMPTY).toPlain())) {
				return false;
			}
			if(!ban.getBanSource().orElse(Text.EMPTY).toPlain().equals(profile.getBanSource().orElse(Text.EMPTY).toPlain())) {
				return false;
			}
			return true;
		};
		UtilsMap.removeIf(this.bans_profile, predicate);
	}
	
	public void remove(Ban.Ip profile) {
		BiPredicate<Ban.Ip, String> predicate = (ban, address) -> {
			if(!ban.getAddress().equals(profile.getAddress())) {
				return false;
			}
			if(ban.getCreationDate().toEpochMilli() != profile.getCreationDate().toEpochMilli()) {
				return false;
			}
			if(ban.getExpirationDate().isPresent() || profile.getExpirationDate().isPresent()) { 
				if (!ban.getExpirationDate().isPresent() || !profile.getExpirationDate().isPresent() ||
						ban.getExpirationDate().get().toEpochMilli() != profile.getExpirationDate().get().toEpochMilli()) {
					return false;
				}
			}
			if(!ban.getReason().orElse(Text.EMPTY).toPlain().equals(profile.getReason().orElse(Text.EMPTY).toPlain())) {
				return false;
			}
			if(!ban.getBanSource().orElse(Text.EMPTY).toPlain().equals(profile.getBanSource().orElse(Text.EMPTY).toPlain())) {
				return false;
			}
			return true;
		};
		UtilsMap.removeIf(this.bans_ip, predicate);
	}
}
