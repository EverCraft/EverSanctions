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
import java.util.function.Predicate;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import fr.evercraft.everapi.exception.ServerDisableException;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.server.user.EUser;
import fr.evercraft.everapi.services.jail.Jail;
import fr.evercraft.everapi.services.sanction.Sanction;
import fr.evercraft.everapi.services.sanction.Sanction.SanctionBanIp;
import fr.evercraft.everapi.services.sanction.Sanction.SanctionBanProfile;
import fr.evercraft.everapi.services.sanction.Sanction.SanctionJail;
import fr.evercraft.everapi.services.sanction.Sanction.SanctionMute;
import fr.evercraft.everapi.services.sanction.SanctionUserSubject;
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;
import fr.evercraft.everapi.services.sanction.manual.SanctionManualProfile;
import fr.evercraft.everapi.sponge.UtilsNetwork;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.auto.EAuto;
import fr.evercraft.eversanctions.service.auto.EAutoBanIp;
import fr.evercraft.eversanctions.service.auto.EAutoBanProfile;
import fr.evercraft.eversanctions.service.auto.EAutoJail;
import fr.evercraft.eversanctions.service.auto.EAutoMute;
import fr.evercraft.eversanctions.service.auto.EAutoMuteAndJail;
import fr.evercraft.eversanctions.service.auto.EAutoBanProfileAndIp;
import fr.evercraft.eversanctions.service.manual.EManualProfile;
import fr.evercraft.eversanctions.service.manual.EManualProfileBan;
import fr.evercraft.eversanctions.service.manual.EManualProfileBanIp;
import fr.evercraft.eversanctions.service.manual.EManualProfileJail;
import fr.evercraft.eversanctions.service.manual.EManualProfileMute;

public class EUserSubject implements SanctionUserSubject {
	
	private final EverSanctions plugin;
	private final UUID uuid;
	
	private final ConcurrentSkipListSet<Sanction> sanctions;
	
	private Optional<SanctionBanProfile> ban;
	private Optional<SanctionMute> mute;
	private Optional<SanctionJail> jail;

	public EUserSubject(final EverSanctions plugin, final UUID uuid) {
		this.plugin = plugin;
		this.uuid = uuid;
		
		this.sanctions = new ConcurrentSkipListSet<Sanction>((Sanction o1, Sanction o2) -> o2.getCreationDate().compareTo(o1.getCreationDate()));
		this.jail = Optional.empty();
		
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
		this.sanctions.clear();
		
		this.sanctions.addAll(this.sqlSelectManual(connection));
		this.sanctions.addAll(this.sqlSelectAuto(connection));
		
		this.ban = this.findFirst(SanctionBanProfile.class);
		this.mute = this.findFirst(SanctionMute.class);
		this.jail = this.findFirst(SanctionJail.class, sanction -> sanction.getJail().isPresent());
	}

	/*
	 * Boolean
	 */
	
	@Override
	public boolean isBanIp() {
		return this.findFirst(SanctionBanIp.class).isPresent();
	}
	
	@Override
	public boolean isBanIp(InetAddress address) {
		return this.findFirst(SanctionBanIp.class, sanction -> UtilsNetwork.equals(sanction.getAddress(), address)).isPresent();
	}

	@Override
	public boolean isBanProfile() {
		if (this.ban.isPresent()) {
			if (this.ban.get().isExpire()) {
				
			}
			return this.ban.isPresent();
		}
		return false;
	}
	
	@Override
	public boolean isMute() {
		return this.mute.isPresent();
	}
	
	@Override
	public boolean isJail() {
		return this.getJail().isPresent();
	}
	
	/*
	 * Value
	 */
	
	public Optional<SanctionBanProfile> getBanProfile() {
		if (this.ban.isPresent()) {
			if (this.ban.get().isExpireDate()) {
				this.ban = this.findFirst(SanctionBanProfile.class);
			}
			return this.ban;
		}
		return Optional.empty();
	}
	
