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

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.text.Text;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;
import fr.evercraft.everapi.sponge.UtilsNetwork;

public abstract class EAuto implements SanctionAuto {

	private final UUID uuid;
	
	private final Long creation;
	private Optional<Long> expiration;
	private final SanctionAuto.Reason reason;
	private final SanctionAuto.Type type;
	private final int level;
	private final String source;
	
	private Optional<Long> pardon_date;
	private Optional<Text> pardon_reason;
	private Optional<String> pardon_source;
	
	public EAuto(final UUID uuid, final long date_start, final Optional<Long> duration, final SanctionAuto.Reason reason, final SanctionAuto.Type type, final int level, 
			final String source) {
		this(uuid, date_start, duration, reason, type, level, source, Optional.empty(), Optional.empty(), Optional.empty());
	}
	
	public EAuto(final UUID uuid, final long creation, final Optional<Long> expiration, final SanctionAuto.Reason reason, final SanctionAuto.Type type, final int level, 
			final String source, final Optional<Long> pardon_date, final Optional<Text> pardon_reason, final Optional<String> pardon_source) {
		this.uuid = uuid;
		this.creation = creation;
		this.reason = reason;
		this.type = type;
		this.level = level;
		this.source = source;
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
	public SanctionAuto.Level getLevel() {
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
		return EChat.of(this.getLevel().getReason());
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
	
	public void pardon(long date, Text reason, String source) {
		this.pardon_date = Optional.ofNullable(date);
		this.pardon_reason = Optional.ofNullable(reason);
		this.pardon_source = Optional.ofNullable(source);
	}
	
	public Optional<String> getContext() {
		if (this instanceof SanctionAuto.SanctionBanIp) {
			return Optional.of(UtilsNetwork.getHostString(((SanctionAuto.SanctionBanIp) this).getAddress()));
		} else if (this instanceof SanctionAuto.SanctionBanIp) {
			return Optional.of(((SanctionAuto.SanctionJail) this).getJailName());
		}
		return Optional.empty();
	}
}
