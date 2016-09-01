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
import java.sql.Statement;
import java.sql.Timestamp;

import fr.evercraft.everapi.exception.PluginDisableException;
import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.plugin.EDataBase;
import fr.evercraft.everapi.server.location.LocationSQL;
import fr.evercraft.everapi.services.essentials.Mail;

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
							"`uuid` varchar(36) NOT NULL," +
							"`staff` varchar(36) NOT NULL," +
							"`type` BOOL NOT NULL DEFAULT '0'," +
							"`date_start` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," + 
							"`date_end` timestamp," + 
							"`reason` varchar(255) NOT NULL," +
							"PRIMARY KEY (`id`));";
		initTable(this.getTableManual(), manual);
		
		this.table_auto = "auto";
		String auto = 	"CREATE TABLE IF NOT EXISTS <table> (" +
							"`id` MEDIUMINT NOT NULL AUTO_INCREMENT," +
							"`uuid` varchar(36) NOT NULL," +
							"`staff` varchar(36) NOT NULL," +
							"`type` varchar(36) NOT NULL," +
							"`date_start` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," + 
							"`ban` timestamp," + 
							"`mute` timestamp," + 
							"`jail` timestamp," + 
							"PRIMARY KEY (`id`));";
		initTable(this.getTableAuto(), auto);
		
		this.table_jails = "jails";
		String ignores = 	"CREATE TABLE IF NOT EXISTS <table> (" +
							"`identifier` varchar(36) NOT NULL," +
							"`radius` integer," +
							"`world` varchar(36) NOT NULL," +
							"`x` double NOT NULL," +
							"`y` double NOT NULL," +
							"`z` double NOT NULL," +
							"`yaw` double," +
							"`pitch` double," +
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
}
