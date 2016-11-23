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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.World;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.server.location.LocationSQL;
import fr.evercraft.everapi.services.jail.Jail;
import fr.evercraft.everapi.services.jail.JailService;
import fr.evercraft.eversanctions.EverSanctions;

public class EJailService implements JailService {
	private final EverSanctions plugin;
	
	private final ConcurrentMap<String, EJail> jails;
	
	public EJailService(final EverSanctions plugin){
		this.plugin = plugin;
		
		this.jails = new ConcurrentHashMap<String, EJail>();
		
		this.reload();
	}
	
	public void reload() {
		this.jails.clear();
		
		this.jails.putAll(this.selectAsync());
	}

	@Override
	public Collection<Jail> getAll() {
		ImmutableList.Builder<Jail> builder = ImmutableList.builder();
		this.jails.values().stream().filter(jail -> jail.getTransform() != null)
									.forEach(jail -> builder.add(jail));
		return builder.build();
	}
	
	@Override
	public Collection<String> getAllNames() {
		ImmutableList.Builder<String> builder = ImmutableList.builder();
		this.jails.values().stream().forEach(jail -> builder.add(jail.getName()));
		return builder.build();
	}
	
	public Collection<EJail> getAllEJail() {
		return ImmutableList.copyOf(this.jails.values());
	}

	@Override
	public boolean has(String identifier) {
		Preconditions.checkNotNull(identifier, "identifier");
		
		return this.jails.containsKey(identifier);
	}

	@Override
	public Optional<Jail> get(String identifier) {
		Optional<EJail> jail = this.getEJail(identifier);
		if(jail.isPresent() && jail.get().getTransform() != null) {
			return Optional.of(jail.get());
		}
		return Optional.empty();
	}
	
	public Optional<EJail> getEJail(String identifier) {
		Preconditions.checkNotNull(identifier, "identifier");
		
		return Optional.ofNullable(this.jails.get(identifier));
	}

	@Override
	public Optional<Jail> add(String identifier, Transform<World> location, Optional<Integer> radius) {
		Preconditions.checkNotNull(identifier, "identifier");
		Preconditions.checkNotNull(location, "location");
		Preconditions.checkNotNull(radius, "radius");
		
		if (!this.jails.containsKey(identifier)) {
			final EJail jail = new EJail(this.plugin, identifier, radius, new LocationSQL(this.plugin, location));
			this.jails.put(identifier, jail);
			this.plugin.getThreadAsync().execute(() -> this.addAsync(jail));
			return Optional.of(jail);
		}
		return Optional.empty();
	}

	@Override
	public boolean remove(String identifier) {
		Preconditions.checkNotNull(identifier, "identifier");
		
		if (this.jails.containsKey(identifier)) {
			this.jails.remove(identifier);
			this.plugin.getThreadAsync().execute(() -> this.removeAsync(identifier));
			return true;
		}
		return false;
	}

	@Override
	public boolean clearAll() {
		if (!this.jails.isEmpty()) {
			this.jails.clear();
			this.plugin.getThreadAsync().execute(() -> this.clearAsync());
			return true;
		}
		return false;
	}
	
	/*
	 * Database
	 */
	
	public Map<String, EJail> selectAsync() {
		Map<String, EJail> jails = new HashMap<String, EJail>();
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "SELECT *" 
							+ "FROM `" + this.plugin.getDataBase().getTableJails() + "` ;";
			preparedStatement = connection.prepareStatement(query);
			ResultSet list = preparedStatement.executeQuery();
			while (list.next()) {
				Optional<Integer> radius = Optional.of(list.getInt("radius"));
				if (list.wasNull()) {
					radius = Optional.empty();
				}
				
				LocationSQL location = new LocationSQL(this.plugin,	list.getString("world"), 
														list.getDouble("x"),
														list.getDouble("y"),
														list.getDouble("z"),
														list.getDouble("yaw"),
														list.getDouble("pitch"));
				jails.put(list.getString("identifier"), new EJail(this.plugin, list.getString("identifier"), radius, location));
				this.plugin.getLogger().debug("Loading : (jail='" + list.getString("identifier") + "';radius='" + radius + "';location='" + location + "')");
			}
    	} catch (SQLException e) {
    		this.plugin.getLogger().warn("Jails error when loading : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
    	return jails;
	}
	
	public void addAsync(final EJail jail) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "INSERT INTO `" + this.plugin.getDataBase().getTableJails() + "` "
    						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, jail.getName());
			preparedStatement.setInt(2, jail.getRadius());
			preparedStatement.setString(3, jail.getLocationSQL().getWorldUUID());
			preparedStatement.setDouble(4, jail.getLocationSQL().getX());
			preparedStatement.setDouble(5, jail.getLocationSQL().getY());
			preparedStatement.setDouble(6, jail.getLocationSQL().getZ());
			preparedStatement.setDouble(7, jail.getLocationSQL().getYaw());
			preparedStatement.setDouble(8, jail.getLocationSQL().getPitch());
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Adding to the database : (jail='" + jail.getName() + "';radius='" + jail.getRadius() + "';location='" + jail.getName() + "')");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of jail : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
	
	public void removeAsync(final String identifier) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.plugin.getDataBase().getTableJails() + "` "
		    				+ "WHERE `identifier` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, identifier);
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (jail='" + identifier + "')");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of jail : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
	
	public void clearAsync() {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "TRUNCATE `" + this.plugin.getDataBase().getTableJails() + "` ;";
			preparedStatement = connection.prepareStatement(query);
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Removes the database jails");
    	} catch (SQLException e) {
    		this.plugin.getLogger().warn("Error jails deletions : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
}