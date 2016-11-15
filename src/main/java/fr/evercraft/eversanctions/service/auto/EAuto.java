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
package fr.evercraft.eversanctions.service.auto;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.BanTypes;
import org.spongepowered.api.util.ban.Ban.Builder;

import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;

public class EAuto implements SanctionAuto {

	private final UUID uuid;
	
	private final Long creation;
	private Optional<Long> expiration;
	private final SanctionAuto.Reason reason;
	private final SanctionAuto.Type type;
	private final int level;
	private final String source;
	
	private final Optional<String> option;
	
	private Optional<Long> pardon_date;
	private Optional<Text> pardon_reason;
	private Optional<String> pardon_source;
	
	public EAuto(final UUID uuid, final long date_start, final Optional<Long> duration, final SanctionAuto.Reason reason, final SanctionAuto.Type type, final int level, 
			final String source) {
		this(uuid, date_start, duration, reason, type, level, source, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
	}
	
	public EAuto(final UUID uuid, final long date_start, final Optional<Long> expiration, final SanctionAuto.Reason reason, final SanctionAuto.Type type, final int level, 
			final String source, final Optional<String> option) {
		this(uuid, date_start, expiration, reason, type, level, source, option, Optional.empty(), Optional.empty(), Optional.empty());
	}
	
	public EAuto(final UUID uuid, final long creation, final Optional<Long> expiration, final SanctionAuto.Reason reason, final SanctionAuto.Type type, final int level, 
			final String source, final Optional<String> option, final Optional<Long> pardon_date, final Optional<Text> pardon_reason, final Optional<String> pardon_source) {
		this.uuid = uuid;
		this.creation = creation;
		this.reason = reason;
		this.type = type;
		this.level = level;
		this.source = source;
		this.option = option;
		this.expiration = expiration;
		
		this.pardon_date = pardon_date;
		this.pardon_reason = pardon_reason;
		this.pardon_source = pardon_source;
	}
	
	@Override
	public UUID getProfile() {
		return this.uuid;
	}
	
	@Override
	public Text getReason() {
		// TODO Auto-generated method stub
		return Text.EMPTY;
	}

	@Override
	public Long getCreationDate() {
		return this.creation;
	}
	
	@Override
	public Optional<Long> getExpirationDate() {
		return this.expiration;
	}

	@Override
	public String getSource() {
		return this.source;
	}
	
	@Override
	public Optional<SanctionAuto.Level> getLevel() {
		return this.getReasonSanction().getLevel(this.getLevelNumber());
	}

	@Override
	public int getLevelNumber() {
		return this.level;
	}

	@Override
	public SanctionAuto.Type getTypeSanction() {
		return this.type;
	}
	
	@Override
	public SanctionAuto.Reason getReasonSanction() {
		return this.reason;
	}
	
	@Override
	public Text getReasonText() {
		Optional<SanctionAuto.Level> level = this.getLevel();
		if(level.isPresent()) {
			return EChat.of(level.get().getReason());
		}
		return Text.EMPTY;
	}
	
	@Override
	public Optional<String> getOption() {
		return this.option;
	}

	@Override
	public Optional<Long> getPardonDate() {
		return this.pardon_date;
	}
	
	@Override
	public Optional<Text> getPardonReason() {
		return this.pardon_reason;
	}

	@Override
	public Optional<String> getPardonSource() {
		return this.pardon_source;
	}

	@Override
	public Optional<Ban.Profile> getBan(GameProfile profile) {
		if(this.isBan()) {
			Builder builder = Ban.builder()
					.type(BanTypes.PROFILE)
					.profile(profile)
					.reason(this.getReasonText())
					.startDate(Instant.ofEpochMilli(this.getCreationDate()))
					.source(EChat.of(this.getSource()));
			
			if(this.getExpirationDate().isPresent()) {
				builder = builder.expirationDate(Instant.ofEpochMilli(this.getExpirationDate().get()));
			}
			return Optional.of((Ban.Profile) builder.build());
		}
		return Optional.empty();
	}

	@Override
	public Optional<Ban.Ip> getBan(GameProfile profile, InetAddress address) {
		if(this.isBanIP()) {
			Builder builder = Ban.builder()
					.type(BanTypes.IP)
					.address(address)
					.reason(this.getReasonText())
					.startDate(Instant.ofEpochMilli(this.getCreationDate()))
					.source(EChat.of(this.getSource()));
			
			if(this.getExpirationDate().isPresent()) {
				builder = builder.expirationDate(Instant.ofEpochMilli(this.getExpirationDate().get()));
			}
			return Optional.of((Ban.Ip) builder.build());
		}
		return Optional.empty();
	}

	public void pardon(long date, Text reason, String source) {
		this.pardon_date = Optional.ofNullable(date);
		this.pardon_reason = Optional.ofNullable(reason);
		this.pardon_source = Optional.ofNullable(source);
	}
}
