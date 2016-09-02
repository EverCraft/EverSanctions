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

import fr.evercraft.everapi.services.sanction.Jail;
import fr.evercraft.everapi.services.sanction.auto.SanctionAutoLevel;
import fr.evercraft.everapi.services.sanction.auto.SanctionAutoType;

public class EAutoLevel implements SanctionAutoLevel {
	
	private final SanctionAutoType type;
	private final Optional<Long> duration;
	private final String reason;
	private final Optional<Jail> jail;
	
	public EAutoLevel(final SanctionAutoType type, final Optional<Long> duration, String reason) {
		this(type, duration, reason, null);
	}
	
	public EAutoLevel(final SanctionAutoType type, final Optional<Long> duration, String reason, Jail jail) {
		this.type = type;
		this.duration = duration;
		this.reason = reason;
		this.jail = Optional.ofNullable(jail);
	}

	@Override
	public SanctionAutoType getType() {
		return this.type;
	}

	@Override
	public Optional<Long> getDuration() {
		return this.duration;
	}

	@Override
	public String getReason() {
		return this.reason;
	}

	@Override
	public Optional<Jail> getJail() {
		return this.jail;
	}
}
