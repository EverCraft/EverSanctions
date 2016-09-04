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
import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.server.location.LocationSQL;
import fr.evercraft.everapi.services.sanction.Jail;
import fr.evercraft.everapi.services.sanction.JailService;
import fr.evercraft.eversanctions.EverSanctions;

public class EJailService implements JailService {
	private final EverSanctions plugin;
	
	private final ConcurrentMap<String, EJail> jails;
	
	public EJailService(final EverSanctions plugin){
		this.plugin = plugin;
		
		this.jails = new ConcurrentHashMap<String, EJail>();
		
		reload();
	}
	
	public void reload() {
		this.jails.clear();
		
		this.jails.putAll(this.selectAsync());
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Collection<Jail> getAll() {
		return (Collection) this.jails.values();
	}

	@Override
	public boolean has(String identifier) {
		Preconditions.checkNotNull(identifier, "identifier");
		
		return this.jails.containsKey(identifier);
	}

	@Override
	public Optional<Jail> get(String identifier) {
		Preconditions.checkNotNull(identifier, "identifier");
		
		return Optional.ofNullable(this.jails.get(identifier));
	}

	@Override
	public boolean add(String identifier, int radius, Transform<World> location) {
		Preconditions.checkNotNull(identifier, "identifier");
		Preconditions.checkNotNull(radius, "radius");
		Preconditions.checkNotNull(location, "location");
		
		if (!this.jails.containsKey(identifier)) {
			final EJail jail = new EJail(identifier, radius, new LocationSQL(this.plugin, location));
			this.jails.put(identifier, jail);
			this.plugin.getThreadAsync().execute(() -> this.addAsync(jail));
			return true;
		}
		return false;
	}

	@Override
	public boolean update(String identifier, int radius, Transform<World> location) {
		Preconditions.checkNotNull(identifier, "identifier");
		Preconditions.checkNotNull(radius, "radius");
		Preconditions.checkNotNull(location, "location");
		
		if (this.jails.containsKey(identifier)) {
			final EJail jail = new EJail(identifier, radius, new LocationSQL(this.plugin, location));
			this.jails.put(identifier, jail);
			this.plugin.getThreadAsync().execute(() -> this.updateAsync(jail));
			return true;
		}
		return false;
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
				LocationSQL location = new LocationSQL(this.plugin,	list.getString("world"), 
														list.getDouble("x"),
														list.getDouble("y"),
														list.getDouble("z"),
														list.getDouble("yaw"),
														list.getDouble("pitch"));
				jails.put(list.getString("identifier"), new EJail(list.getString("identifier"), list.getInt("radius"), location));
				this.plugin.getLogger().debug("Loading : (jail='" + list.getString("identifier") + "';radius='" + list.getInt("radius") + "';location='" + location + "')");
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
	
	public void updateAsync(final EJail jail) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "UPDATE `" + this.plugin.getDataBase().getTableJails() + "` "
    						+ "SET `radius` = ?,"
    							+ "`world` = ?, "
	    						+ "`x` = ?, "
	    						+ "`y` = ?, "
	    						+ "`z` = ?, "
	    						+ "`yaw` = ?, "
	    						+ "`pitch` = ? "
    						+ "WHERE `identifier` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setInt(1, jail.getRadius());
			preparedStatement.setString(2, jail.getLocationSQL().getWorldUUID());
			preparedStatement.setDouble(3, jail.getLocationSQL().getX());
			preparedStatement.setDouble(4, jail.getLocationSQL().getY());
			preparedStatement.setDouble(5, jail.getLocationSQL().getZ());
			preparedStatement.setDouble(6, jail.getLocationSQL().getYaw());
			preparedStatement.setDouble(7, jail.getLocationSQL().getPitch());
			preparedStatement.setString(8, jail.getName());
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Updating the database : (jail='" + jail.getName() + "';radius='" + jail.getRadius() + "';location='" + jail.getName() + "')");
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