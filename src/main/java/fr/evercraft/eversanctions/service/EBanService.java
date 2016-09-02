package fr.evercraft.eversanctions.service;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.Ban.Ip;
import org.spongepowered.api.util.ban.Ban.Profile;
import org.spongepowered.api.util.ban.BanTypes;

import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.subject.EUserSubject;

public class EBanService extends ESanctionService {
	
	private static final String UNKNOWN = "unknown";
	
	// Ordre décroissant
	private final ConcurrentSkipListSet<? extends Ban> bans;
	
	public EBanService(final EverSanctions plugin) {
		super(plugin);
		
		this.bans = new ConcurrentSkipListSet<Ban>((Ban o1, Ban o2) -> o2.getCreationDate().compareTo(o1.getCreationDate()));
	}
	
	@Override
	public Collection<? extends Ban> getBans() {
		return null;
	}


	@Override
	@SuppressWarnings("unchecked")
	public Collection<Profile> getProfileBans() {
		this.removeExpired();
		return (Collection<Profile>) this.bans.stream().filter(ban -> ban.getType().equals(BanTypes.PROFILE));
	}


	@Override
	@SuppressWarnings("unchecked")
	public Collection<Ban.Ip> getIpBans() {
		this.removeExpired();
		return (Collection<Ban.Ip>) this.bans.stream().filter(ban -> ban.getType().equals(BanTypes.IP));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Optional<Ban.Profile> getBanFor(GameProfile profile) {
		this.removeExpired();
		return (Optional<Ban.Profile>) this.bans.stream().filter(ban -> ban.getType().equals(BanTypes.PROFILE) && ((Ban.Profile)ban).getProfile().equals(profile)).findFirst();
	}


	@Override
	@SuppressWarnings("unchecked")
	public Optional<Ban.Ip> getBanFor(InetAddress address) {
		this.removeExpired();
		return (Optional<Ban.Ip>) this.bans.stream().filter(ban -> ban.getType().equals(BanTypes.IP) && ((Ip)ban).getAddress().equals(address)).findFirst();
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
			return subject.get().pardon(Text.EMPTY, EBanService.UNKNOWN);
		} else {
        	throw new IllegalArgumentException(String.format("User not found : %s", profile.getUniqueId()));
        }
	}


	@Override
	public boolean pardon(InetAddress address) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean removeBan(Ban ban) {
		if (this.bans.contains(ban)) {
			if (ban.getType().equals(BanTypes.PROFILE)) {
	            return this.pardon(((Ban.Profile) ban).getProfile());
	        } else if (ban.getType().equals(BanTypes.IP)) {
	            return this.pardon(((Ban.Ip) ban).getAddress());
	        }
	        throw new IllegalArgumentException(String.format("Ban %s had unrecognized BanType %s!", ban, ban.getType()));
		} 
		return false;
	}


	@Override
	public Optional<? extends Ban> addBan(Ban ban) {
		Optional<? extends Ban> before;
		
		long creation = ban.getCreationDate().toEpochMilli();
    	Text reason = ban.getReason().orElse(Text.EMPTY);
    	String source = ban.getBanSource().orElse(Text.of(EBanService.UNKNOWN)).toPlain();
		long duration = -1;
    	if(ban.getExpirationDate().isPresent()) {
    		duration = ban.getCreationDate().toEpochMilli() - ban.getExpirationDate().get().toEpochMilli();
    	}

        if (ban.getType().equals(BanTypes.PROFILE)) {
        	Ban.Profile profile = (Ban.Profile) ban;
        	before = this.getBanFor(profile.getProfile());

            Optional<EUserSubject> subject = this.getSubject(profile.getProfile().getUniqueId());
            if(subject.isPresent()) {
            	subject.get().addBan(creation, duration, reason, source);
            } else {
            	throw new IllegalArgumentException(String.format("User not found : %s", profile.getProfile().getUniqueId()));
            }
        } else if (ban.getType().equals(BanTypes.IP)) {
        	Ban.Ip ip = (Ban.Ip) ban;
        	before = this.getBanFor(ip.getAddress());
            
            this.addBan(ip.getAddress(), creation, duration, reason, source);
        } else {
            throw new IllegalArgumentException(String.format("Ban %s had unrecognized BanType %s!", ban, ban.getType()));
        }
        return before;
	}


	@Override
	public boolean hasBan(Ban ban) {
		return this.bans.contains(ban);
	}
	
	public void removeExpired() {
		long time = System.currentTimeMillis();
		this.bans.removeIf(ban -> ban.getExpirationDate().isPresent() && ban.getExpirationDate().get().toEpochMilli() < time);
	}
}
