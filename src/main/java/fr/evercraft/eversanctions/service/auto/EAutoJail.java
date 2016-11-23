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

import fr.evercraft.everapi.services.jail.Jail;
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;

public class EAutoJail extends EAuto implements SanctionAuto.SanctionJail {
	
	private final String jail;
	
	public EAutoJail(final UUID uuid, final long creation, final Optional<Long> expiration, final SanctionAuto.Reason reason, final SanctionAuto.Type type, final int level, 
			final String source, final String jail) {
		super(uuid, creation, expiration, reason, type, level, source, Optional.empty(), Optional.empty(), Optional.empty());
		this.jail = jail;
	}
	
	public EAutoJail(final UUID uuid, final long creation, final Optional<Long> expiration, final SanctionAuto.Reason reason, final SanctionAuto.Type type, final int level, 
			final String source, final String jail, final Optional<Long> pardon_date, final Optional<Text> pardon_reason, final Optional<String> pardon_source) {
		super(uuid, creation, expiration, reason, type, level, source, pardon_date, pardon_reason, pardon_source);
		
		this.jail = jail;
	}

	@Override
	public String getJailName() {
		return this.jail;
	}

	@Override
	public Optional<Jail> getJail() {
		return Optional.empty();
	}
}
