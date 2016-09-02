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
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import fr.evercraft.everapi.java.Chronometer;
import fr.evercraft.everapi.server.location.LocationSQL;
import fr.evercraft.everapi.services.sanction.Jail;
import fr.evercraft.everapi.services.sanction.SanctionService;
import fr.evercraft.everapi.services.sanction.SubjectUserSanction;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.manual.EManualIPBan;
import fr.evercraft.eversanctions.service.subject.EUserSubject;

public abstract class ESanctionService implements SanctionService {
	
	protected final EverSanctions plugin;
	
	private final ConcurrentMap<UUID, EUserSubject> subjects;
	private final LoadingCache<UUID, EUserSubject> cache;

	private final ConcurrentSkipListSet<EManualIPBan> ips;
	
	private final ConcurrentMap<String, EJail> jails;
	
	public ESanctionService(final EverSanctions plugin) {
		this.plugin = plugin;
		
		this.ips = new ConcurrentSkipListSet<EManualIPBan>((EManualIPBan o1, EManualIPBan o2) -> o2.getCreationDate().compareTo(o1.getCreationDate()));
		this.subjects = new ConcurrentHashMap<UUID, EUserSubject>();
		this.jails = new ConcurrentHashMap<String, EJail>();
		this.cache = CacheBuilder.newBuilder()
					    .maximumSize(100)
					    .expireAfterAccess(5, TimeUnit.MINUTES)
					    .removalListener(new RemovalListener<UUID, EUserSubject>() {
					    	/**
					    	 * Supprime un joueur du cache
					    	 */
							@Override
							public void onRemoval(RemovalNotification<UUID, EUserSubject> notification) {
								//EssentialsSubject.this.plugin.getManagerEvent().post(notification.getValue(), PermUserEvent.Action.USER_REMOVED);
							}
					    	
					    })
					    .build(new CacheLoader<UUID, EUserSubject>() {
					    	/**
					    	 * Ajoute un joueur au cache
					    	 */
					        @Override
					        public EUserSubject load(UUID uuid){
					        	Chronometer chronometer = new Chronometer();
					        	
					        	EUserSubject subject = new EUserSubject(ESanctionService.this.plugin, uuid);
					        	ESanctionService.this.plugin.getLogger().debug("Loading user '" + uuid.toString() + "' in " +  chronometer.getMilliseconds().toString() + " ms");
					            
					            //EssentialsSubject.this.plugin.getManagerEvent().post(subject, PermUserEvent.Action.USER_ADDED);
					            return subject;
					        }
					    });
	}
	

	@Override
	public Optional<SubjectUserSanction> get(UUID uuid) {
		return Optional.ofNullable(this.getSubject(uuid).orElse(null));
	}
	
	public Optional<EUserSubject> getSubject(UUID uuid) {
		Preconditions.checkNotNull(uuid, "uuid");
		try {
			if (!this.subjects.containsKey(uuid)) {
				return Optional.of(this.cache.get(uuid));
	    	}
	    	return Optional.ofNullable(this.subjects.get(uuid));
		} catch (ExecutionException e) {
			this.plugin.getLogger().warn("Error : Loading user (identifier='" + uuid + "';message='" + e.getMessage() + "')");
			return Optional.empty();
		}
	}
	
	public Optional<EUserSubject> getOnline(UUID uuid) {
		Preconditions.checkNotNull(uuid, "uuid");
		return Optional.ofNullable(this.subjects.get(uuid));
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
	 * Rechargement : Vide le cache et recharge tous les joueurs
	 */
	public void reload() {
		this.cache.cleanUp();
		for (EUserSubject subject : this.subjects.values()) {
			subject.reload();
		}
	}
	
	/**
	 * Ajoute un joueur à la liste
	 * @param identifier L'UUID du joueur
	 */
	public void registerPlayer(UUID uuid) {
		Preconditions.checkNotNull(uuid, "uuid");
		
		EUserSubject player = this.cache.getIfPresent(uuid);
		// Si le joueur est dans le cache
		if (player != null) {
			this.subjects.putIfAbsent(uuid, player);
			this.plugin.getLogger().debug("Loading player cache : " + uuid.toString());
		// Si le joueur n'est pas dans le cache
		} else {
			Chronometer chronometer = new Chronometer();
			player = new EUserSubject(this.plugin, uuid);
			this.subjects.putIfAbsent(uuid, player);
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
		
		EUserSubject player = this.subjects.remove(uuid);
		// Si le joueur existe
		if (player != null) {
			this.cache.put(uuid, player);
			//this.plugin.getManagerEvent().post(player, PermUserEvent.Action.USER_REMOVED);
			this.plugin.getLogger().debug("Unloading the player : " + uuid.toString());
		}
	}
	
	public boolean addBan(InetAddress address, long creation, long duration, Text reason, final String source) {
		return true;
	}
	
	public boolean pardon(InetAddress address, Text reason, String source) {
		return false;
	}
	
	/*
	 * Jails
	 */
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Collection<Jail> getAllJails() {
		return (Collection) this.jails.values();
	}

	@Override
	public boolean hasJail(String identifier) {
		Preconditions.checkNotNull(identifier, "identifier");
		
		return this.jails.containsKey(identifier);
	}

	@Override
	public Optional<Jail> getJail(String identifier) {
		Preconditions.checkNotNull(identifier, "identifier");
		
		return Optional.ofNullable(this.jails.get(identifier));
	}

	@Override
	public boolean addJail(String identifier, int radius, Transform<World> location) {
		Preconditions.checkNotNull(identifier, "identifier");
		Preconditions.checkNotNull(radius, "radius");
		Preconditions.checkNotNull(location, "location");
		
		if (!this.jails.containsKey(identifier)) {
			final EJail jail = new EJail(identifier, radius, new LocationSQL(this.plugin, location));
			this.jails.put(identifier, jail);
			this.plugin.getThreadAsync().execute(() -> this.plugin.getDataBase().addJail(jail));
			return true;
		}
		return false;
	}

	@Override
	public boolean updateJail(String identifier, int radius, Transform<World> location) {
		Preconditions.checkNotNull(identifier, "identifier");
		Preconditions.checkNotNull(radius, "radius");
		Preconditions.checkNotNull(location, "location");
		
		if (this.jails.containsKey(identifier)) {
			final EJail jail = new EJail(identifier, radius, new LocationSQL(this.plugin, location));
			this.jails.put(identifier, jail);
			this.plugin.getThreadAsync().execute(() -> this.plugin.getDataBase().updateJail(jail));
			return true;
		}
		return false;
	}

	@Override
	public boolean removeJail(String identifier) {
		Preconditions.checkNotNull(identifier, "identifier");
		
		if (this.jails.containsKey(identifier)) {
			this.jails.remove(identifier);
			this.plugin.getThreadAsync().execute(() -> this.plugin.getDataBase().removeJail(identifier));
			return true;
		}
		return false;
	}

	@Override
	public boolean clearAllJails() {
		if (!this.jails.isEmpty()) {
			this.jails.clear();
			this.plugin.getThreadAsync().execute(() -> this.plugin.getDataBase().clearJail());
			return true;
		}
		return false;
	}
}
