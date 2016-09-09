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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import fr.evercraft.everapi.java.Chronometer;
import fr.evercraft.everapi.services.sanction.SanctionIpSubject;
import fr.evercraft.everapi.services.sanction.SanctionService;
import fr.evercraft.everapi.services.sanction.SanctionUserSubject;
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;
import fr.evercraft.everapi.sponge.UtilsNetwork;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.auto.EAutoReason;
import fr.evercraft.eversanctions.service.subject.EIpSubject;
import fr.evercraft.eversanctions.service.subject.EUserSubject;

public abstract class ESanctionService implements SanctionService {
	
	protected final EverSanctions plugin;
	
	private final ConcurrentMap<UUID, EUserSubject> users;
	private final LoadingCache<UUID, EUserSubject> users_cache;

	private final LoadingCache<String, EIpSubject> ips_cache;
	
	private final ConcurrentMap<String, EAutoReason> reasons;
	
	public ESanctionService(final EverSanctions plugin) {
		this.plugin = plugin;
		
		this.reasons = new ConcurrentHashMap<String, EAutoReason>();
		this.users = new ConcurrentHashMap<UUID, EUserSubject>();
		this.users_cache = CacheBuilder.newBuilder()
			    .maximumSize(100)
			    .expireAfterAccess(5, TimeUnit.MINUTES)
			    .build(new CacheLoader<UUID, EUserSubject>() {
			    	/**
			    	 * Ajoute un joueur au cache
			    	 */
			        @Override
			        public EUserSubject load(UUID uuid){
			        	Chronometer chronometer = new Chronometer();
			        	
			        	EUserSubject subject = new EUserSubject(ESanctionService.this.plugin, uuid);
			        	
			        	ESanctionService.this.plugin.getLogger().debug("Loading user '" + uuid.toString() + "' in " +  chronometer.getMilliseconds().toString() + " ms");
			            return subject;
			        }
			    });
		this.ips_cache = CacheBuilder.newBuilder()
					    .maximumSize(100)
					    .expireAfterAccess(10, TimeUnit.MINUTES)
					    .build(new CacheLoader<String, EIpSubject>() {
					    	/**
					    	 * Ajoute un joueur au cache
					    	 */
					        @Override
					        public EIpSubject load(String address_string){
					        	Chronometer chronometer = new Chronometer();
					        	
					        	Optional<InetAddress> address = UtilsNetwork.getHost(address_string);
					        	if(address.isPresent()) {
						        	EIpSubject subject = new EIpSubject(ESanctionService.this.plugin, address.get());
						        	ESanctionService.this.plugin.getLogger().debug("Loading ip '" + address_string + "' in " +  chronometer.getMilliseconds().toString() + " ms");
						            return subject;
					        	}
					        	return null;
					        }
					    });
	}
	
	/**
	 * Rechargement : Vide le cache et recharge tous les joueurs
	 */
	public void reload() {
		this.users_cache.cleanUp();
		this.ips_cache.cleanUp();
		for (EUserSubject subject : this.users.values()) {
			subject.reload();
		}
	}
	
	/*
	 * User
	 */

	@Override
	public Optional<SanctionUserSubject> get(UUID uuid) {
		return Optional.ofNullable(this.getSubject(uuid).orElse(null));
	}
	
	public Optional<EUserSubject> getSubject(UUID uuid) {
		Preconditions.checkNotNull(uuid, "uuid");
		try {
			if (!this.users.containsKey(uuid)) {
				return Optional.of(this.users_cache.get(uuid));
	    	}
	    	return Optional.ofNullable(this.users.get(uuid));
		} catch (ExecutionException e) {
			this.plugin.getLogger().warn("Error : Loading user (identifier='" + uuid + "';message='" + e.getMessage() + "')");
			return Optional.empty();
		}
	}
	
	@Override
	public boolean hasRegistered(UUID uuid) {
		Preconditions.checkNotNull(uuid, "uuid");
		
		try {
			return this.plugin.getGame().getServer().getPlayer(uuid).isPresent();
		} catch (IllegalArgumentException e) {}
		return false;
	}
	
	/**
	 * Ajoute un joueur à la liste
	 * @param identifier L'UUID du joueur
	 */
	public void registerPlayer(UUID uuid) {
		Preconditions.checkNotNull(uuid, "uuid");
		
		EUserSubject player = this.users_cache.getIfPresent(uuid);
		// Si le joueur est dans le cache
		if (player != null) {
			this.users.putIfAbsent(uuid, player);
			this.plugin.getLogger().debug("Loading player cache : " + uuid.toString());
		// Si le joueur n'est pas dans le cache
		} else {
			Chronometer chronometer = new Chronometer();
			player = new EUserSubject(this.plugin, uuid);
			this.users.putIfAbsent(uuid, player);
			this.plugin.getLogger().debug("Loading player '" + uuid.toString() + "' in " +  chronometer.getMilliseconds().toString() + " ms");
		}
		//this.plugin.getManagerEvent().post(player, PermUserEvent.Action.USER_ADDED);
	}
	
	/**
	 * Supprime un joueur à la liste et l'ajoute au cache
	 * @param identifier L'UUID du joueur
	 */
	public void removePlayer(UUID uuid) {
		Preconditions.checkNotNull(uuid, "uuid");
		
		EUserSubject player = this.users.remove(uuid);
		// Si le joueur existe
		if (player != null) {
			this.users_cache.put(uuid, player);
			//this.plugin.getManagerEvent().post(player, PermUserEvent.Action.USER_REMOVED);
			this.plugin.getLogger().debug("Unloading the player : " + uuid.toString());
		}
	}
	
	/*
	 * Ip
	 */

	@Override
	public Optional<SanctionIpSubject> get(InetAddress address) {
		return Optional.ofNullable(this.getSubject(address).orElse(null));
	}
	
	public Optional<EIpSubject> getSubject(InetAddress address) {
		Preconditions.checkNotNull(address, "address");
		try {
			return Optional.of(this.ips_cache.get(UtilsNetwork.getHostString(address)));
		} catch (ExecutionException e) {
			this.plugin.getLogger().warn("Error : Loading user (identifier='" + address.toString() + "';message='" + e.getMessage() + "')");
			return Optional.empty();
		}
	}
	
	@Override
	public boolean hasRegistered(InetAddress address) {
		Preconditions.checkNotNull(address, "address");
		
		return this.ips_cache.getIfPresent(UtilsNetwork.getHostString(address)) != null;
	}
	
	/*
	 * Reason
	 */
	
	@Override
	public Optional<SanctionAuto.Reason> getReason(String identifier) {
		return Optional.ofNullable(this.reasons.get(identifier.toLowerCase()));
	}
}
