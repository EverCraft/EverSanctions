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

import org.spongepowered.api.text.Text;

import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;
import fr.evercraft.everapi.services.sanction.auto.SanctionAutoReason;
import fr.evercraft.everapi.services.sanction.auto.SanctionAutoType;

public class EAuto implements SanctionAuto {

	private final Long date_start;
	private Optional<Long> duration;
	private final SanctionAutoReason reason;
	private final SanctionAutoType type;
	private final int level;
	private final String source;
	private final Optional<String> option;
	
	private final Optional<Long> pardon_date;
	private final Optional<Text> pardon_reason;
	private final Optional<String> pardon_source;
	
	public EAuto(final long date_start, final Long duration, final SanctionAutoReason reason, final SanctionAutoType type, final int level, final String source) {
		this(date_start, duration, reason, type, level, source, null, null, null, null);
	}
	
	public EAuto(final long date_start, final Long duration, final SanctionAutoReason reason, final SanctionAutoType type, final int level, final String source, final String option) {
		this(date_start, duration, reason, type, level, source, null, null, null, null);
	}
	
	public EAuto(final long date_start, final Long duration, final SanctionAutoReason reason, final SanctionAutoType type, final int level, final String source, final String option, 
				final Long pardon_date, final Text pardon_reason, final String pardon_source) {
		this.date_start = date_start;
		this.reason = reason;
		this.type = type;
		this.level = level;
		this.source = source;
		this.option = Optional.ofNullable(option);
		this.duration = Optional.ofNullable(duration);
		
		this.pardon_date = Optional.ofNullable(pardon_date);
		this.pardon_reason = Optional.ofNullable(pardon_reason);
		this.pardon_source = Optional.ofNullable(pardon_source);
	}

	@Override
	public Long getCreationDate() {
		return this.date_start;
	}
	
	@Override
	public Optional<Long> getExpirationDate() {
		if(this.getDuration().isPresent()) {
			return Optional.of(this.date_start + this.getDuration().get());
		}
		return Optional.empty();
	}
	
	@Override
	public Optional<Long> getDuration() {
		return this.duration;
	}
	
	@Override
	public String getSource() {
		return this.source;
	}

	@Override
	public int getLevel() {
		return this.level;
	}

	@Override
	public SanctionAutoType getType() {
		return this.type;
	}
	
	@Override
	public SanctionAutoReason getReason() {
		return this.reason;
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
}
