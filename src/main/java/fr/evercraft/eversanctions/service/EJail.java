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
package fr.evercraft.eversanctions.service;

import java.util.Optional;

import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.World;

import fr.evercraft.everapi.server.location.LocationSQL;
import fr.evercraft.everapi.services.sanction.Jail;

public class EJail implements Jail {
	
	private final String name;
	private final int radius;
	private final LocationSQL location;
	
	public EJail(final String name, final int radius, final LocationSQL location) {
		this.name = name;
		this.radius = radius;
		this.location = location;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public int getRadius() {
		return this.radius;
	}
	
	@Override
	public Optional<Transform<World>> getTransform() {
		return this.location.getTransform();
	}

	public LocationSQL getLocationSQL() {
		return this.location;
	}
}
