package fr.evercraft.eversanctions.service;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.Ban.Ip;
import org.spongepowered.api.util.ban.Ban.Profile;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import fr.evercraft.everapi.java.Chronometer;
import fr.evercraft.everapi.services.essentials.SubjectUserEssentials;
import fr.evercraft.everapi.services.sanction.SanctionService;
import fr.evercraft.everapi.services.sanction.SubjectUserSanction;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.manual.EManual;
import fr.evercraft.eversanctions.service.subject.EUserSubject;

public class EBanService extends ESanctionService {
	
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
		return (Collection<Profile>) this.bans.stream().filter(ban -> ban instanceof Profile);
	}


	@Override
	@SuppressWarnings("unchecked")
	public Collection<Ip> getIpBans() {
		this.removeExpired();
		return (Collection<Ip>) this.bans.stream().filter(ban -> ban instanceof Ip);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Optional<Profile> getBanFor(GameProfile profile) {
		this.removeExpired();
		return (Optional<Profile>) this.bans.stream().filter(ban -> ban instanceof Profile && ((Profile)ban).getProfile().equals(profile)).findFirst();
	}


	@Override
	@SuppressWarnings("unchecked")
	public Optional<Ip> getBanFor(InetAddress address) {
		this.removeExpired();
		return (Optional<Ip>) this.bans.stream().filter(ban -> ban instanceof Ip && ((Ip)ban).getAddress().equals(address)).findFirst();
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
			//return subject.pardon();
		}
		return false;
	}


	@Override
	public boolean pardon(InetAddress address) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean removeBan(Ban ban) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public Optional<? extends Ban> addBan(Ban ban) {
		// TODO Auto-generated method stub
		return null;
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
