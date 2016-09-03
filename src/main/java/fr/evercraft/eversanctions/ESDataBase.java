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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

import org.spongepowered.api.text.Text;

import fr.evercraft.everapi.exception.PluginDisableException;
import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.EDataBase;
import fr.evercraft.everapi.services.sanction.manual.SanctionManualProfile;
import fr.evercraft.eversanctions.service.auto.EAuto;
import fr.evercraft.eversanctions.service.manual.EManualProfile;
import fr.evercraft.eversanctions.service.manual.EManualProfileBanIp;
import fr.evercraft.eversanctions.service.manual.EManualProfileJail;

public class ESDataBase extends EDataBase<EverSanctions> {
	private String table_manual_profile;
	private String table_manual_ip;
	private String table_auto;
	private String table_jails;

	public ESDataBase(final EverSanctions plugin) throws PluginDisableException {
		super(plugin, true);
	}

	public boolean init() throws ServerDisableException {
		this.table_manual_profile = "manual_profile";
		String manual_profile = "CREATE TABLE IF NOT EXISTS <table> (" +
							"`identifier` VARCHAR(36) NOT NULL," +
							"`creation` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," + 
							"`duration` DOUBLE," +
							"`type` VARCHAR(36) NOT NULL," +
							"`reason` VARCHAR(255) NOT NULL," +
							"`source` VARCHAR(36) NOT NULL," +
							"`option` VARCHAR(36)," +
							"`pardon_date` TIMESTAMP NOT NULL," +
							"`pardon_reason` VARCHAR(255) NOT NULL," +
							"`pardon_source` VARCHAR(36) NOT NULL," +
							"PRIMARY KEY (`identifier`, `creation`));";
		initTable(this.getTableManualProfile(), manual_profile);
		
		this.table_manual_ip = "manual_ip";
		String manual_ip = 	"CREATE TABLE IF NOT EXISTS <table> (" +
							"`identifier` VARCHAR(36) NOT NULL," +
							"`creation` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," + 
							"`duration` DOUBLE," +
							"`reason` VARCHAR(255) NOT NULL," +
							"`source` VARCHAR(36) NOT NULL," +
							"`pardon_date` TIMESTAMP NOT NULL," +
							"`pardon_reason` VARCHAR(255) NOT NULL," +
							"`pardon_source` VARCHAR(36) NOT NULL," +
							"PRIMARY KEY (`identifier`, `creation`));";
		initTable(this.getTableManualIp(), manual_ip);
		
		this.table_auto = "auto";
		String auto = 	"CREATE TABLE IF NOT EXISTS <table> (" +
							"`identifier` VARCHAR(36) NOT NULL," +
							"`creation` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," + 
							"`duration` DOUBLE," +
							"`type` VARCHAR(36) NOT NULL," +
							"`reason` VARCHAR(36) NOT NULL," +
							"`source` VARCHAR(36) NOT NULL," +
							"`option` VARCHAR(36)," +
							"`pardon_date` TIMESTAMP," +
							"`pardon_reason` VARCHAR(255)," +
							"`pardon_source` VARCHAR(36)," +
							"PRIMARY KEY (`identifier`, `creation`));";
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
							"PRIMARY KEY (`identifier`));";
		initTable(this.getTableJails(), ignores);
		
		return true;
	}
	
	public String getTableManualProfile() {
		return this.getPrefix() + this.table_manual_profile;
	}
	
	public String getTableManualIp() {
		return this.getPrefix() + this.table_manual_ip;
	}
	
	public String getTableAuto() {
		return this.getPrefix() + this.table_auto;
	}
	
	public String getTableJails() {
		return this.getPrefix() + this.table_jails;
	}	
	
	/*
	 * Manual Profile
	 */
	