	public Optional<SanctionMute> getMute() {
		if (this.mute.isPresent()) {
			if (this.mute.get().isExpireDate()) {
				SanctionMute expire = this.mute.get();
				this.mute = this.findFirst(SanctionMute.class);
				this.plugin.getManagerEvents().postDisable(this.getUniqueId(), expire, Optional.empty());
				
				if (expire instanceof SanctionAuto.SanctionMuteAndJail) {
					this.getJail();
				}
			}
			return this.mute;
		}
		return Optional.empty();
	}
	
	public Optional<SanctionJail> getJail() {
		if (this.jail.isPresent()) {
			if (this.jail.get().isExpireDate()) {
				SanctionJail expire = this.jail.get();
				this.jail = this.findFirst(SanctionJail.class);
				this.plugin.getManagerEvents().postDisable(this.getUniqueId(), expire, Optional.empty());
				
				if (expire instanceof SanctionAuto.SanctionMuteAndJail) {
					this.getMute();
				}
			}
			return this.jail;
		}
		return Optional.empty();
	}
	
	@Override
	public Collection<Sanction> getAll() {
		return ImmutableList.copyOf(this.sanctions);
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
		if (this.findFirst(EManualProfileBan.class).isPresent()) {
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
		this.sanctions.add(manual);
		this.ban = this.findFirst(SanctionBanProfile.class);
		
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
		if (this.findFirst(EManualProfileBanIp.class, sanction -> UtilsNetwork.equals(sanction.getAddress(), address)).isPresent()) {
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
		
		final EManualProfileBanIp manual = new EManualProfileBanIp(this.getUniqueId(), address, creation, expiration, reason, source.getIdentifier());
		final Ban.Ip ban = manual.getBan();
		
		// Event cancel
		if (Sponge.getEventManager().post(SpongeEventFactory.createBanIpEvent(Cause.source(this).build(), ban))) {
			return false;
		}
		
		this.sanctions.add(manual);
		subject_ip.get().add(manual);
		this.plugin.getSanctionService().add(ban);
		this.plugin.getThreadAsync().execute(() -> this.sqlAddManual(manual));
		return true;
	}
	
	@Override
	public boolean mute(long creation, final Optional<Long> expiration, final Text reason, final CommandSource source) {
		Preconditions.checkNotNull(expiration, "expiration");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		// Le joueur a déjà une saction
		if (this.findFirst(EManualProfileMute.class).isPresent()) {
			return false;
		}
		
		// User inconnu
		final Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(this.getUniqueId());
		if (!user.isPresent()) {
			return false;
		}
		
		final EManualProfileMute manual = new EManualProfileMute(this.getUniqueId(), creation, expiration, reason, source.getIdentifier());
		
		// Event cancel
		if(this.plugin.getManagerEvents().postEnable(user.get(), manual, source)) {
			return false;
		}
		
		this.sanctions.add(manual);
		this.mute = this.findFirst(SanctionMute.class);
		
		if (!this.mute.isPresent()) {
			this.plugin.getEServer().broadcast("empty !!!");
		}
		
		this.plugin.getThreadAsync().execute(() -> this.sqlAddManual(manual));
		return true;
	}
	
	@Override
	public boolean jail(Jail jail, long creation, Optional<Long> expiration, Text reason, final CommandSource source) {
		Preconditions.checkNotNull(jail, "jail");
		Preconditions.checkNotNull(expiration, "expiration");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		// Le joueur a déjà une saction
		if (this.findFirst(EManualProfileJail.class).isPresent()) {
			return false;
		}
		
		// User inconnu
		final Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(this.getUniqueId());
		if (!user.isPresent()) {
			return false;
		}
		
		final EManualProfileJail manual = new EManualProfileJail(this.plugin, this.getUniqueId(), jail.getName(), creation, expiration, reason, source.getIdentifier());
		
		// Event cancel
		if(this.plugin.getManagerEvents().postEnable(user.get(), manual, jail, source)) {
			return false;
		}
		
		this.sanctions.add(manual);
		this.jail = this.findFirst(SanctionJail.class);
		
		this.plugin.getThreadAsync().execute(() -> this.sqlAddManual(manual));
		return true;
	}
	
	@Override
	public Optional<SanctionManualProfile.Ban> pardonBan(long date, Text reason, CommandSource source) {
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		Optional<EManualProfileBan> manual = this.findFirst(EManualProfileBan.class);
		
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
		
		this.plugin.getSanctionService().remove(ban);
		this.ban = this.findFirst(SanctionBanProfile.class);
		
		this.plugin.getThreadAsync().execute(() -> this.sqlPardonManual(manual.get()));
		return Optional.of(manual.get());
	}
	
	@Override
	public Optional<SanctionManualProfile.BanIp> pardonBanIp(InetAddress address, long date, Text reason, CommandSource source) {
		Preconditions.checkNotNull(address, "address");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		Optional<EManualProfileBanIp> optManual = this.findFirst(EManualProfileBanIp.class, sanction -> UtilsNetwork.equals(sanction.getAddress(), address));
		if(!optManual.isPresent()) {
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
		
		Collection<EManualProfileBanIp> manuals = this.find(EManualProfileBanIp.class);
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
		
		Optional<EManualProfileMute> manual = this.findFirst(EManualProfileMute.class);
		
		// Aucun manual
		if(!manual.isPresent()) {
			return Optional.empty();
		}
		
		final Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(this.getUniqueId());
		if (!user.isPresent()) {
			return Optional.empty();
		}
		
		// TODO : Event
		
		manual.get().pardon(date, reason, source.getIdentifier());
		this.mute = this.findFirst(SanctionMute.class);
		
		this.plugin.getThreadAsync().execute(() -> this.sqlPardonManual(manual.get()));
		return Optional.of(manual.get());
	}
	
	@Override
	public Optional<SanctionManualProfile.Jail> pardonJail(long date, Text reason, CommandSource source) {
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		Optional<EManualProfileJail> manual = this.findFirst(EManualProfileJail.class);
		
		// Aucun manual
		if(!manual.isPresent()) {
			return Optional.empty();
		}
		
		final Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(this.getUniqueId());
		if (!user.isPresent()) {
			return Optional.empty();
		}
		
		// TODO : Event
		
		manual.get().pardon(date, reason, source.getIdentifier());
		this.jail = this.findFirst(SanctionJail.class);
		
		this.plugin.getThreadAsync().execute(() -> this.sqlPardonManual(manual.get()));
		return Optional.of(manual.get());
	}
	
	/*
	 * Auto
	 */
	
	@Override
	public Optional<SanctionAuto> addSanction(SanctionAuto.Reason reason, long creation, CommandSource source) {
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		Optional<EUser> user = this.plugin.getEServer().getEUser(this.getUniqueId());
		// User introuvable
		if (!user.isPresent()) {
			return Optional.empty();
		}
		
		int level_int = this.getLevel(reason);
		SanctionAuto.Level level = reason.getLevel(level_int);
		
		
		SanctionAuto.Type type = level.getType();
		EAuto auto = null;
		if (level.getType().isBanIP()) {
			// Ip inconnue
			if (!user.get().getLastIP().isPresent()) {
				return Optional.empty();
			}
			InetAddress address = user.get().getLastIP().get();
			
			if (type.equals(SanctionAuto.Type.BAN_PROFILE_AND_IP)) {
				auto = new EAutoBanProfileAndIp(this.getUniqueId(), creation, level.getExpirationDate(creation), reason, level.getType(), level_int, source.getIdentifier(), address);
			} else if (type.equals(SanctionAuto.Type.BAN_IP)) {
				auto = new EAutoBanIp(this.getUniqueId(), creation, level.getExpirationDate(creation), reason, level.getType(), level_int, source.getIdentifier(), address);
			}
		} else if (level.getType().isJail()) {
			// Prison inconnue
			if(!level.getJail().isPresent()) {
				return Optional.empty();
			}
			
			String jail = level.getJail().get().getName();
			if (type.equals(SanctionAuto.Type.MUTE_AND_JAIL)) {
				auto = new EAutoMuteAndJail(this.getUniqueId(), creation, level.getExpirationDate(creation), reason, level.getType(), level_int, source.getIdentifier(), jail);
			} else if (type.equals(SanctionAuto.Type.JAIL)) {
				auto = new EAutoJail(this.getUniqueId(), creation, level.getExpirationDate(creation), reason, level.getType(), level_int, source.getIdentifier(), jail);
			}
		} else {
			if (type.equals(SanctionAuto.Type.BAN_PROFILE)) {
				auto = new EAutoBanProfile(this.getUniqueId(), creation, level.getExpirationDate(creation), reason, level.getType(), level_int, source.getIdentifier());
			} else if (type.equals(SanctionAuto.Type.MUTE)) {
				auto = new EAutoMute(this.getUniqueId(), creation, level.getExpirationDate(creation), reason, level.getType(), level_int, source.getIdentifier());
			}
		}
		
		// Type inconnue
		if (auto == null) {
			return Optional.empty();
		}
		
		if(auto instanceof SanctionAuto.SanctionBanProfile) {
			Ban.Profile ban = ((SanctionAuto.SanctionBanProfile) auto).getBan(user.get().getProfile());
			if(Sponge.getEventManager().post(SpongeEventFactory.createBanUserEvent(Cause.source(this).build(), ban, user.get()))) {
				return Optional.empty();
			}
			this.plugin.getSanctionService().add(ban);
		}
		
		if(auto instanceof SanctionAuto.SanctionBanIp) {
			// IP inconnu
			if(!user.get().getLastIP().isPresent()) {
				return Optional.empty();
			}
			
			// SubjectIP inconnu
			final Optional<EIpSubject> subject_ip = this.plugin.getSanctionService().getSubject(user.get().getLastIP().get());
			if (!subject_ip.isPresent()) {
				return Optional.empty();
			}
			
			Ban.Ip ban = ((SanctionAuto.SanctionBanIp) auto).getBan(user.get().getProfile(), user.get().getLastIP().get());
			if(Sponge.getEventManager().post(SpongeEventFactory.createBanIpEvent(Cause.source(this).build(), ban))) {
				return Optional.empty();
			}
			
			subject_ip.get().add(auto);
			this.plugin.getSanctionService().add(ban);
		}
		
		this.sanctions.add(auto);

		if (auto.isBan()) {
			this.ban = this.findFirst(SanctionBanProfile.class);
		} else if (auto.isMute()) {
			this.mute = this.findFirst(SanctionMute.class);
		} else if (auto.isJail()) {
			this.jail = this.findFirst(SanctionJail.class, sanction -> sanction.getJail().isPresent());
		}
		
		final EAuto auto_final = auto;
		this.plugin.getThreadAsync().execute(() -> this.sqlAddAuto(auto_final));
		return Optional.of(auto);
	}
	
	public Optional<SanctionAuto> pardonSanction(SanctionAuto.Reason type, long date, Text reason, CommandSource source) {
		Preconditions.checkNotNull(type, "type");
		Preconditions.checkNotNull(reason, "reason");
		Preconditions.checkNotNull(source, "source");
		
		Optional<EAuto> auto = this.findFirst(EAuto.class);
		// Aucun auto
		if(!auto.isPresent()) {
			return Optional.empty();
		}
		
		auto.get().pardon(date, reason, source.getIdentifier());

		if (auto.get().isBan()) {
			this.ban = this.findFirst(SanctionBanProfile.class);
		} else if (auto.get().isMute()) {
			this.mute = this.findFirst(SanctionMute.class);
		} else if (auto.get().isJail()) {
			this.jail = this.findFirst(SanctionJail.class, sanction -> sanction.getJail().isPresent());
		}
		
		this.plugin.getThreadAsync().execute(() -> this.sqlPardonAuto(auto.get()));
		return Optional.of(auto.get());
	}
	
	public int getLevel(final SanctionAuto.Reason reason) {
		return Math.toIntExact(this.sanctions.stream()
					.filter(sanction -> sanction instanceof SanctionAuto && ((SanctionAuto) sanction).getReasonSanction().equals(reason) && !sanction.isPardon())
					.count()) + 1;
	}

	@Override
	public Optional<SanctionManualProfile> getManual(SanctionManualProfile.Type type) {
		return this.findFirst(SanctionManualProfile.class, sanction -> sanction.getType().equals(type));
	}

	@Override
	public Optional<SanctionAuto> getAuto(SanctionAuto.Reason reason) {
		return this.findFirst(SanctionAuto.class, sanction -> sanction.getReasonSanction().equals(reason));
	}

	@Override
	public boolean removeManual(SanctionManualProfile profile) {
		if (this.sanctions.remove(profile)) {
			this.plugin.getThreadAsync().execute(() -> this.sqlRemoveManual(profile));
			return true;
		}
		return false;
	}

	@Override
	public boolean removeSanction(SanctionAuto profile) {
		if (this.sanctions.remove(profile)) {
			this.plugin.getThreadAsync().execute(() -> this.sqlRemoveAuto(profile));
			return true;
		}
		return false;
	}
	
	public boolean clear() {
		if (this.sanctions.isEmpty()) {
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
						Optional<InetAddress> address = UtilsNetwork.getHost(list.getString("context"));
						if (address.isPresent()) {
							profiles.add(new EManualProfileBanIp(this.getUniqueId(), address.get(), creation, expiration, reason, source, pardon_date, pardon_reason, pardon_source));
						}
					} else if (type.get().equals(SanctionManualProfile.Type.MUTE)) {
						profiles.add(new EManualProfileMute(this.getUniqueId(), creation, expiration, reason, source, pardon_date, pardon_reason, pardon_source));
					} else if (type.get().equals(SanctionManualProfile.Type.JAIL)) {
						Optional<String> jail = Optional.ofNullable(list.getString("context"));
						if (jail.isPresent()) {
							profiles.add(new EManualProfileJail(this.plugin, this.getUniqueId(), jail.get(), creation, expiration, reason, source, pardon_date, pardon_reason, pardon_source));
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
		
		Optional<String> context = Optional.empty();
		if(ban.getType().equals(SanctionManualProfile.Type.JAIL)) {
			context = Optional.of(((EManualProfileJail) ban).getJailName());
		} else if(ban.getType().equals(SanctionManualProfile.Type.BAN_IP)) {
			context = Optional.of(((EManualProfileBanIp) ban).getAddress().getHostAddress());
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
			preparedStatement.setString(7, context.orElse(null));
			
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
					 											  + "context='" + context.orElse("null") + "';"
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
				Optional<String> context = Optional.ofNullable(list.getString("context"));
				
				Optional<SanctionAuto.Type> type = SanctionAuto.Type.get(list.getString("type"));
				Optional<SanctionAuto.Reason> reason = this.plugin.getSanctionService().getReason(list.getString("reason"));
				if (type.isPresent() && reason.isPresent()) {
					int level_type = Optional.ofNullable(levels.get(type.get())).orElse(0) + 1;
					if(pardon_date != null) {
						levels.put(type.get(), level_type);
					}
					
					EAuto auto = null;
					if (context.isPresent()) {
						if (type.get().isBanIP()) {
							Optional<InetAddress> address = UtilsNetwork.getHost(context.get());
							// Ip inconnue
							if (address.isPresent()) {
								if (type.get().equals(SanctionAuto.Type.BAN_PROFILE_AND_IP)) {
									auto = new EAutoBanProfileAndIp(this.getUniqueId(), creation, expiration, reason.get(), type.get(), level_type, source, address.get(), 
											pardon_date, pardon_reason, pardon_source);
								} else if (type.get().equals(SanctionAuto.Type.BAN_IP)) {
									auto = new EAutoBanIp(this.getUniqueId(), creation, expiration, reason.get(), type.get(), level_type, source, address.get(), 
											pardon_date, pardon_reason, pardon_source);
								}
							}
						} else if (type.get().isJail()) {
							Optional<Jail> jail = reason.get().getLevel(level_type).getJail();
							// Prison inconnue
							if(jail.isPresent()) {
								if (type.get().equals(SanctionAuto.Type.MUTE_AND_JAIL)) {
									auto = new EAutoMuteAndJail(this.getUniqueId(), creation, expiration, reason.get(), type.get(), level_type, source, jail.get().getName(), 
											pardon_date, pardon_reason, pardon_source);
								} else if (type.get().equals(SanctionAuto.Type.JAIL)) {
									auto = new EAutoJail(this.getUniqueId(), creation, expiration, reason.get(), type.get(), level_type, source, jail.get().getName(), 
											pardon_date, pardon_reason, pardon_source);
								}
							}
						}
					} else {
						if (type.get().equals(SanctionAuto.Type.BAN_PROFILE)) {
							auto = new EAutoBanProfile(this.getUniqueId(), creation, expiration, reason.get(), type.get(), level_type, source, 
									pardon_date, pardon_reason, pardon_source);
						} else if (type.get().equals(SanctionAuto.Type.MUTE)) {
							auto = new EAutoMute(this.getUniqueId(), creation, expiration, reason.get(), type.get(), level_type, source, 
									pardon_date, pardon_reason, pardon_source);
						}
					}
					
					if (auto != null) {
						profiles.add(auto);
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
			preparedStatement.setString(4, ban.getTypeSanction().name());
			preparedStatement.setString(5, ban.getReasonSanction().getName());
			preparedStatement.setString(6, ban.getSource());
			preparedStatement.setString(7, ban.getContext().orElse(null));
			
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
					 											  + "type='" + ban.getTypeSanction().name() + "';"
					 											  + "reason='" + ban.getReasonSanction().getName() + "';"
					 											  + "source='" + ban.getCreationDate() + "';"
					 											  + "context='" + ban.getContext().orElse("null") + "';"
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
																  + "type='" + ban.getTypeSanction().name() + "';"
																  + "reason='" + ban.getReasonSanction().getName() + "';"
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
	
	/*
	 * Fonctions
	 */
	
	@SuppressWarnings("unchecked")
	public <T extends Sanction> Optional<T> findFirst(final Class<T> type) {
		Optional<Sanction> santion = this.sanctions.stream()
				.filter(manual -> type.isAssignableFrom(manual.getClass()) && !manual.isExpire())
				.findFirst();
		if (santion.isPresent()) {
			return Optional.of((T) santion.get());
		}
		return Optional.empty();
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Sanction> Optional<T> findFirst(final Class<T> type, final Predicate<T> predicate) {
		Optional<Sanction> santion = this.sanctions.stream()
				.filter(manual -> type.isAssignableFrom(manual.getClass()) && !manual.isExpire() && predicate.test((T) manual))
				.findFirst();
		if (santion.isPresent()) {
			return Optional.of((T) santion.get());
		}
		return Optional.empty();
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Sanction> Collection<T> find(final Class<T> type) {
		Builder<T> sanctions = ImmutableList.builder();
		
		this.sanctions.stream()
				.filter(manual -> type.isAssignableFrom(manual.getClass()) && !manual.isExpire())
				.forEach(manual -> sanctions.add((T) manual));

		return sanctions.build();
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Sanction> Collection<T> find(final Class<T> type, final Predicate<T> predicate) {
		Builder<T> sanctions = ImmutableList.builder();
		
		this.sanctions.stream()
				.filter(manual -> type.isAssignableFrom(manual.getClass()) && !manual.isExpire() && predicate.test((T) manual))
				.forEach(manual -> sanctions.add((T) manual));

		return sanctions.build();
	}
}
