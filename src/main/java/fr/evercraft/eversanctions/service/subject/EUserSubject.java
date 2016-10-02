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
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import fr.evercraft.everapi.event.ESpongeEventFactory;
import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.server.user.EUser;
import fr.evercraft.everapi.services.sanction.Jail;
import fr.evercraft.everapi.services.sanction.SanctionUserSubject;
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto.Reason;
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
		this.manual.clear();
		this.auto.clear();
		
		this.manual.addAll(this.sqlSelectManual(connection));
		this.auto.addAll(this.sqlSelectAuto(connection));
	}
	
	public void update() {
		this.ban = false;
		this.mute = false;
		this.jail = false;
		
		this.manual.stream().filter(manual -> !manual.isExpire())
			.forEach(manual -> {
				if(manual.getType().equals(SanctionManualProfile.Type.BAN_PROFILE)) {
					this.ban = true;
				} else if(manual.getType().equals(SanctionManualProfile.Type.MUTE)) {
					this.mute = true;
				} else if(manual.getType().equals(SanctionManualProfile.Type.JAIL)) {
					this.jail = true;
				}
			});
		
		this.auto.stream().filter(auto -> !auto.isExpire())
			.forEach(auto -> {
				this.ban = this.ban || auto.isBan();
				this.mute = this.mute || auto.isMute();
				this.jail = this.jail || auto.isJail();
			});
	}
	
	public Optional<EManualProfile> get(final SanctionManualProfile.Type type) {
		this.plugin.getLogger().warn("size : " + this.manual.size());
		this.plugin.getLogger().warn("size type : " + this.manual.stream().filter(manual -> manual.getType().equals(type)).count());
		this.plugin.getLogger().warn("size unexpire: " + this.manual.stream().filter(manual -> manual.getType().equals(type) && !manual.isExpire()).count());
		return this.manual.stream().filter(manual -> manual.getType().equals(type) && !manual.isExpire()).findFirst();
	}
	
	/**
	 * Retourne le ban manuel
	 * @return
	 */
	public Optional<EManualProfileBan> getManualBan() {
		Optional<EManualProfile> ban = this.manual.stream()
													.filter(manual -> manual.getType().equals(SanctionManualProfile.Type.BAN_PROFILE) && !manual.isExpire())
													.findFirst();
		if (ban.isPresent() && ban.get() instanceof EManualProfileBan) {
			return Optional.of((EManualProfileBan) ban.get()); 
		}
		return Optional.empty();
	}
	
	/**
	 * Retourne les ban-ip manuel
	 * @return
	 */
	public Optional<EManualProfileBanIp> getManualBanIp(InetAddress address) {
		Optional<EManualProfile> ban = this.manual.stream().filter(manual ->
			manual.getType().equals(SanctionManualProfile.Type.BAN_IP) && UtilsNetwork.equals(((EManualProfileBanIp) manual).getAddress(), address) && !manual.isExpire()
		).findFirst();
		
		if (ban.isPresent()) {
			return Optional.of((EManualProfileBanIp) ban.get());
		}
		return Optional.empty();
	}
	
	public Collection<EManualProfileBanIp> getManualBanIp() {
		Builder<EManualProfileBanIp> bans = ImmutableList.builder();
		for(EManualProfile manual : this.manual) {
			if(manual.getType().equals(SanctionManualProfile.Type.BAN_IP)) {
				if(!manual.isExpire()) {
					bans.add((EManualProfileBanIp) manual);
				}
			}
		}
		return bans.build();
	}
	
	/**
	 * Retourne le mute manuel
	 * @return
	 */
	public Optional<EManualProfileMute> getManualMute() {
		Optional<EManualProfile> ban = this.manual.stream()
													.filter(manual -> manual.getType().equals(SanctionManualProfile.Type.MUTE) && !manual.isExpire())
													.findFirst();
		if (ban.isPresent() && ban.get() instanceof EManualProfileBan) {
			return Optional.of((EManualProfileMute) ban.get()); 
		}
		return Optional.empty();
	}
	
	/**
	 * Retourne le jail manuel
	 * @return
	 */
	public Optional<EManualProfileJail> getManualJail() {
		Optional<EManualProfile> ban = this.manual.stream()
													.filter(manual -> manual.getType().equals(SanctionManualProfile.Type.JAIL) && !manual.isExpire())
													.findFirst();
		if (ban.isPresent() && ban.get() instanceof EManualProfileBan) {
			return Optional.of((EManualProfileJail) ban.get()); 
		}
		return Optional.empty();
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
		return this.manual.stream()
				.filter(manual -> manual.getType().equals(SanctionManualProfile.Type.BAN_IP) && !manual.isExpire())
				.findFirst()
				.isPresent();
	}
	
	@Override
	public boolean isBanIp(InetAddress address) {
		Optional<EManualProfile> ban_manual = this.manual.stream()
			.filter(manual -> manual.getType().equals(SanctionManualProfile.Type.BAN_IP) && UtilsNetwork.equals(((SanctionManualProfile.BanIp) manual).getAddress(), address))
			.findFirst();
		if(ban_manual.isPresent()) {
			return true;
		}
		
		Optional<EAuto> ban_auto = this.auto.stream()
				.filter(auto -> auto.getAddress().isPresent() && UtilsNetwork.equals(auto.getAddress().get(), address))
				.findFirst();
		return ban_auto.isPresent();
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
			
			this.sqlRemoveManual(connection);
			this.sqlRemoveAuto(connection);
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
	public boolean ban(long creation, Optional<Long> expiration, Text reason, final CommandSource source) {
		Preconditions.checkNotNull(expiration, "expiration");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		// Le joueur a déjà une saction
		if (this.getManualBan().isPresent()) {
			return false;
		}
		
		final Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(this.getUniqueId());
		// Joueur introuvable
		if (!user.isPresent()) {
			return false;
		}
		
		final EManualProfileBan manual = new EManualProfileBan(this.getUniqueId(), creation, expiration, reason, source.getIdentifier());
		final Ban.Profile ban = manual.getBan(user.get().getProfile());
		
		// Event cancel
		if(Sponge.getEventManager().post(SpongeEventFactory.createBanUserEvent(Cause.source(this).build(), ban, user.get()))) {
			return false;
		}
		
		this.plugin.getSanctionService().add(ban);
		this.manual.add(manual);
		this.plugin.getThreadAsync().execute(() -> this.sqlAddManual(manual));
		return true;
	}
	
	@Override
	public boolean banIp(InetAddress address, long creation, Optional<Long> expiration, Text reason, final CommandSource source) {
		Preconditions.checkNotNull(address, "address");
		Preconditions.checkNotNull(expiration, "expiration");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		// Le joueur a déjà une saction
		if (!this.getManualBanIp(address).isPresent()) {
			return false;
		}
		
		// User inconnu
		final Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(this.getUniqueId());
		if (!user.isPresent()) {
			return false;
		}
		
		// SubjectIP inconnu
		final Optional<EIpSubject> subject_ip = this.plugin.getSanctionService().getSubject(address);
		if (!subject_ip.isPresent()) {
			return false;
		}
		
		final EManualProfileBanIp ban = new EManualProfileBanIp(this.getUniqueId(), address, creation, expiration, reason, source.getIdentifier());
		
		// Event cancel
		if (Sponge.getEventManager().post(SpongeEventFactory.createBanIpEvent(Cause.source(this).build(), ban.getBan()))) {
			return false;
		}
		
		this.manual.add(ban);
		subject_ip.get().add(ban);
		this.plugin.getThreadAsync().execute(() -> this.sqlAddManual(ban));
		return true;
	}
	
	@Override
	public boolean mute(long creation, final Optional<Long> expiration, final Text reason, final CommandSource source) {
		Preconditions.checkNotNull(expiration, "expiration");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		// Le joueur a déjà une saction
		if (this.getManualMute().isPresent()) {
			return false;
		}
		
		// User inconnu
		final Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(this.getUniqueId());
		if (!user.isPresent()) {
			return false;
		}
		
		final EManualProfileMute mute = new EManualProfileMute(this.getUniqueId(), creation, expiration, reason, source.getIdentifier());
		
		// Event cancel
		if(Sponge.getEventManager().post(ESpongeEventFactory.createMuteEventEnable(user.get(), reason, creation, expiration, source, Cause.source(this).build()))) {
			return false;
		}
		
		this.manual.add(mute);
		this.plugin.getThreadAsync().execute(() -> this.sqlAddManual(mute));
		return false;
	}
	
	@Override
	public boolean jail(Jail jail, long creation, Optional<Long> expiration, Text reason, final CommandSource source) {
		Preconditions.checkNotNull(jail, "jail");
		Preconditions.checkNotNull(expiration, "expiration");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		// Le joueur a déjà une saction
		if (this.getManualJail().isPresent()) {
			return false;
		}
		
		// User inconnu
		final Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(this.getUniqueId());
		if (!user.isPresent()) {
			return false;
		}
		
		final EManualProfileMute mute = new EManualProfileMute(this.getUniqueId(), creation, expiration, reason, source.getIdentifier());
		
		// Event cancel
		if(Sponge.getEventManager().post(ESpongeEventFactory.createJailEventEnable(user.get(), jail, reason, creation, expiration, source, Cause.source(this).build()))) {
			return false;
		}
		
		this.manual.add(mute);
		this.plugin.getThreadAsync().execute(() -> this.sqlAddManual(mute));
		return false;
	}
	
	@Override
	public Optional<SanctionManualProfile.Ban> pardonBan(long date, Text reason, CommandSource source) {
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		Optional<EManualProfileBan> manual = this.getManualBan();
		
		// Aucun manual
		if(!manual.isPresent()) {
			return Optional.empty();
		}
		
		final Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(this.getUniqueId());
		if (!user.isPresent()) {
			return Optional.empty();
		}
		
		manual.get().pardon(date, reason, source.getIdentifier());
		final Ban.Profile ban = manual.get().getBan(user.get().getProfile());
		
		this.plugin.getLogger().warn("pardon");
		
		this.plugin.getSanctionService().remove(ban);
		this.plugin.getThreadAsync().execute(() -> this.sqlPardonManual(manual.get()));
		this.plugin.getLogger().warn("return");
		return Optional.of(manual.get());
	}
	
	@Override
	public Optional<SanctionManualProfile.BanIp> pardonBanIp(InetAddress address, long date, Text reason, CommandSource source) {
		Preconditions.checkNotNull(address, "address");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		Optional<EManualProfileBanIp> optManual = this.getManualBanIp(address);
		if(optManual.isPresent()) {
			return Optional.empty();
		}
		EManualProfileBanIp manual = optManual.get();
		
		
		final Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(this.getUniqueId());
		if (!user.isPresent()) {
			return Optional.empty();
		}
		
		
		manual.pardon(date, reason, source.getIdentifier());
		final Ban.Ip ban = manual.getBan();
		
		this.plugin.getSanctionService().remove(ban);
		this.plugin.getThreadAsync().execute(() -> this.sqlPardonManual(manual));
		return Optional.of(manual);
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Collection<SanctionManualProfile.BanIp> pardonBanIp(long date, Text reason, CommandSource source) {
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		Collection<EManualProfileBanIp> manuals = this.getManualBanIp();
		if(manuals.isEmpty()) {
			return ImmutableList.of();
		}
		
		final Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(this.getUniqueId());
		if (!user.isPresent()) {
			return ImmutableList.of();
		}
		
		
		for (EManualProfileBanIp manual : manuals) {
			manual.pardon(date, reason, source.getIdentifier());
			final Ban.Ip ban = manual.getBan();
			
			this.plugin.getSanctionService().remove(ban);
			this.plugin.getThreadAsync().execute(() -> this.sqlPardonManual(manual));
		}
		return (Collection) manuals;
	}
	
	
	@Override
	public Optional<SanctionManualProfile.Mute> pardonMute(long date, Text reason, CommandSource source) {
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		Optional<EManualProfileMute> manual = this.getManualMute();
		
		// Aucun manual
		if(!manual.isPresent()) {
			return Optional.empty();
		}
		
		final Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(this.getUniqueId());
		if (!user.isPresent()) {
			return Optional.empty();
		}
		
		manual.get().pardon(date, reason, source.getIdentifier());
		this.plugin.getThreadAsync().execute(() -> this.sqlPardonManual(manual.get()));
		return Optional.of(manual.get());
	}
	
	@Override
	public Optional<SanctionManualProfile.Jail> pardonJail(long date, Text reason, CommandSource source) {
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		Optional<EManualProfileJail> manual = this.getManualJail();
		
		// Aucun manual
		if(!manual.isPresent()) {
			return Optional.empty();
		}
		
		final Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(this.getUniqueId());
		if (!user.isPresent()) {
			return Optional.empty();
		}
		
		manual.get().pardon(date, reason, source.getIdentifier());
		this.plugin.getThreadAsync().execute(() -> this.sqlPardonManual(manual.get()));
		return Optional.of(manual.get());
	}
	
	/*
	 * Auto
	 */
	
	public boolean pardon(SanctionAuto.Reason type, long date, Text reason, CommandSource source) {
		Preconditions.checkNotNull(type, "type");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		Optional<EAuto> auto = this.get(type);
		// Aucun auto
		if(!auto.isPresent()) {
			return false;
		}
		
		auto.get().pardon(date, reason, source.getIdentifier());
		this.plugin.getThreadAsync().execute(() -> this.sqlPardonAuto(auto.get()));
		return true;
	}
	
	@Override
	public boolean addSanction(SanctionAuto.Reason reason, long creation, CommandSource source) {
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
		if(level.get().getType().isBanIP() && user.get().getLastIP().isPresent()) {
			option = Optional.ofNullable(UtilsNetwork.getHostString(user.get().getLastIP().get()));
		} else if(level.get().getType().isMute()) {
			option = level.get().getOption();
		}
		
		EAuto auto = new EAuto(this.getUniqueId(), creation, level.get().getExpirationDate(creation), reason, level.get().getType(), level_int, source.getIdentifier(), option);
		
		if(auto.isBan()) {
			if(Sponge.getEventManager().post(SpongeEventFactory.createBanUserEvent(Cause.source(this).build(), auto.getBan(user.get().getProfile()).get(), user.get()))) {
				return false;
			}
		}
		
		if(auto.isBanIP()) {
			// IP inconnu
			if(!user.get().getLastIP().isPresent()) {
				return false;
			}
			
			// SubjectIP inconnu
			final Optional<EIpSubject> subject_ip = this.plugin.getSanctionService().getSubject(user.get().getLastIP().get());
			if (!subject_ip.isPresent()) {
				return false;
			}
			
			if(Sponge.getEventManager().post(SpongeEventFactory.createBanIpEvent(Cause.source(this).build(), auto.getBan(user.get().getProfile(), user.get().getLastIP().get()).get()))) {
				return false;
			}
			
			subject_ip.get().add(auto);
		}
		
		this.auto.add(auto);
		this.plugin.getThreadAsync().execute(() -> this.sqlAddAuto(auto));
		return true;
	}
	
	@Override
	public boolean pardonSanction(Reason reason, long creation, CommandSource source) {
		// TODO Auto-generated method stub
		return false;
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
			this.plugin.getThreadAsync().execute(() -> this.sqlRemoveManual(profile));
			return true;
		}
		return false;
	}

	@Override
	public boolean removeAuto(SanctionAuto profile) {
		if (this.auto.remove(profile)) {
			this.plugin.getThreadAsync().execute(() -> this.sqlRemoveAuto(profile));
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
	
	private Collection<EManualProfile> sqlSelectManual(final Connection connection) {
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
				
				Optional<SanctionManualProfile.Type> type = SanctionManualProfile.Type.get(list.getString("type"));
				if (type.isPresent()) {
					if(type.get().equals(SanctionManualProfile.Type.BAN_PROFILE)) {
						profiles.add(new EManualProfileBan(this.getUniqueId(), creation, expiration, reason, source, pardon_date, pardon_reason, pardon_source));
					} else if (type.get().equals(SanctionManualProfile.Type.BAN_IP)) {
						Optional<InetAddress> address = UtilsNetwork.getHost(list.getString("option"));
						if (address.isPresent()) {
							profiles.add(new EManualProfileBanIp(this.getUniqueId(), address.get(), creation, expiration, reason, source, pardon_date, pardon_reason, pardon_source));
						}
					} else if (type.get().equals(SanctionManualProfile.Type.MUTE)) {
						profiles.add(new EManualProfileMute(this.getUniqueId(), creation, expiration, reason, source, pardon_date, pardon_reason, pardon_source));
					} else if (type.get().equals(SanctionManualProfile.Type.JAIL)) {
						Optional<String> jail = Optional.ofNullable(list.getString("option"));
						if (jail.isPresent()) {
							profiles.add(new EManualProfileJail(this.getUniqueId(), jail.get(), creation, expiration, reason, source, pardon_date, pardon_reason, pardon_source));
						}
					}
				}
			}
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn("Error during a change of manual_ip : (identifier='" + this.getIdentifier() + "'): " + e.getMessage());
		} finally {
			try {if (preparedStatement != null) preparedStatement.close();} catch (SQLException e) {}
	    }
		return profiles;
	}
	
	private void sqlAddManual(final EManualProfile ban) {
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
	
	private void sqlPardonManual(final EManualProfile ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
    	try {    		
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "UPDATE `" + this.plugin.getDataBase().getTableManualProfile() + "` "
    						+ "SET `pardon_date` = ? ,"
    							+ " `pardon_reason` = ? ,"
    							+ " `pardon_source` = ? "
    						+ "WHERE `identifier` = ? AND creation = ? ;";
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
			this.plugin.getLogger().debug("Updating to the database : (identifier ='" + this.getIdentifier() + "';"
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
	
	private void sqlRemoveManual(final SanctionManualProfile ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.plugin.getDataBase().getTableManualProfile() + "` "
		    				+ "WHERE `identifier` = ? AND `creation` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			preparedStatement.setDouble(2, ban.getCreationDate());
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (identifier ='" + this.getIdentifier() + "';"
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
	
	private void sqlRemoveManual(final Connection connection) {
		PreparedStatement preparedStatement = null;
    	try {
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.plugin.getDataBase().getTableManualProfile() + "` "
		    				+ "WHERE `identifier` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (identifier ='" + this.getIdentifier() + "';");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of manual : " + e.getMessage());
		} finally {
			try {if (preparedStatement != null) preparedStatement.close();} catch (SQLException e) {}
	    }
	}
		
	/*
	 * Auto
	 */
	
	private Collection<EAuto> sqlSelectAuto(final Connection connection) {
		Collection<EAuto> profiles = new ArrayList<EAuto>();
		PreparedStatement preparedStatement = null;
		try {
			String query = "SELECT * "
						+ "FROM `" + this.plugin.getDataBase().getTableAuto() + "` "
						+ "WHERE `identifier` = ? "
						+ "ORDER BY `creation` ASC;";
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
					profiles.add(new EAuto(this.getUniqueId(), creation, expiration, reason.get(), type.get(), level_type, source, option, pardon_date, pardon_reason, pardon_source));
				}
			}
		} catch (SQLException e) {
	    	this.plugin.getLogger().warn("Error during a change of manual_ip : (identifier='" + this.getIdentifier() + "'): " + e.getMessage());
		} finally {
			try {if (preparedStatement != null) preparedStatement.close();} catch (SQLException e) {}
	    }
		return profiles;
	}
	
	private void sqlAddAuto(final EAuto ban) {
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
			this.plugin.getLogger().debug("Adding to the database : (identifier ='" + this.getIdentifier() + "';"
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
	
	private void sqlPardonAuto(final EAuto ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		
    	try {    		
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "UPDATE `" + this.plugin.getDataBase().getTableAuto() + "` "
    						+ "SET `pardon_date` = ? ,"
    							+ "`pardon_reason` = ? ,"
    							+ "`pardon_source` = ? "
    						+ "WHERE `identifier` = ? AND `creation` = ? ;";
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
			this.plugin.getLogger().debug("Updating to the database : (identifier ='" + this.getIdentifier() + "';"
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
	
	private void sqlRemoveAuto(final SanctionAuto ban) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
    	try {
    		connection = this.plugin.getDataBase().getConnection();
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.plugin.getDataBase().getTableAuto() + "` "
		    				+ "WHERE `identifier` = ? AND `creation` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			preparedStatement.setDouble(2, ban.getCreationDate());
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (identifier ='" + this.getIdentifier() + "';"
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
	
	private void sqlRemoveAuto(final Connection connection) {
    	PreparedStatement preparedStatement = null;
    	try {
    		String query = 	  "DELETE " 
		    				+ "FROM `" + this.plugin.getDataBase().getTableAuto() + "` "
		    				+ "WHERE `identifier` = ? ;";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, this.getIdentifier());
			
			preparedStatement.execute();
			this.plugin.getLogger().debug("Remove from database : (identifier ='" + this.getIdentifier() + "';");
    	} catch (SQLException e) {
        	this.plugin.getLogger().warn("Error during a change of auto : " + e.getMessage());
		} finally {
			try {if (preparedStatement != null) preparedStatement.close();} catch (SQLException e) {}
	    }
	}
}
