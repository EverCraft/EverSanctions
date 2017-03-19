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

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.BanTypes;

import fr.evercraft.everapi.exception.PluginDisableException;
import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.EDataBase;
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;
import fr.evercraft.everapi.services.sanction.manual.SanctionManualProfile;
import fr.evercraft.everapi.sponge.UtilsNetwork;

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
							"`creation` DOUBLE NOT NULL," + 
							"`expiration` DOUBLE DEFAULT NULL," +
							"`type` VARCHAR(36) NOT NULL," +
							"`reason` VARCHAR(255) NOT NULL," +
							"`source` VARCHAR(36) NOT NULL," +
							"`context` VARCHAR(36) DEFAULT NULL," +
							"`pardon_date` DOUBLE DEFAULT NULL," +
							"`pardon_reason` VARCHAR(255) DEFAULT NULL," +
							"`pardon_source` VARCHAR(36) DEFAULT NULL," +
							"PRIMARY KEY (`identifier`, `creation`));";
		initTable(this.getTableManualProfile(), manual_profile);
		
		this.table_manual_ip = "manual_ip";
		String manual_ip = 	"CREATE TABLE IF NOT EXISTS <table> (" +
							"`identifier` VARCHAR(36) NOT NULL," +
							"`creation` DOUBLE NOT NULL," + 
							"`expiration` DOUBLE DEFAULT NULL," +
							"`reason` VARCHAR(255) NOT NULL," +
							"`source` VARCHAR(36) NOT NULL," +
							"`pardon_date` DOUBLE DEFAULT NULL," +
							"`pardon_reason` VARCHAR(255) DEFAULT NULL," +
							"`pardon_source` VARCHAR(36) DEFAULT NULL," +
							"PRIMARY KEY (`identifier`, `creation`));";
		initTable(this.getTableManualIp(), manual_ip);
		
		this.table_auto = "auto";
		String auto = 	"CREATE TABLE IF NOT EXISTS <table> (" +
							"`identifier` VARCHAR(36) NOT NULL," +
							"`creation` DOUBLE NOT NULL," + 
							"`expiration` DOUBLE DEFAULT NULL," +
							"`type` VARCHAR(36) NOT NULL," +
							"`reason` VARCHAR(36) NOT NULL," +
							"`source` VARCHAR(36) NOT NULL," +
							"`context` VARCHAR(36) DEFAULT NULL," +
							"`pardon_date` DOUBLE DEFAULT NULL," +
							"`pardon_reason` VARCHAR(255) DEFAULT NULL," +
							"`pardon_source` VARCHAR(36) DEFAULT NULL," +
							"PRIMARY KEY (`identifier`, `creation`));";
		initTable(this.getTableAuto(), auto);
		
		this.table_jails = "jails";
		String ignores = 	"CREATE TABLE IF NOT EXISTS <table> (" +
							"`identifier` VARCHAR(36) NOT NULL," +
							"`radius` INTEGER DEFAULT NULL," +
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

	public Map<Ban.Profile, UUID> getBansProfile(Connection connection) {
		Map<Ban.Profile, UUID> bans = new HashMap<Ban.Profile, UUID>();
		PreparedStatement preparedStatement = null;
		try {
			String query = "(SELECT `identifier`, `creation`, `expiration`, `reason`, `source` "
					+ "		FROM `" + this.plugin.getDataBase().getTableManualProfile() + "` "
					+ "		WHERE `type` = ? "
					+ "			AND `pardon_date` IS NULL "
					+ "			AND (`expiration` IS NULL OR `expiration` > ?))"
					+ "UNION"
					+ "(SELECT `identifier`, `creation`, `expiration`, `reason`, `source` "
					+ "		FROM `" + this.plugin.getDataBase().getTableAuto() + "` "
					+ "		WHERE (`type` = ? OR `type` = ?) "
					+ "			AND `pardon_date` IS NULL "
					+ "			AND (`expiration` IS NULL OR `expiration`  > ?));";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, SanctionManualProfile.Type.BAN_PROFILE.name());
			preparedStatement.setLong(2, System.currentTimeMillis());
			preparedStatement.setString(3, SanctionAuto.Type.BAN_PROFILE.name());
			preparedStatement.setString(4, SanctionAuto.Type.BAN_PROFILE_AND_IP.name());
			preparedStatement.setLong(5, System.currentTimeMillis());
			ResultSet list = preparedStatement.executeQuery();
			while(list.next()) {
				try {
					UUID uuid = UUID.fromString(list.getString("identifier"));
					Optional<GameProfile> profile = this.plugin.getEServer().getGameProfile(uuid);
					if(profile.isPresent()) {					
						Instant creation = Instant.ofEpochMilli(list.getLong("creation"));
						
						Ban.Builder build = Ban.builder()
								.type(BanTypes.PROFILE)
								.profile(profile.get())
								.startDate(creation)
								.reason(EChat.of(list.getString("reason")))
								.source(Text.of(list.getString("source")));
						
						long expiration = list.getLong("expiration");
						if(!list.wasNull()) {
							build = build.expirationDate(Instant.ofEpochMilli(expiration));
						}
						
						bans.put((Ban.Profile) build.build(), uuid);
					}
				} catch (IllegalArgumentException e) {}
			}
		} catch (SQLException e) {
	    	this.plugin.getELogger().warn("getBansProfile : " + e.getMessage());
		} finally {
			try { if (preparedStatement != null) preparedStatement.close(); } catch (SQLException e) {}
	    }
		return bans;
	}
	
	/*
	 * Ip
	 */
	
	public Map<Ban.Ip, String> getBansIp(Connection connection) {
		Map<Ban.Ip, String> bans = new HashMap<Ban.Ip, String>();
		bans.putAll(this.getBanIpProfile(connection));
		bans.putAll(this.getBanIp(connection));
		return bans;
	}
	
	public Map<Ban.Ip, String> getBanIpProfile(Connection connection) {
		Map<Ban.Ip, String> bans = new HashMap<Ban.Ip, String>();
		PreparedStatement preparedStatement = null;
		try {
			String query = "(SELECT `identifier`, `creation`, `expiration`, `reason`, `source`, `context` "
						+ "		FROM `" + this.plugin.getDataBase().getTableManualProfile() + "` "
						+ "		WHERE `type` = ? "
						+ "			AND `pardon_date` IS NULL "
						+ "			AND `context` IS NOT NULL "
						+ "			AND (`expiration` IS NULL OR `expiration` > ?))"
						+ "UNION"
						+ "(SELECT `identifier`, `creation`, `expiration`, `reason`, `source`, `context` "
						+ "		FROM `" + this.plugin.getDataBase().getTableAuto() + "` "
						+ "		WHERE (`type` = ? OR `type` = ?) "
						+ "			AND `pardon_date` IS NULL "
						+ "			AND `context` IS NOT NULL "
						+ "			AND (`expiration` IS NULL OR `expiration`  > ?));";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, SanctionManualProfile.Type.BAN_IP.name());
			preparedStatement.setLong(2, System.currentTimeMillis());
			preparedStatement.setString(3, SanctionAuto.Type.BAN_IP.name());
			preparedStatement.setString(4, SanctionAuto.Type.BAN_PROFILE_AND_IP.name());
			preparedStatement.setLong(5, System.currentTimeMillis());
			ResultSet list = preparedStatement.executeQuery();
			while(list.next()) {
				try {
					Optional<InetAddress> address = UtilsNetwork.getHost(list.getString("context"));
					this.plugin.getELogger().warn("getBanIpProfile");
					if(address.isPresent()) {
						this.plugin.getELogger().warn("getBanIpProfile : address");
						long creation = list.getLong("creation");
						
						Ban.Builder build = Ban.builder()
								.type(BanTypes.IP)
								.address(address.get())
								.startDate(Instant.ofEpochMilli(creation))
								.reason(EChat.of(list.getString("reason")))
								.source(Text.of(list.getString("source")));
						
						long expiration = list.getLong("expiration");
						if(!list.wasNull()) {
							build = build.expirationDate(Instant.ofEpochMilli(expiration));
						}
						bans.put((Ban.Ip) build.build(), list.getString("context"));
					}
				} catch (IllegalArgumentException e) {}
			}
		} catch (SQLException e) {
	    	this.plugin.getELogger().warn("getBanIpProfile : " + e.getMessage());
		} finally {
			try { if (preparedStatement != null) preparedStatement.close(); } catch (SQLException e) {}
	    }
		return bans;
	}
	
	public Map<Ban.Ip, String> getBanIp(Connection connection) {
		Map<Ban.Ip, String> bans = new HashMap<Ban.Ip, String>();
		PreparedStatement preparedStatement = null;
		try {
			String query = "SELECT * "
						+ "FROM `" + this.plugin.getDataBase().getTableManualIp() + "` "
						+ "WHERE `pardon_date` IS NULL "
						+ " AND (`expiration` IS NULL OR `expiration`  > ?);";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setLong(1, System.currentTimeMillis());
			ResultSet list = preparedStatement.executeQuery();
			while(list.next()) {
				try {
					Optional<InetAddress> address = UtilsNetwork.getHost(list.getString("identifier"));
					if(address.isPresent()) {					
						long creation = list.getLong("creation");
						
						Ban.Builder build = Ban.builder()
								.type(BanTypes.IP)
								.address(address.get())
								.startDate(Instant.ofEpochMilli(creation))
								.reason(EChat.of(list.getString("reason")))
								.source(Text.of(list.getString("source")));
						
						long expiration = list.getLong("expiration");
						if(!list.wasNull()) {
							build = build.expirationDate(Instant.ofEpochMilli(expiration));
						}
						
						bans.put((Ban.Ip) build.build(), list.getString("identifier"));
					}
				} catch (IllegalArgumentException e) {}
			}
		} catch (SQLException e) {
	    	this.plugin.getELogger().warn("getBanIp : " + e.getMessage());
		} finally {
			try { if (preparedStatement != null) preparedStatement.close(); } catch (SQLException e) {}
	    }
		return bans;
	}
}