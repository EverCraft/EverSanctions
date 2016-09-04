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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;

public class EAutoReason implements SanctionAuto.Reason {
	
	private final String name;
	private final ConcurrentSkipListMap<Integer, EAutoLevel> levels;
	
	public EAutoReason(final String name, Map<Integer, EAutoLevel> levels) {
		this.name = name;
		this.levels = new ConcurrentSkipListMap<Integer, EAutoLevel>((Integer o1, Integer o2) -> o1.compareTo(o2));
	}

	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public Optional<SanctionAuto.Level> getLevel(int level) {
		if(level <= 0) {
			new IllegalArgumentException("Level is negative");
		}
		
		return Optional.ofNullable(this.levels.floorEntry(level).getValue());
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Collection<SanctionAuto.Level> getLevels() {
		return (Collection) this.levels.values();
	}
	
}