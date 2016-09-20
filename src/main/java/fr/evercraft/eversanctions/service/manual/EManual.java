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
package fr.evercraft.eversanctions.service.manual;

import java.util.Optional;

import org.spongepowered.api.text.Text;

import fr.evercraft.everapi.services.sanction.manual.SanctionManual;

public abstract class EManual implements SanctionManual {

	private final Long date_start;
	private Optional<Long> expiration;
	private final Text reason;
	private final String source;
	
	private Optional<Long> pardon_date;
	private Optional<Text> pardon_reason;
	private Optional<String> pardon_source;
	
	public EManual(final long date_start, final Optional<Long> expiration, final Text reason, final String source, 
			Optional<Long> pardon_date, Optional<Text> pardon_reason, Optional<String> pardon_source) {
		this.date_start = date_start;
		this.reason = reason;
		this.source = source;
		this.expiration = expiration;
		
		this.pardon_date = pardon_date;
		this.pardon_reason = pardon_reason;
		this.pardon_source = pardon_source;
	}

	@Override
	public Long getCreationDate() {
		return this.date_start;
	}
	
	@Override
	public Optional<Long> getExpirationDate() {
		return this.expiration;
	}
	
	@Override
	public Text getReason() {
		return this.reason;
	}
	
	@Override
	public String getSource() {
		return this.source;
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
	
	public void pardon(Long date, Text reason, String source) {
		this.pardon_date = Optional.ofNullable(date);
		this.pardon_reason = Optional.ofNullable(reason);
		this.pardon_source = Optional.ofNullable(source);
	}
}