	public void addManualProfile(final String identifier, final EManualProfile ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
		Optional<String> option = Optional.empty();
		if(ban.getType().equals(SanctionManualProfile.Type.JAIL)) {
			option = Optional.of(((EManualProfileJail) ban).getJailName());
		} else if(ban.getType().equals(SanctionManualProfile.Type.BAN_IP)) {
			option = Optional.of(((EManualProfileBanIp) ban).getAddress().getHostAddress());
		}
		
    	try {    		
    		connection = this.getConnection();
    		String query = 	  "INSERT INTO `" + this.getTableManualProfile() + "` "
    						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, identifier);
			preparedStatement.setTimestamp(2, new Timestamp(ban.getCreationDate()));
			preparedStatement.setLong(3, ban.getDuration().orElse(null));
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
			this.plugin.getLogger().debug("Adding to the database : (identifier ='" + identifier + "';"
					 											  + "creation='" + ban.getCreationDate() + "';"
					 											  + "duration='" + ban.getDuration().orElse(-1L) + "';"
					 											  + "type='" + ban.getType().name() + "';"
					 											  + "reason='" + EChat.serialize(ban.getReason()) + "';"
					 											  + "source='" + ban.getCreationDate() + "';"
					 											  + "option='" + option.orElse("null") + "';"
					 											  + "pardon_date='" + ban.getPardonDate().orElse(-1L) + "';"
					 											  + "pardon_reason='" + ban.getPardonReason().orElse(Text.EMPTY) + "';"
																  + "pardon_source='" + ban.getPardonSource().orElse("null") + "')");
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
	
	public void pardonManualProfile(final String identifier, final EManualProfile ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
    	try {    		
    		connection = this.getConnection();
    		String query = 	  "UPDATE `" + this.getTableManualProfile() + "` "
    						+ "SET pardon_date = ? ,"
    							+ "pardon_reason = ? ,"
    							+ "pardon_source = ? "
    						+ "WHERE uuid = ? AND creation = ? ;";
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
			this.plugin.getLogger().debug("Updating to the database : (identifier ='" + identifier + "';"
					 											  + "creation='" + ban.getCreationDate() + "';"
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
	
	public void removeManualProfile(final String identifier, final EManualProfile ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.getTableManualProfile() + "` "
		    				+ "WHERE `uuid` = ? AND `creation` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, identifier);
			preparedStatement.setTimestamp(2, new Timestamp(ban.getCreationDate()));
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (uuid ='" + identifier + "';"
					 											  + "creation='" + ban.getCreationDate() + "';"
					 											  + "duration='" + ban.getDuration().orElse(-1L) + "';"
					 											  + "type='" + ban.getType().name() + "';"
					 											  + "reason='" + EChat.serialize(ban.getReason()) + "';"
					 											  + "source='" + ban.getCreationDate() + "';"
					 											  + "pardon_date='" + ban.getPardonDate().orElse(-1L) + "';"
					 											  + "pardon_reason='" + ban.getPardonReason().orElse(Text.EMPTY) + "';"
																  + "pardon_source='" + ban.getPardonSource().orElse("null") + "')");
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
	
	public void removeManualProfile(final String identifier) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.getTableManualProfile() + "` "
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
			preparedStatement.setLong(3, ban.getDuration().orElse(null));
			preparedStatement.setString(4, ban.getType().name());
			preparedStatement.setString(5, ban.getReason().getName());
			preparedStatement.setString(6, ban.getSource());
			preparedStatement.setString(7, ban.getOption().orElse(null));
			
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
					 											  + "creation='" + ban.getCreationDate() + "';"
					 											  + "duration='" + ban.getDuration().orElse(-1L) + "';"
					 											  + "type='" + ban.getType().name() + "';"
					 											  + "reason='" + ban.getReason().getName() + "';"
					 											  + "source='" + ban.getCreationDate() + "';"
					 											  + "option='" + ban.getOption().orElse("null") + "';"
					 											  + "pardon_date='" + ban.getPardonDate().orElse(-1L) + "';"
					 											  + "pardon_reason='" + ban.getPardonReason().orElse(Text.EMPTY) + "';"
																  + "pardon_source='" + ban.getPardonSource().orElse("null") + "')");
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
    						+ "WHERE uuid = ? AND creation = ? ;";
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
					 											  + "creation='" + ban.getCreationDate() + "';"
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
		    				+ "WHERE `uuid` = ? AND `creation` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, identifier);
			preparedStatement.setTimestamp(2, new Timestamp(ban.getCreationDate()));
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (uuid ='" + identifier + "';"
																  + "creation='" + ban.getCreationDate() + "';"
																  + "duration='" + ban.getDuration().orElse(-1L) + "';"
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
}
