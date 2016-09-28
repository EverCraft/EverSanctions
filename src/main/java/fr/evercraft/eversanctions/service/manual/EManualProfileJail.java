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
import java.util.UUID;

import org.spongepowered.api.text.Text;

import fr.evercraft.everapi.services.sanction.manual.SanctionManualProfile;

public class EManualProfileJail extends EManualProfile implements SanctionManualProfile.Jail {
	
	public EManualProfileJail(final UUID uuid, final String jail, final long creation, final Optional<Long> expiration, final Text reason, final String source) {
		this(uuid, jail, creation, expiration, reason, source, Optional.empty(), Optional.empty(), Optional.empty());
	}
	
	public EManualProfileJail(final UUID uuid, final String jail, final long date_start, final Optional<Long> expiration, final Text reason, final String source, 
			final Optional<Long> pardon_date, final Optional<Text> pardon_reason, final Optional<String> pardon_source) {
		super(uuid, date_start, expiration, reason, source, pardon_date, pardon_reason, pardon_source);
	}

	@Override
	public String getJailName() {
		return null;
	}

	@Override
	public Optional<Jail> getJail() {
		return null;
	}
}
