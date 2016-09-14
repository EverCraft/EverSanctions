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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban;

import com.google.common.base.Preconditions;

import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.server.user.EUser;
import fr.evercraft.everapi.services.sanction.Jail;
import fr.evercraft.everapi.services.sanction.SanctionUserSubject;
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;
import fr.evercraft.everapi.services.sanction.manual.SanctionManualProfile;
import fr.evercraft.everapi.sponge.UtilsNetwork;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.auto.EAuto;
import fr.evercraft.eversanctions.service.manual.EManualProfile;
import fr.evercraft.eversanctions.service.manual.EManualProfileBan;
import fr.evercraft.eversanctions.service.manual.EManualProfileBanIp;
import fr.evercraft.eversanctions.service.manual.EManualProfileJail;
import fr.evercraft.eversanctions.service.manual.EManualProfileMute;

public class EUserSubject implements SanctionUserSubject {
	
	private final EverSanctions plugin;
	private final UUID uuid;
	
	private final ConcurrentSkipListSet<EManualProfile> manual;
	private final ConcurrentSkipListSet<EAuto> auto;
	
	private boolean ban;
	private boolean ip;
	private boolean mute;
	private boolean jail;

	public EUserSubject(final EverSanctions plugin, final UUID uuid) {
		this.plugin = plugin;
		this.uuid = uuid;
		
		this.manual = new ConcurrentSkipListSet<EManualProfile>((EManualProfile o1, EManualProfile o2) -> o2.getCreationDate().compareTo(o1.getCreationDate()));
		this.auto = new ConcurrentSkipListSet<EAuto>((EAuto o1, EAuto o2) -> o2.getCreationDate().compareTo(o1.getCreationDate()));
		
		this.reload();
	}
	
	public void reload() {
		
	}
	
	public void reload(Connection connection) {
		this.manual.clear();
		this.auto.clear();
		
		this.manual.addAll(this.requeteSelectManual(connection));
		this.auto.addAll(this.requeteSelectAuto(connection));
	}
	
	public void update() {
		this.ban = false;
		this.ip = false;
		this.mute = false;
		this.jail = false;
		
		this.manual.stream().filter(manual -> !manual.isExpire())
			.forEach(manual -> {
				if(manual.getType().equals(SanctionManualProfile.Type.BAN_PROFILE)) {
					this.ban = true;
				} else if(manual.getType().equals(SanctionManualProfile.Type.BAN_IP)) {
					this.ip = true;
				} else if(manual.getType().equals(SanctionManualProfile.Type.MUTE)) {
					this.mute = true;
				} else if(manual.getType().equals(SanctionManualProfile.Type.JAIL)) {
					this.jail = true;
				}
			});
		
		this.auto.stream().filter(auto -> !auto.isExpire())
			.forEach(auto -> {
				this.ban = this.ban || auto.isBan();
				this.ip = this.ip || auto.isBanIP();
				this.mute = this.mute || auto.isMute();
				this.jail = this.jail || auto.isJail();
			});
	}
	
	public Optional<EManualProfile> get(final SanctionManualProfile.Type type) {
		return this.manual.stream().filter(manual -> manual.getType().equals(type)).findFirst();
	}
	
	public Stream<EManualProfile> getManual() {
		return this.manual.stream().filter(manual -> !manual.isExpire());
	}
	
	public Optional<EAuto> get(final SanctionAuto.Reason reason) {
		return this.auto.stream().filter(auto -> auto.getReason().equals(reason) && !auto.isExpire()).findFirst();
	}

	public Stream<EAuto> getReasons() {
		return this.auto.stream().filter(auto -> !auto.isExpire());
	}
	
	@Override
	public boolean isBan() {
		return this.ban;
	}
	
	@Override
	public boolean isBanIp() {
		return this.ip;
	}


	@Override
	public boolean isMute() {
		return this.mute;
	}

	@Override
	public boolean isJail() {
		return this.jail;
	}
	
	/*
	 * DataBase
	 */
	
	private void requeteRemove() {
		Connection connection = null;
		try {
			connection = this.plugin.getDataBase().getConnection();
			
			this.requeteRemoveManual(connection);
			this.requeteRemoveAuto(connection);
		} catch (ServerDisableException e) {
			e.execute();
		} finally {
			try {if (connection != null) connection.close();} catch (SQLException e) {}
	    }
	}
	
