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
package fr.evercraft.eversanctions.service.subject;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.services.sanction.Sanction;
import fr.evercraft.everapi.services.sanction.SanctionIpSubject;
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;
import fr.evercraft.everapi.services.sanction.manual.SanctionManualIP;
import fr.evercraft.everapi.services.sanction.manual.SanctionManualProfile;
import fr.evercraft.everapi.sponge.UtilsNetwork;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.auto.EAuto;
import fr.evercraft.eversanctions.service.manual.EManualIP;
import fr.evercraft.eversanctions.service.manual.EManualProfileBanIp;

public class EIpSubject implements SanctionIpSubject {
	
	private final EverSanctions plugin;
	private final InetSocketAddress socket;
	private final String identifier;
	
	private final ConcurrentSkipListSet<EManualIP> ip_manual;
	private final ConcurrentSkipListSet<EManualProfileBanIp> profile_manual;
	private final ConcurrentSkipListSet<EAuto> profile_auto;
	
	

	public EIpSubject(final EverSanctions plugin, final InetAddress address) {
		this.plugin = plugin;
		this.socket = UtilsNetwork.getSocketAddress(address);
		this.identifier = UtilsNetwork.getHostString(address);
		
		this.ip_manual = new ConcurrentSkipListSet<EManualIP>((EManualIP o1, EManualIP o2) -> o2.getCreationDate().compareTo(o1.getCreationDate()));
		this.profile_manual = new ConcurrentSkipListSet<EManualProfileBanIp>((EManualProfileBanIp o1, EManualProfileBanIp o2) -> o2.getCreationDate().compareTo(o1.getCreationDate()));
		this.profile_auto = new ConcurrentSkipListSet<EAuto>((EAuto o1, EAuto o2) -> o2.getCreationDate().compareTo(o1.getCreationDate()));
		
		this.reload();
	}
	
	public void reload() {
		Connection connection = null;
		try {
			connection = this.plugin.getDataBase().getConnection();
			
			this.reload(connection);
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {if (connection != null) connection.close();} catch (SQLException e) {}
	    }
	}
	
	public void reload(Connection connection) {
		this.ip_manual.clear();
		this.profile_manual.clear();
		this.profile_auto.clear();
		
		this.ip_manual.addAll(this.sqlIpManual(connection));
		this.profile_manual.addAll(this.sqlProfileManual(connection));
		this.profile_auto.addAll(this.sqlProfileAuto(connection));
	}
	
	@Override
	public Collection<Sanction> getAllManuals() {
		Builder<Sanction> builder = new ImmutableList.Builder<Sanction>();
		builder.addAll(this.ip_manual);
		builder.addAll(this.profile_manual);
		return builder.build();
	}
	
	@Override
	public Collection<SanctionAuto> getAllAutos() {
		return ImmutableList.copyOf(this.profile_auto);
	}

	public boolean isBanIpManual() {
		return this.ip_manual.stream().filter(manual -> !manual.isExpire()).findFirst().isPresent();
	}
	
	public boolean isBanProfileManual() {
		return this.profile_manual.stream().filter(manual -> !manual.isExpire()).findFirst().isPresent();
	}
	
	@Override
	public boolean isBanManual() {
		return this.isBanIpManual() || this.isBanProfileManual();
	}
	
	@Override
	public boolean isBanAuto() {		
		return this.profile_auto.stream().filter(manual -> !manual.isExpire()).findFirst().isPresent();
	}
	
	public boolean add(final EManualProfileBanIp manual) {
		if (this.profile_manual.contains(manual)) {
			this.profile_manual.add(manual);
			return true;
		}
		return false;
	}
	
