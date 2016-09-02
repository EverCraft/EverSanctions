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
package fr.evercraft.eversanctions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.spongepowered.api.text.Text;

import fr.evercraft.everapi.exception.PluginDisableException;
import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.EDataBase;
import fr.evercraft.everapi.server.location.LocationSQL;
import fr.evercraft.everapi.services.sanction.manual.SanctionManualProfile;
import fr.evercraft.everapi.services.sanction.manual.SanctionManualType;
import fr.evercraft.eversanctions.service.EJail;
import fr.evercraft.eversanctions.service.auto.EAuto;
import fr.evercraft.eversanctions.service.manual.EManual;

public class ESDataBase extends EDataBase<EverSanctions> {
	private String table_manual;
	private String table_auto;
	private String table_jails;

	public ESDataBase(final EverSanctions plugin) throws PluginDisableException {
		super(plugin, true);
	}

	public boolean init() throws ServerDisableException {
		this.table_manual = "manual";
		String manual = 	"CREATE TABLE IF NOT EXISTS <table> (" +
							"`uuid` VARCHAR(36) NOT NULL," +
							"`date_creation` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," + 
							"`date_duration` DOUBLE," +
							"`type` VARCHAR(36) NOT NULL," +
							"`reason` VARCHAR(255) NOT NULL," +
							"`source` VARCHAR(36) NOT NULL," +
							"`option` VARCHAR(36) NOT NULL," +
							"`pardon_date` TIMESTAMP NOT NULL," +
							"`pardon_reason` VARCHAR(255) NOT NULL," +
							"`pardon_source` VARCHAR(36) NOT NULL," +
							"PRIMARY KEY (`id`, `date_creation`));";
		initTable(this.getTableManual(), manual);
		
		this.table_auto = "auto";
		String auto = 	"CREATE TABLE IF NOT EXISTS <table> (" +
							"`uuid` VARCHAR(36) NOT NULL," +
							"`date_creation` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," + 
							"`date_duration` DOUBLE," +
							"`type` VARCHAR(36) NOT NULL," +
							"`reason` VARCHAR(36) NOT NULL," +
							"`source` VARCHAR(36) NOT NULL," +
							"`pardon_date` TIMESTAMP," +
							"`pardon_reason` VARCHAR(255)," +
							"`pardon_source` VARCHAR(36)," +
							"PRIMARY KEY (`id`, `date_creation`));";
		initTable(this.getTableAuto(), auto);
		
		this.table_jails = "jails";
		String ignores = 	"CREATE TABLE IF NOT EXISTS <table> (" +
							"`identifier` VARCHAR(36) NOT NULL," +
							"`radius` INTEGER," +
							"`world` VARCHAR(36) NOT NULL," +
							"`x` DOUBLE NOT NULL," +
							"`y` DOUBLE NOT NULL," +
							"`z` DOUBLE NOT NULL," +
							"`yaw` DOUBLE," +
							"`pitch` DOUBLE," +
							"PRIMARY KEY (`uuid`, `ignore`));";
		initTable(this.getTableJails(), ignores);
		
		return true;
	}
	
	public String getTableManual() {
		return this.getPrefix() + this.table_manual;
	}
	
	public String getTableAuto() {
		return this.getPrefix() + this.table_auto;
	}
	
	public String getTableJails() {
		return this.getPrefix() + this.table_jails;
	}	
	
	/*
	 * Manual
	 */
	