	/*
	 * Manual
	 */
	
	@Override
	public boolean ban(long creation, Optional<Long> expiration, Text reason, final String source) {
		Preconditions.checkNotNull(expiration, "expiration");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		if (this.get(SanctionManualProfile.Type.BAN_PROFILE).isPresent()) {
			return false;
		}
		
		final Optional<EUser> user = this.plugin.getEServer().getEUser(this.getUniqueId());
		if (!user.isPresent()) {
			return false;
		}
		
		final EManualProfileBan manual = new EManualProfileBan(creation, expiration, reason, source);
		final Ban.Profile ban = manual.getBan(user.get().getProfile());
		if(Sponge.getEventManager().post(SpongeEventFactory.createBanUserEvent(Cause.source(this).build(), ban, user.get()))) {
			return false;
		}
		
		this.plugin.getSanctionService().add(ban);
		this.manual.add(manual);
		this.plugin.getThreadAsync().execute(() -> this.requeteAddManual(manual));
		return true;
	}
	
	@Override
	public boolean banIp(InetAddress address, long creation, Optional<Long> expiration, Text reason, final String source) {
		Preconditions.checkNotNull(address, "address");
		Preconditions.checkNotNull(expiration, "expiration");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		if(!this.get(SanctionManualProfile.Type.BAN_IP).isPresent()) {
			final EManualProfileBanIp ban = new EManualProfileBanIp(address, creation, expiration, reason, source);
			Optional<EUser> user = this.plugin.getEServer().getEUser(this.getUniqueId());
			if(user.isPresent() && !Sponge.getEventManager().post(SpongeEventFactory.createBanIpEvent(Cause.source(this).build(), ban.getBan(address)))) {
				this.manual.add(ban);
				this.plugin.getThreadAsync().execute(() -> this.requeteAddManual(ban));
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean mute(long creation, Optional<Long> expiration, Text reason, final String source) {
		Preconditions.checkNotNull(expiration, "expiration");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		if(!this.get(SanctionManualProfile.Type.MUTE).isPresent()) {
			final EManualProfileMute ban = new EManualProfileMute(creation, expiration, reason, source);
			this.manual.add(ban);
			this.plugin.getThreadAsync().execute(() -> this.requeteAddManual(ban));
			return true;
		}
		return false;
	}
	
	@Override
	public boolean jail(Jail jail, long creation, Optional<Long> expiration, Text reason, final String source) {
		Preconditions.checkNotNull(jail, "jail");
		Preconditions.checkNotNull(expiration, "expiration");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		if(!this.get(SanctionManualProfile.Type.JAIL).isPresent()) {
			final EManualProfileJail ban = new EManualProfileJail(jail.getName(), creation, expiration, reason, source);
			this.manual.add(ban);
			this.plugin.getThreadAsync().execute(() -> this.requeteAddManual(ban));
			return true;
		}
		return false;
	}
	
	@Override
	public Optional<SanctionManualProfile> pardon(SanctionManualProfile.Type type, long date, Text reason, String source) {
		Preconditions.checkNotNull(type, "type");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		Optional<EManualProfile> manual = this.get(type);
		this.plugin.getEServer().broadcast("size : " + this.manual.size());
		this.plugin.getEServer().broadcast("manual : " + manual);
		// Aucun manual
		if(!manual.isPresent()) {
			return Optional.empty();
		}
		
		manual.get().pardon(date, reason, source);
		this.plugin.getThreadAsync().execute(() -> this.requetePardonManual(manual.get()));
		return Optional.of(manual.get());
	}
	
	/*
	 * Auto
	 */
	
	public boolean pardon(SanctionAuto.Reason type, long date, Text reason, String source) {
		Preconditions.checkNotNull(type, "type");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		Optional<EAuto> auto = this.get(type);
		// Aucun auto
		if(!auto.isPresent()) {
			return false;
		}
		
		auto.get().pardon(date, reason, source);
		this.plugin.getThreadAsync().execute(() -> this.requetePardonAuto(auto.get()));
		return true;
	}
	
	@Override
	public boolean addSanction(SanctionAuto.Reason reason, long creation, String source) {
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		Optional<EUser> user = this.plugin.getEServer().getEUser(this.getUniqueId());
		// User introuvable
		if (!user.isPresent()) {
			return false;
		}
		
		int level_int = this.getLevel(reason);
		Optional<SanctionAuto.Level> level = reason.getLevel(level_int);
		// Level introuvable
		if (!level.isPresent()) {
			return false;
		}
		
		
		Optional<String> option = Optional.empty();
		if(level.get().getType().isBanIP() && user.get().getLastIp().isPresent()) {
			option = Optional.ofNullable(UtilsNetwork.getHostString(user.get().getLastIp().get()));
		} else if(level.get().getType().isMute()) {
			option = level.get().getOption();
		}
		
		EAuto auto = new EAuto(creation, level.get().getExpirationDate(creation), reason, level.get().getType(), level_int, source, option);
		
		if(auto.isBan()) {
			if(Sponge.getEventManager().post(SpongeEventFactory.createBanUserEvent(Cause.source(this).build(), auto.getBan(user.get().getProfile()).get(), user.get()))) {
				return false;
			}
		}
		
		if(auto.isBanIP()) {
			if(!user.get().getLastIp().isPresent() || 
				Sponge.getEventManager().post(SpongeEventFactory.createBanIpEvent(Cause.source(this).build(), auto.getBan(user.get().getProfile(), user.get().getLastIp().get()).get()))) {
				return false;
			}
		}
		
		this.auto.add(auto);
		this.plugin.getThreadAsync().execute(() -> this.requeteAddAuto(auto));
		return true;
	}
	
	public int getLevel(final SanctionAuto.Reason reason) {
		return Math.toIntExact(this.auto.stream().filter(auto -> auto.getReason().equals(reason) && !auto.isPardon()).count()) + 1;
	}

	@Override
	public Optional<SanctionManualProfile> getManual(SanctionManualProfile.Type type) {
		return Optional.ofNullable(this.get(type).orElse(null));
	}

	@Override
	public Collection<SanctionAuto> getAuto(SanctionAuto.Type type) {
		return this.auto.stream().filter(auto -> auto.getType().equals(type) && !auto.isExpire()).collect(Collectors.toList());
	}

	@Override
	public boolean removeManual(SanctionManualProfile profile) {
		if (this.manual.remove(profile)) {
			this.plugin.getThreadAsync().execute(() -> this.requeteRemoveManual(profile));
			return true;
		}
		return false;
	}

	@Override
	public boolean removeAuto(SanctionAuto profile) {
		if (this.auto.remove(profile)) {
			this.plugin.getThreadAsync().execute(() -> this.requeteRemoveAuto(profile));
			return true;
		}
		return false;
	}
	
	public boolean clear() {
		if (this.auto.isEmpty() || this.manual.isEmpty()) {
			this.plugin.getThreadAsync().execute(() -> this.requeteRemove());
			return true;
		}
		return false;
	}
	
	public String getIdentifier() {
		return this.uuid.toString();
	}
	
	public UUID getUniqueId() {
		return this.uuid;
	}
	
	/*
	 * Manual
	 */
	
	private Collection<EManualProfile> requeteSelectManual(final Connection connection) {
		Collection<EManualProfile> profiles = new ArrayList<EManualProfile>();
		PreparedStatement preparedStatement = null;
		try {
			String query = "SELECT * "
						+ "FROM `" + this.plugin.getDataBase().getTableManualProfile() + "` "
						+ "WHERE `identifier` = ? ;";
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
				
				Optional<SanctionManualProfile.Type> type = SanctionManualProfile.Type.get(list.getString("type"));
				if (type.isPresent()) {
					if(type.get().equals(SanctionManualProfile.Type.BAN_PROFILE)) {
						profiles.add(new EManualProfileBan(creation, expiration, reason, source, pardon_date, pardon_reason, pardon_source));
					} else if (type.get().equals(SanctionManualProfile.Type.BAN_IP)) {
						Optional<InetAddress> address = UtilsNetwork.getHost(list.getString("option"));
						if (address.isPresent()) {
							profiles.add(new EManualProfileBanIp(address.get(), creation, expiration, reason, source, pardon_date, pardon_reason, pardon_source));
						}
					} else if (type.get().equals(SanctionManualProfile.Type.MUTE)) {
						profiles.add(new EManualProfileMute(creation, expiration, reason, source, pardon_date, pardon_reason, pardon_source));
					} else if (type.get().equals(SanctionManualProfile.Type.JAIL)) {
						profiles.add(new EManualProfileJail(list.getString("option"), creation, expiration, reason, source, pardon_date, pardon_reason, pardon_source));
					}
				}
			}
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn("Error during a change of manual_ip : (uuid='" + this.getIdentifier() + "'): " + e.getMessage());
		} finally {
			try {if (preparedStatement != null) preparedStatement.close();} catch (SQLException e) {}
	    }
		return profiles;
	}
	
	private void requeteAddManual(final EManualProfile ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
		Optional<String> option = Optional.empty();
		if(ban.getType().equals(SanctionManualProfile.Type.JAIL)) {
			option = Optional.of(((EManualProfileJail) ban).getJailName());
		} else if(ban.getType().equals(SanctionManualProfile.Type.BAN_IP)) {
			option = Optional.of(((EManualProfileBanIp) ban).getAddress().getHostAddress());
		}
		
    	try {    		
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "INSERT INTO `" + this.plugin.getDataBase().getTableManualProfile() + "` "
    						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			preparedStatement.setDouble(2, ban.getCreationDate());
			if (ban.getExpirationDate().isPresent()) {
				preparedStatement.setDouble(3, ban.getExpirationDate().get());
			} else {
				preparedStatement.setNull(3, Types.DOUBLE);
			}
			preparedStatement.setString(4, ban.getType().name());
			preparedStatement.setString(5, EChat.serialize(ban.getReason()));
			preparedStatement.setString(6, ban.getSource());
			preparedStatement.setString(7, option.orElse(null));
			
			if(ban.isPardon()) {
				preparedStatement.setDouble(8, ban.getPardonDate().get());
				preparedStatement.setString(9, EChat.serialize(ban.getPardonReason().get()));
				preparedStatement.setString(10, ban.getPardonSource().get());
			} else {
				preparedStatement.setNull(8, Types.DOUBLE);
				preparedStatement.setString(9, null);
				preparedStatement.setString(10, null);
			}
			preparedStatement.execute();
			this.plugin.getLogger().debug("Adding to the database : (uuid ='" + this.getIdentifier() + "';"
					 											  + "creation='" + ban.getCreationDate() + "';"
					 											  + "expiration='" + ban.getExpirationDate().orElse(-1L) + "';"
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
	
	private void requetePardonManual(final EManualProfile ban) {
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
				preparedStatement.setDouble(1, ban.getPardonDate().get());
				preparedStatement.setString(2, EChat.serialize(ban.getPardonReason().get()));
				preparedStatement.setString(3, ban.getPardonSource().get());
			} else {
				preparedStatement.setNull(1, Types.DOUBLE);
				preparedStatement.setString(2, null);
				preparedStatement.setString(3, null);
			}
			preparedStatement.setString(4, this.getIdentifier());
			preparedStatement.setDouble(5, ban.getCreationDate());
			preparedStatement.execute();
			this.plugin.getLogger().debug("Updating to the database : (uuid ='" + this.getIdentifier() + "';"
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
	
	private void requeteRemoveManual(final SanctionManualProfile ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.plugin.getDataBase().getTableManualProfile() + "` "
		    				+ "WHERE `uuid` = ? AND `creation` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			preparedStatement.setDouble(2, ban.getCreationDate());
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (uuid ='" + this.getIdentifier() + "';"
					 											  + "creation='" + ban.getCreationDate() + "';"
					 											  + "expiration='" + ban.getExpirationDate().orElse(-1L) + "';"
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
	
	private void requeteRemoveManual(final Connection connection) {
		PreparedStatement preparedStatement = null;
    	try {
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.plugin.getDataBase().getTableManualProfile() + "` "
		    				+ "WHERE `uuid` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (uuid ='" + this.getIdentifier() + "';");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of manual : " + e.getMessage());
		} finally {
			try {if (preparedStatement != null) preparedStatement.close();} catch (SQLException e) {}
	    }
	}
		
	/*
	 * Auto
	 */
	
	private Collection<EAuto> requeteSelectAuto(final Connection connection) {
		Collection<EAuto> profiles = new ArrayList<EAuto>();
		PreparedStatement preparedStatement = null;
		try {
			String query = "SELECT * "
						+ "FROM `" + this.plugin.getDataBase().getTableAuto() + "` "
						+ "WHERE `identifier` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			ResultSet list = preparedStatement.executeQuery();
			
			Map<SanctionAuto.Type, Integer> levels = new HashMap<SanctionAuto.Type, Integer>();
			while(list.next()) {
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
				Optional<String> option = Optional.ofNullable(list.getString("option"));
				
				Optional<SanctionAuto.Type> type = SanctionAuto.Type.get(list.getString("type"));
				Optional<SanctionAuto.Reason> reason = this.plugin.getSanctionService().getReason(list.getString("reason"));
				if (type.isPresent() && reason.isPresent()) {
					int level_type = Optional.ofNullable(levels.get(type.get())).orElse(0) + 1;
					if(pardon_date != null) {
						levels.put(type.get(), level_type);
					}
					profiles.add(new EAuto(creation, expiration, reason.get(), type.get(), level_type, source, option, pardon_date, pardon_reason, pardon_source));
				}
			}
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn("Error during a change of manual_ip : (uuid='" + this.getIdentifier() + "'): " + e.getMessage());
		} finally {
			try {if (preparedStatement != null) preparedStatement.close();} catch (SQLException e) {}
	    }
		return profiles;
	}
	
	private void requeteAddAuto(final EAuto ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
    	try {    		
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "INSERT INTO `" + this.plugin.getDataBase().getTableAuto() + "` "
    						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			preparedStatement.setDouble(2, ban.getCreationDate());
			if (ban.getExpirationDate().isPresent()) {
				preparedStatement.setDouble(3, ban.getExpirationDate().get());
			} else {
				preparedStatement.setNull(3, Types.DOUBLE);
			}
			preparedStatement.setString(4, ban.getType().name());
			preparedStatement.setString(5, ban.getReason().getName());
			preparedStatement.setString(6, ban.getSource());
			preparedStatement.setString(7, ban.getOption().orElse(null));
			
			if(ban.isPardon()) {
				preparedStatement.setDouble(8, ban.getPardonDate().get());
				preparedStatement.setString(9, EChat.serialize(ban.getPardonReason().get()));
				preparedStatement.setString(10, ban.getPardonSource().get());
			} else {
				preparedStatement.setNull(8, Types.DOUBLE);
				preparedStatement.setString(9, null);
				preparedStatement.setString(10, null);
			}
			preparedStatement.execute();
			this.plugin.getLogger().debug("Adding to the database : (uuid ='" + this.getIdentifier() + "';"
					 											  + "creation='" + ban.getCreationDate() + "';"
					 											  + "expiration='" + ban.getExpirationDate().orElse(-1L) + "';"
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
	
	private void requetePardonAuto(final EAuto ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
    	try {    		
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "UPDATE `" + this.plugin.getDataBase().getTableAuto() + "` "
    						+ "SET pardon_date = ? ,"
    							+ "pardon_reason = ? ,"
    							+ "pardon_source = ? "
    						+ "WHERE uuid = ? AND creation = ? ;";
    		preparedStatement = connection.prepareStatement(query);

			if(ban.isPardon()) {
				preparedStatement.setDouble(1, ban.getPardonDate().get());
				preparedStatement.setString(2, EChat.serialize(ban.getPardonReason().get()));
				preparedStatement.setString(3, ban.getPardonSource().get());
			} else {
				preparedStatement.setNull(1, Types.DOUBLE);
				preparedStatement.setString(2, null);
				preparedStatement.setString(3, null);
			}
			preparedStatement.setString(4, this.getIdentifier());
			preparedStatement.setDouble(5, ban.getCreationDate());
			preparedStatement.execute();
			this.plugin.getLogger().debug("Updating to the database : (uuid ='" + this.getIdentifier() + "';"
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
	
	private void requeteRemoveAuto(final SanctionAuto ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.plugin.getDataBase().getTableAuto() + "` "
		    				+ "WHERE `uuid` = ? AND `creation` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			preparedStatement.setDouble(2, ban.getCreationDate());
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (uuid ='" + this.getIdentifier() + "';"
																  + "creation='" + ban.getCreationDate() + "';"
																  + "expiration='" + ban.getExpirationDate().orElse(-1L) + "';"
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
	
	private void requeteRemoveAuto(final Connection connection) {
    	PreparedStatement preparedStatement = null;
    	try {
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.plugin.getDataBase().getTableAuto() + "` "
		    				+ "WHERE `uuid` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (uuid ='" + this.getIdentifier() + "';");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of auto : " + e.getMessage());
		} finally {
			try {if (preparedStatement != null) preparedStatement.close();} catch (SQLException e) {}
	    }
	}
}
