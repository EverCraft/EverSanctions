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

import fr.evercraft.everapi.exception.PluginDisableException;
import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.plugin.EDataBase;

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
							"`id` MEDIUMINT NOT NULL AUTO_INCREMENT," +
							"`uuid` VARCHAR(36) NOT NULL," +
							"`staff` VARCHAR(36) NOT NULL," +
							"`sanction` VARCHAR(36) NOT NULL," +
							"`date_start` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," + 
							"`date_end` TIMESTAMP," + 
							"`reason` VARCHAR(255) NOT NULL," +
							"PRIMARY KEY (`id`));";
		initTable(this.getTableManual(), manual);
		
		this.table_auto = "auto";
		String auto = 	"CREATE TABLE IF NOT EXISTS <table> (" +
							"`id` VARCHAR NOT NULL AUTO_INCREMENT," +
							"`uuid` VARCHAR(36) NOT NULL," +
							"`staff` VARCHAR(36) NOT NULL," +
							"`type` VARCHAR(36) NOT NULL," +
							"`date_start` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," + 
							"`ban` TIMESTAMP," + 
							"`mute` TIMESTAMP," + 
							"`jail` TIMESTAMP," + 
							"PRIMARY KEY (`id`));";
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
	
	public void addManual(final String identifier, final String staff, final SanctionType sanction, long date_start, long date_end, String reason) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.getConnection();
    		String query = 	  "INSERT INTO `" + this.getTableManual() + "` "
    						+ "VALUES (?, ?, ?, ?, ?, ?);";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, identifier);
			preparedStatement.setString(2, staff);
			preparedStatement.setString(3, sanction.name());
			preparedStatement.setTimestamp(4, new Timestamp(date_start));
			preparedStatement.setTimestamp(5, new Timestamp(date_end));
			preparedStatement.setString(6, reason);
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Adding to the database : (identifier='" + identifier + "';"
																  + "staff='" + staff + "';"
																  + "sanction='" + sanction.name() + "';"
																  + "date_start='" + date_start + "';"
																  + "date_end='" + date_end + "';"
																  + "reason='" + reason + "')");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of ignore : " + e.getMessage());
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (connection != null) connection.close();
			} catch (SQLException e) {}
	    }
	}
	
	public void removeManual(final String identifier, final String ignore) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.getTableManual() + "` "
		    				+ "WHERE `uuid` = ? AND `ignore` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, identifier);
			preparedStatement.setString(2, ignore);
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (identifier='" + identifier + "';ignore='" + ignore + "')");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of ignore : " + e.getMessage());
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