	public boolean add(final EAuto manual) {
		if (this.profile_auto.contains(manual)) {
			this.profile_auto.add(manual);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean ban(final long creation, final Optional<Long> expiration, final Text reason, final CommandSource source) {
		Preconditions.checkNotNull(expiration, "expiration");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");

		// L'ip est déjà banni
		if (this.isBanIpManual()) {
			return false;
		}
		
		final EManualIP ban = new EManualIP(creation, expiration, reason, source.getIdentifier());
		
		// Event cancel
		if (Sponge.getEventManager().post(SpongeEventFactory.createBanIpEvent(Cause.source(this).build(), ban.getBan(this.getAddress())))) {
			return false;
		}
		
		this.ip_manual.add(ban);
		this.plugin.getThreadAsync().execute(() -> this.sqlAdd(ban));
		return true;
	}
	
	@Override
	public Collection<Sanction> pardonBan(final long date, final Text reason, final CommandSource source) {
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		ImmutableList.Builder<Sanction> pardons = ImmutableList.builder();
		
		Optional<EManualIP> ip = this.ip_manual.stream().filter(ban -> !ban.isExpire()).findFirst();
		List<EManualProfileBanIp> profiles = this.profile_manual.stream().filter(manual -> !manual.isExpire()).collect(Collectors.toList());
		
		if (ip.isPresent()) {
			ip.get().pardon(date, reason, source.getIdentifier());
			this.plugin.getSanctionService().remove(ip.get().getBan(this.getAddress()));
			this.plugin.getThreadAsync().execute(() -> this.sqlUpdate(ip.get()));
		}
		
		profiles.forEach(ban -> {
			Optional<EUserSubject> subject = this.plugin.getSanctionService().getSubject(ban.getProfile());
			if (subject.isPresent()) {
				Optional<SanctionManualProfile.BanIp> pardon = subject.get().pardonBanIp(this.getAddress(), date, reason, source);
				if(pardon.isPresent()) {
					pardons.add(pardon.get());
				}
			}
		});
		
		return pardons.build();
	}
	
	public boolean remove(final SanctionManualIP ban) {
		Preconditions.checkNotNull(ban, "ban");
		
		if (this.ip_manual.remove(ban)) {
			this.plugin.getThreadAsync().execute(() -> this.sqlRemove(ban));
			return true;
		}
		return false;
	}
	
	public boolean clear() {
		if (!this.ip_manual.isEmpty()) {
			this.plugin.getThreadAsync().execute(() -> this.sqlClear());
			return true;
		}
		return false;
	}
	
	/*
	 * Assesseur
	 */
	
	public InetAddress getAddress() {
		return this.socket.getAddress();
	}
	
	public InetSocketAddress getSocketAddress() {
		return this.socket;
	}
	
	public String getIdentifier() {
		return this.identifier;
	}
	
	/*
	 * Database
	 */
	
	private Collection<EManualIP> sqlIpManual(final Connection connection) {
		Collection<EManualIP> ips = new ArrayList<EManualIP>();
		PreparedStatement preparedStatement = null;
		try {
			String query = "SELECT * "
						+ "FROM `" + this.plugin.getDataBase().getTableManualIp() + "` "
						+ "WHERE `identifier` = ? "
						+ "ORDER BY `creation` ASC;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			ResultSet list = preparedStatement.executeQuery();
			while(list.next()) {
				Long creation = list.getLong("creation");
				Text reason = EChat.of(list.getString("reason"));
				String source = list.getString("source");
				Optional<Text> pardon_reason = Optional.ofNullable(EChat.of(list.getString("pardon_reason")));
				Optional<String> pardon_source = Optional.ofNullable(list.getString("pardon_source"));
				
				Optional<Long> expiration = Optional.of(list.getLong("expiration"));
				if(list.wasNull()) {
					expiration = Optional.empty();
				}
				Optional<Long> pardon_date = Optional.of(list.getLong("pardon_date"));
				if(list.wasNull()) {
					pardon_date = Optional.empty();
				}
				
				ips.add(new EManualIP(creation, expiration, reason, source, pardon_date, pardon_reason, pardon_source));
			}
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn("Error during a change of manual_ip : (identifier='" + this.getIdentifier() + "'): " + e.getMessage());
		} finally {
			try { if (preparedStatement != null) preparedStatement.close(); } catch (SQLException e) {}
	    }
		return ips;
	}
	
	private Collection<EManualProfileBanIp> sqlProfileManual(final Connection connection) {
		Collection<EManualProfileBanIp> profiles = new ArrayList<EManualProfileBanIp>();
		PreparedStatement preparedStatement = null;
		try {
			String query = "SELECT * "
						+ "FROM `" + this.plugin.getDataBase().getTableManualProfile() + "` "
						+ "WHERE `type` = ? "
						+ " AND `context` = ? "
						+ "ORDER BY `creation` ASC;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, SanctionManualProfile.Type.BAN_IP.name());
			preparedStatement.setString(2, this.getIdentifier());
			ResultSet list = preparedStatement.executeQuery();
			while(list.next()) {
				try {
					UUID uuid = UUID.fromString(list.getString("identifier"));
					Long creation = list.getLong("creation");
					Text reason = EChat.of(list.getString("reason"));
					String source = list.getString("source");
					Optional<String> pardon_source = Optional.ofNullable(list.getString("pardon_source"));
					
					Optional<Text> pardon_reason = Optional.empty();
					if(list.getString("pardon_reason") != null) {
						pardon_reason = Optional.ofNullable(EChat.of(list.getString("pardon_reason")));
					}
					Optional<Long> expiration = Optional.of(list.getLong("expiration"));
					if(list.wasNull()) {
						expiration = Optional.empty();
					}
					Optional<Long> pardon_date = Optional.of(list.getLong("pardon_date"));
					if(list.wasNull()) {
						pardon_date = Optional.empty();
					}
					
					profiles.add(new EManualProfileBanIp(uuid, this.getAddress(), creation, expiration, reason, source, pardon_date, pardon_reason, pardon_source));
				} catch (IllegalArgumentException e) {}
			}
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn("Error during a change of manual_ip : (identifier='" + this.getIdentifier() + "'): " + e.getMessage());
		} finally {
			try {if (preparedStatement != null) preparedStatement.close();} catch (SQLException e) {}
	    }
		return profiles;
	}
	
	private Collection<EAuto> sqlProfileAuto(final Connection connection) {
		Collection<EAuto> profiles = new ArrayList<EAuto>();
		PreparedStatement preparedStatement = null;
		try {
			String query = "SELECT * "
						+ "FROM `" + this.plugin.getDataBase().getTableAuto() + "` "
						+ "WHERE (`type` = ? OR `type` = ?)"
						+ " AND `context` = ? "
						+ "ORDER BY `creation` ASC;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, SanctionAuto.Type.BAN_IP.name());
			preparedStatement.setString(2, SanctionAuto.Type.BAN_PROFILE_AND_IP.name());
			preparedStatement.setString(3, this.getIdentifier());
			ResultSet list = preparedStatement.executeQuery();
			
			Map<SanctionAuto.Type, Integer> levels = new HashMap<SanctionAuto.Type, Integer>();
			while(list.next()) {
				try {
					UUID uuid = UUID.fromString(list.getString("identifier"));
					long creation = list.getLong("creation");
					String source = list.getString("source");
					Optional<Text> pardon_reason = Optional.ofNullable(EChat.of(list.getString("pardon_reason")));
					Optional<String> pardon_source = Optional.ofNullable(list.getString("pardon_source"));
					
					Optional<Long> expiration = Optional.of(list.getLong("expiration"));
					if(list.wasNull()) {
						expiration = Optional.empty();
					}
					Optional<Long> pardon_date = Optional.of(list.getLong("pardon_date"));
					if(list.wasNull()) {
						pardon_date = Optional.empty();
					}
					Optional<String> context = Optional.ofNullable(list.getString("context"));
					
					Optional<SanctionAuto.Type> type = SanctionAuto.Type.get(list.getString("type"));
					Optional<SanctionAuto.Reason> reason = this.plugin.getSanctionService().getReason(list.getString("reason"));
					if (type.isPresent() && reason.isPresent()) {
						int level_type = Optional.ofNullable(levels.get(type.get())).orElse(0) + 1;
						if(pardon_date != null) {
							levels.put(type.get(), level_type);
						}
						profiles.add(new EAuto(uuid, creation, expiration, reason.get(), type.get(), level_type, source, context, pardon_date, pardon_reason, pardon_source));
					}
				} catch (IllegalArgumentException e) {}
			}
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn("Error during a change of manual_ip : (identifier='" + this.getIdentifier() + "'): " + e.getMessage());
		} finally {
			try {if (preparedStatement != null) preparedStatement.close();} catch (SQLException e) {}
	    }
		return profiles;
	}
	
	private void sqlAdd(final SanctionManualIP ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
    	try {    		
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "INSERT INTO `" + this.plugin.getDataBase().getTableManualProfile() + "` "
    						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			preparedStatement.setTimestamp(2, new Timestamp(ban.getCreationDate()));
			preparedStatement.setLong(3, ban.getExpirationDate().orElse(null));
			preparedStatement.setString(4, EChat.serialize(ban.getReason()));
			preparedStatement.setString(5, ban.getSource());
			
			if(ban.isPardon()) {
				preparedStatement.setTimestamp(6, new Timestamp(ban.getPardonDate().get()));
				preparedStatement.setString(7, EChat.serialize(ban.getPardonReason().get()));
				preparedStatement.setString(8, ban.getPardonSource().get());
			} else {
				preparedStatement.setTimestamp(6, null);
				preparedStatement.setString(7, null);
				preparedStatement.setString(8, null);
			}
			preparedStatement.execute();
			this.plugin.getLogger().debug("Adding to the database : (identifier ='" + this.getIdentifier() + "';"
					 											  + "creation='" + ban.getCreationDate() + "';"
					 											  + "expiration='" + ban.getExpirationDate().orElse(-1L) + "';"
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
	
	private void sqlUpdate(final SanctionManualIP ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
    	try {    		
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "UPDATE `" + this.plugin.getDataBase().getTableManualProfile() + "` "
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
			preparedStatement.setString(4, this.getIdentifier());
			preparedStatement.setTimestamp(5, new Timestamp(ban.getCreationDate()));
			preparedStatement.execute();
			this.plugin.getLogger().debug("Updating to the database : (identifier='" + this.getIdentifier() + "';"
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
	
	private void sqlRemove(final SanctionManualIP ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.plugin.getDataBase().getTableManualIp() + "` "
		    				+ "WHERE `identifier` = ? AND `creation` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			preparedStatement.setTimestamp(2, new Timestamp(ban.getCreationDate()));
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (identifier='" + this.getIdentifier() + "';"
					 											  + "creation='" + ban.getCreationDate() + "';"
					 											  + "expiration='" + ban.getExpirationDate().orElse(-1L) + "';"
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
	
	private void sqlClear() {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.plugin.getDataBase().getTableManualProfile() + "` "
		    				+ "WHERE `identifier` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (identifier='" + this.getIdentifier() + "';");
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
