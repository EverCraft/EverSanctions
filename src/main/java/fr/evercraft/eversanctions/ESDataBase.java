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
}
