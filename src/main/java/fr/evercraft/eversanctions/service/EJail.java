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
import java.sql.SQLException;
import java.util.Optional;

import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.World;

import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.server.location.EVirtualTransform;
import fr.evercraft.everapi.server.location.VirtualTransform;
import fr.evercraft.everapi.services.jail.Jail;
import fr.evercraft.eversanctions.EverSanctions;

public class EJail implements Jail {
	
	private final EverSanctions plugin;
	
	private final String name;
	private Optional<Integer> radius;
	private VirtualTransform location;
	
	public EJail(final EverSanctions plugin, final String name, final Optional<Integer> radius, final VirtualTransform location) {
		this.plugin = plugin;
		this.name = name;
		this.radius = radius;
		this.location = location;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public int getRadius() {
		return this.radius.orElse(this.plugin.getConfigs().getJailRadius());
	}
	
	@Override
	public Transform<World> getTransform() {
		return this.location.getTransform().orElse(null);
	}

	public VirtualTransform getVirtualTransform() {
		return this.location;
	}

	public boolean update(final Transform<World> transform) {
		this.location = new EVirtualTransform(this.plugin, transform);
		this.plugin.getThreadAsync().execute(() -> this.updateAsync());
		return true;
	}
	
	public boolean update(final Optional<Integer> radius) {
		this.radius = radius;
		this.plugin.getThreadAsync().execute(() -> this.updateAsync());
		return true;
	}
	
	public boolean update(final Transform<World> transform, final Optional<Integer> radius) {
		this.location = new EVirtualTransform(this.plugin, transform);
		this.radius = radius;
		this.plugin.getThreadAsync().execute(() -> this.updateAsync());
		return true;
	}
	
	public void updateAsync() {
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
			preparedStatement.setInt(1, this.getRadius());
			preparedStatement.setString(2, this.location.getWorldIdentifier());
			preparedStatement.setDouble(3, this.location.getPosition().getFloorX());
			preparedStatement.setDouble(4, this.location.getPosition().getFloorY());
			preparedStatement.setDouble(5, this.location.getPosition().getFloorZ());
			preparedStatement.setDouble(6, this.location.getYaw());
			preparedStatement.setDouble(7, this.location.getPitch());
			preparedStatement.setString(8, this.getName());
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Updating the database : (jail='" + this.getName() + "';radius='" + this.getRadius() + "';location='" + this.getName() + "')");
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
}