	public void addManual(final String identifier, final EManual ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
		Optional<String> option = Optional.empty();
		if(ban.getType().equals(SanctionManualType.JAIL)) {
			option = Optional.of(((SanctionManualProfile.Jail) ban).getJailName());
		}
		
    	try {    		
    		connection = this.getConnection();
    		String query = 	  "INSERT INTO `" + this.getTableManual() + "` "
    						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, identifier);
			preparedStatement.setTimestamp(2, new Timestamp(ban.getCreationDate()));
			preparedStatement.setLong(3, ban.getDuration().orElse(-1L));
			preparedStatement.setString(4, ban.getType().name());
			preparedStatement.setString(5, EChat.serialize(ban.getReason()));
			preparedStatement.setString(6, ban.getSource());
			preparedStatement.setString(7, option.orElse(null));
			
			if(ban.isPardon()) {
				preparedStatement.setTimestamp(8, new Timestamp(ban.getPardonDate().get()));
				preparedStatement.setString(9, EChat.serialize(ban.getPardonReason().get()));
				preparedStatement.setString(10, ban.getPardonSource().get());
			} else {
				preparedStatement.setTimestamp(8, null);
				preparedStatement.setString(9, null);
				preparedStatement.setString(10, null);
			}
			preparedStatement.execute();
			this.plugin.getLogger().debug("Adding to the database : (uuid ='" + identifier + "';"
					 											  + "date_creation='" + ban.getCreationDate() + "';"
					 											  + "date_duration='" + ban.getExpirationDate().orElse(-1L) + "';"
					 											  + "type='" + ban.getType().name() + "';"
					 											  + "reason='" + EChat.serialize(ban.getReason()) + "';"
					 											  + "source='" + ban.getCreationDate() + "';"
					 											  + "option='" + option.orElse("null") + "';"
					 											  + "pardon_date='" + ban.getPardonDate().orElse(-1L) + "';"
					 											  + "pardon_reason='" + ban.getPardonReason().orElse(Text.EMPTY) + "';"
																  + "pardon_source='" + ban.getPardonSource().orElse("") + "')");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of manual : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
	
	public void pardonManual(final String identifier, final EManual ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
    	try {    		
    		connection = this.getConnection();
    		String query = 	  "UPDATE `" + this.getTableManual() + "` "
    						+ "SET pardon_date = ? ,"
    							+ "pardon_reason = ? ,"
    							+ "pardon_source = ? "
    						+ "WHERE uuid = ?"
    							+ "date_creation = ? ;";
    		preparedStatement = connection.prepareStatement(query);

			if(ban.isPardon()) {
				preparedStatement.setTimestamp(1, new Timestamp(ban.getPardonDate().get()));
				preparedStatement.setString(2, EChat.serialize(ban.getPardonReason().get()));
				preparedStatement.setString(3, ban.getPardonSource().get());
			} else {
				preparedStatement.setTimestamp(1, null);
				preparedStatement.setString(2, null);
				preparedStatement.setString(3, null);
			}
			preparedStatement.setString(4, identifier);
			preparedStatement.setTimestamp(5, new Timestamp(ban.getCreationDate()));
			preparedStatement.execute();
			this.plugin.getLogger().debug("Updating to the database : (uuid ='" + identifier + "';"
					 											  + "date_creation='" + ban.getCreationDate() + "';"
					 											  + "pardon_date='" + ban.getPardonDate().orElse(-1L) + "';"
					 											  + "pardon_reason='" + ban.getPardonReason().orElse(Text.EMPTY) + "';"
																  + "pardon_source='" + ban.getPardonSource().orElse("") + "')");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of manual : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
	
	public void removeManual(final String identifier, final EManual ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.getTableManual() + "` "
		    				+ "WHERE `uuid` = ? AND `date_creation` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, identifier);
			preparedStatement.setTimestamp(2, new Timestamp(ban.getCreationDate()));
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (uuid ='" + identifier + "';"
					 											  + "date_creation='" + ban.getCreationDate() + "';"
					 											  + "date_duration='" + ban.getExpirationDate().orElse(-1L) + "';"
					 											  + "type='" + ban.getType().name() + "';"
					 											  + "reason='" + EChat.serialize(ban.getReason()) + "';"
					 											  + "source='" + ban.getCreationDate() + "';"
					 											  + "pardon_date='" + ban.getPardonDate().orElse(-1L) + "';"
					 											  + "pardon_reason='" + ban.getPardonReason().orElse(Text.EMPTY) + "';"
																  + "pardon_source='" + ban.getPardonSource().orElse("") + "')");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of manual : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
	
	public void removeManual(final String identifier) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.getTableManual() + "` "
		    				+ "WHERE `uuid` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, identifier);
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (uuid ='" + identifier + "';");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of manual : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
	
	/*
	 * Auto
	 */
	
	public void addAuto(final String identifier, final EAuto ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
    	try {    		
    		connection = this.getConnection();
    		String query = 	  "INSERT INTO `" + this.getTableAuto() + "` "
    						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, identifier);
			preparedStatement.setTimestamp(2, new Timestamp(ban.getCreationDate()));
			preparedStatement.setLong(3, ban.getDuration().orElse(-1L));
			preparedStatement.setString(4, ban.getType().name());
			preparedStatement.setString(5, ban.getReason().getName());
			preparedStatement.setString(6, ban.getSource());
			
			if(ban.isPardon()) {
				preparedStatement.setTimestamp(7, new Timestamp(ban.getPardonDate().get()));
				preparedStatement.setString(8, EChat.serialize(ban.getPardonReason().get()));
				preparedStatement.setString(9, ban.getPardonSource().get());
			} else {
				preparedStatement.setTimestamp(7, null);
				preparedStatement.setString(8, null);
				preparedStatement.setString(9, null);
			}
			preparedStatement.execute();
			this.plugin.getLogger().debug("Adding to the database : (uuid ='" + identifier + "';"
					 											  + "date_creation='" + ban.getCreationDate() + "';"
					 											  + "date_duration='" + ban.getExpirationDate().orElse(-1L) + "';"
					 											  + "type='" + ban.getType().name() + "';"
					 											  + "reason='" + ban.getReason().getName() + "';"
					 											  + "source='" + ban.getCreationDate() + "';"
					 											  + "pardon_date='" + ban.getPardonDate().orElse(-1L) + "';"
					 											  + "pardon_reason='" + ban.getPardonReason().orElse(Text.EMPTY) + "';"
																  + "pardon_source='" + ban.getPardonSource().orElse("") + "')");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of manual : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
	
	public void pardonAuto(final String identifier, final EAuto ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
    	try {    		
    		connection = this.getConnection();
    		String query = 	  "UPDATE `" + this.getTableAuto() + "` "
    						+ "SET pardon_date = ? ,"
    							+ "pardon_reason = ? ,"
    							+ "pardon_source = ? "
    						+ "WHERE uuid = ?"
    							+ "date_creation = ? ;";
    		preparedStatement = connection.prepareStatement(query);

			if(ban.isPardon()) {
				preparedStatement.setTimestamp(1, new Timestamp(ban.getPardonDate().get()));
				preparedStatement.setString(2, EChat.serialize(ban.getPardonReason().get()));
				preparedStatement.setString(3, ban.getPardonSource().get());
			} else {
				preparedStatement.setTimestamp(1, null);
				preparedStatement.setString(2, null);
				preparedStatement.setString(3, null);
			}
			preparedStatement.setString(4, identifier);
			preparedStatement.setTimestamp(5, new Timestamp(ban.getCreationDate()));
			preparedStatement.execute();
			this.plugin.getLogger().debug("Updating to the database : (uuid ='" + identifier + "';"
					 											  + "date_creation='" + ban.getCreationDate() + "';"
					 											  + "pardon_date='" + ban.getPardonDate().orElse(-1L) + "';"
					 											  + "pardon_reason='" + ban.getPardonReason().orElse(Text.EMPTY) + "';"
																  + "pardon_source='" + ban.getPardonSource().orElse("") + "')");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of manual : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
	
	public void removeAuto(final String identifier, final EAuto ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.getTableAuto() + "` "
		    				+ "WHERE `uuid` = ? AND `date_creation` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, identifier);
			preparedStatement.setTimestamp(2, new Timestamp(ban.getCreationDate()));
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (uuid ='" + identifier + "';"
																  + "date_creation='" + ban.getCreationDate() + "';"
																  + "date_duration='" + ban.getExpirationDate().orElse(-1L) + "';"
																  + "type='" + ban.getType().name() + "';"
																  + "reason='" + ban.getReason().getName() + "';"
																  + "source='" + ban.getCreationDate() + "';"
																  + "pardon_date='" + ban.getPardonDate().orElse(-1L) + "';"
																  + "pardon_reason='" + ban.getPardonReason().orElse(Text.EMPTY) + "';"
																  + "pardon_source='" + ban.getPardonSource().orElse("") + "')");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of manual : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
	
	public void removeAuto(final String identifier) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.getTableAuto() + "` "
		    				+ "WHERE `uuid` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, identifier);
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (uuid ='" + identifier + "';");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of manual : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
	
	/*
	 * Jails
	 */
	
	public Map<String, EJail> selectJails() {
		Map<String, EJail> jails = new HashMap<String, EJail>();
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.getConnection();
    		String query = 	  "SELECT *" 
							+ "FROM `" + this.getTableJails() + "` ;";
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
	
	public void addJail(final EJail jail) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.getConnection();
    		String query = 	  "INSERT INTO `" + this.getTableJails() + "` "
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
	
	public void updateJail(final EJail jail) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.getConnection();
    		String query = 	  "UPDATE `" + this.getTableJails() + "` "
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
	
	public void removeJail(final String identifier) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.getTableJails() + "` "
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
	
	public void clearJail() {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.getConnection();
    		String query = 	  "TRUNCATE `" + this.getTableJails() + "` ;";
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
