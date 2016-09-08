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
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
	
	public Map<Ban.Ip, Optional<UUID>> getBansIp(Connection connection) {
		Map<Ban.Ip, Optional<UUID>> bans = new HashMap<Ban.Ip, Optional<UUID>>();
		bans.putAll(this.getBanIpManualProfile(connection));
		bans.putAll(this.getBanIpAuto(connection));
		bans.putAll(this.getBanIpManualIp(connection));
		return bans;
	}

	public Set<Ban.Profile> getBansProfile(Connection connection) {
		Set<Ban.Profile> bans = new HashSet<Ban.Profile>();
		bans.addAll(this.getBanProfileManual(connection));
		bans.addAll(this.getBanProfileAuto(connection));
		return bans;
	}
	
	/*
	 * Ip
	 */
	
	public Map<Ban.Ip, Optional<UUID>> getBanIpManualProfile(Connection connection) {
		Map<Ban.Ip, Optional<UUID>> bans = new HashMap<Ban.Ip, Optional<UUID>>();
		PreparedStatement preparedStatement = null;
		try {
			String query = "SELECT * "
						+ "FROM `" + this.plugin.getDataBase().getTableManualProfile() + "` "
						+ "WHERE `type` = ? "
						+ "	AND `pardon_date` IS NULL "
						+ "	AND `option` IS NOT NULL "
						+ " AND (`duration` IS NULL OR DATEADD(millisecond,`duration`,`creation`)  > NOW());";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, SanctionManualProfile.Type.BAN_IP.name());
			ResultSet list = preparedStatement.executeQuery();
			while(list.next()) {
				try {
					Optional<InetAddress> address = UtilsNetwork.getHost(list.getString("option"));
					if(address.isPresent()) {					
						long creation = list.getTimestamp("creation").getTime();
						
						Ban.Builder build = Ban.builder()
								.address(address.get())
								.type(BanTypes.IP)
								.startDate(Instant.ofEpochMilli(creation))
								.reason(EChat.of(list.getString("reason")))
								.source(Text.of(list.getString("source")));
						
						long duration = list.getLong("duration");
						if(!list.wasNull()) {
							build = build.expirationDate(Instant.ofEpochMilli(creation + duration));
						}
						
						bans.put((Ban.Ip) build.build(), Optional.of(UUID.fromString(list.getString("identifier"))));
					}
				} catch (IllegalArgumentException e) {}
			}
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn(" : " + e.getMessage());
		} finally {
			try { if (preparedStatement != null) preparedStatement.close(); } catch (SQLException e) {}
	    }
		return bans;
	}
	
	public Map<Ban.Ip, Optional<UUID>> getBanIpAuto(Connection connection) {
		Map<Ban.Ip, Optional<UUID>> bans = new HashMap<Ban.Ip, Optional<UUID>>();
		PreparedStatement preparedStatement = null;
		try {
			String query = "SELECT * "
						+ "FROM `" + this.plugin.getDataBase().getTableAuto() + "` "
						+ "WHERE (`type` = ? OR `type` = ?) "
						+ "	AND `pardon_date` IS NULL "
						+ "	AND `option` IS NOT NULL "
						+ " AND (`duration` IS NULL OR DATEADD(millisecond,`duration`,`creation`)  > NOW());";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, SanctionAuto.Type.BAN_IP.name());
			preparedStatement.setString(2, SanctionAuto.Type.BAN_PROFILE_AND_BAN_IP.name());
			ResultSet list = preparedStatement.executeQuery();
			while(list.next()) {
				try {
					Optional<InetAddress> address = UtilsNetwork.getHost(list.getString("option"));
					if(address.isPresent()) {					
						long creation = list.getTimestamp("creation").getTime();
						
						Ban.Builder build = Ban.builder()
								.address(address.get())
								.type(BanTypes.IP)
								.startDate(Instant.ofEpochMilli(creation))
								.reason(EChat.of(list.getString("reason")))
								.source(Text.of(list.getString("source")));
						
						long duration = list.getLong("duration");
						if(!list.wasNull()) {
							build = build.expirationDate(Instant.ofEpochMilli(creation + duration));
						}
						
						bans.put((Ban.Ip) build.build(), Optional.of(UUID.fromString(list.getString("identifier"))));
					}
				} catch (IllegalArgumentException e) {}
			}
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn(" : " + e.getMessage());
		} finally {
			try { if (preparedStatement != null) preparedStatement.close(); } catch (SQLException e) {}
	    }
		return bans;
	}
	
	public Map<Ban.Ip, Optional<UUID>> getBanIpManualIp(Connection connection) {
		Map<Ban.Ip, Optional<UUID>> bans = new HashMap<Ban.Ip, Optional<UUID>>();
		PreparedStatement preparedStatement = null;
		try {
			String query = "SELECT * "
						+ "FROM `" + this.plugin.getDataBase().getTableManualIp() + "` "
						+ "WHERE (`type` = ? OR `type` = ?) "
						+ "	AND `pardon_date` IS NULL "
						+ " AND (`duration` IS NULL OR DATEADD(millisecond,`duration`,`creation`)  > NOW());";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setLong(3, System.currentTimeMillis());
			ResultSet list = preparedStatement.executeQuery();
			while(list.next()) {
				try {
					Optional<InetAddress> address = UtilsNetwork.getHost(list.getString("identifier"));
					if(address.isPresent()) {					
						long creation = list.getTimestamp("creation").getTime();
						
						Ban.Builder build = Ban.builder()
								.address(address.get())
								.type(BanTypes.IP)
								.startDate(Instant.ofEpochMilli(creation))
								.reason(EChat.of(list.getString("reason")))
								.source(Text.of(list.getString("source")));
						
						long duration = list.getLong("duration");
						if(!list.wasNull()) {
							build = build.expirationDate(Instant.ofEpochMilli(creation + duration));
						}
						
						bans.put((Ban.Ip) build.build(), Optional.empty());
					}
				} catch (IllegalArgumentException e) {}
			}
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn(" : " + e.getMessage());
		} finally {
			try { if (preparedStatement != null) preparedStatement.close(); } catch (SQLException e) {}
	    }
		return bans;
	}
	
	/*
	 * Profile
	 */
	
	public Set<Ban.Profile> getBanProfileManual(Connection connection) {
		Set<Ban.Profile> bans = new HashSet<Ban.Profile>();
		PreparedStatement preparedStatement = null;
		try {
			String query = "SELECT * "
						+ "FROM `" + this.plugin.getDataBase().getTableManualProfile() + "` "
						+ "WHERE `type` = ? "
						+ "	AND `pardon_date` IS NULL "
						+ " AND (`duration` IS NULL OR DATEADD(millisecond,`duration`,`creation`)  > NOW());";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, SanctionManualProfile.Type.BAN_IP.name());
			ResultSet list = preparedStatement.executeQuery();
			while(list.next()) {
				try {
					Optional<GameProfile> profile = this.plugin.getEServer().getGameProfile(UUID.fromString(list.getString("identifier")));
					if(profile.isPresent()) {					
						long creation = list.getTimestamp("creation").getTime();
						
						Ban.Builder build = Ban.builder()
								.profile(profile.get())
								.type(BanTypes.PROFILE)
								.startDate(Instant.ofEpochMilli(creation))
								.reason(EChat.of(list.getString("reason")))
								.source(Text.of(list.getString("source")));
						
						long duration = list.getLong("duration");
						if(!list.wasNull()) {
							build = build.expirationDate(Instant.ofEpochMilli(creation + duration));
						}
						
						bans.add((Ban.Profile) build.build());
					}
				} catch (IllegalArgumentException e) {}
			}
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn(" : " + e.getMessage());
		} finally {
			try { if (preparedStatement != null) preparedStatement.close(); } catch (SQLException e) {}
	    }
		return bans;
	}
	
	public Set<Ban.Profile> getBanProfileAuto(Connection connection) {
		Set<Ban.Profile> bans = new HashSet<Ban.Profile>();
		PreparedStatement preparedStatement = null;
		try {
			String query = "SELECT * "
						+ "FROM `" + this.plugin.getDataBase().getTableAuto() + "` "
						+ "WHERE (`type` = ? OR `type` = ?) "
						+ "	AND `pardon_date` IS NULL "
						+ "	AND `option` IS NOT NULL "
						+ " AND (`duration` IS NULL OR DATEADD(millisecond,`duration`,`creation`)  > NOW());";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, SanctionAuto.Type.BAN_PROFILE.name());
			preparedStatement.setString(2, SanctionAuto.Type.BAN_PROFILE_AND_BAN_IP.name());
			ResultSet list = preparedStatement.executeQuery();
			while(list.next()) {
				try {
					Optional<GameProfile> profile = this.plugin.getEServer().getGameProfile(UUID.fromString(list.getString("identifier")));
					if(profile.isPresent()) {					
						long creation = list.getTimestamp("creation").getTime();
						
						Ban.Builder build = Ban.builder()
								.profile(profile.get())
								.type(BanTypes.PROFILE)
								.startDate(Instant.ofEpochMilli(creation))
								.reason(EChat.of(list.getString("reason")))
								.source(Text.of(list.getString("source")));
						
						long duration = list.getLong("duration");
						if(!list.wasNull()) {
							build = build.expirationDate(Instant.ofEpochMilli(creation + duration));
						}
						
						bans.add((Ban.Profile) build.build());
					}
				} catch (IllegalArgumentException e) {}
			}
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn(" : " + e.getMessage());
		} finally {
			try { if (preparedStatement != null) preparedStatement.close(); } catch (SQLException e) {}
	    }
		return bans;
	}
}