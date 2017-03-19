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
package fr.evercraft.eversanctions.event;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.cause.Cause;

import fr.evercraft.everapi.event.ESpongeEventFactory;
import fr.evercraft.everapi.server.user.EUser;
import fr.evercraft.everapi.services.jail.Jail;
import fr.evercraft.everapi.services.sanction.Sanction.SanctionJail;
import fr.evercraft.everapi.services.sanction.Sanction.SanctionMute;
import fr.evercraft.eversanctions.EverSanctions;

public class ESManagerEvents {
	private EverSanctions plugin;
	
	public ESManagerEvents(final EverSanctions plugin) {
		this.plugin = plugin;
	}
	
	public Cause getCause() {
		return Cause.source(this.plugin).build();
	}
	
	/*
	 * Jail
	 */
	
	public boolean postEnable(EUser user, SanctionJail sanction, Jail jail, CommandSource source) {
		this.plugin.getELogger().debug("Event SanctionJail.Enable : ("
				+ "uuid='" + user.getIdentifier() + "';"
				+ "jail='" + sanction.getJailName() + "';"
				+ "creation='" + sanction.getCreationDate() + "')");
		return this.plugin.getGame().getEventManager().post(ESpongeEventFactory.createJailEventEnable(user, sanction, jail, source, this.getCause()));
	}
	
	public void postDisable(UUID uuid, SanctionJail sanction, Optional<CommandSource> pardonSource) {
		this.plugin.getELogger().debug("Event SanctionJail.Disable : ("
				+ "uuid='" + uuid + "';"
				+ "jail='" + sanction.getJailName() + "';"
				+ "expiration='" + sanction.getExpirationDate().orElse(-1L) + "')");
		Optional<EUser> user = this.plugin.getEServer().getEUser(uuid);
		if (user.isPresent()) {
			this.plugin.getGame().getEventManager().post(ESpongeEventFactory.createJailEventDisable(user.get(), sanction, pardonSource, this.getCause()));
		}
	}
	
	/*
	 * Mute
	 */
	
	public boolean postEnable(EUser user, SanctionMute sanction, CommandSource source) {
		this.plugin.getELogger().debug("Event SanctionMute.Enable : ("
				+ "uuid='" + user.getIdentifier() + "';"
				+ "creation='" + sanction.getCreationDate() + "')");
		return this.plugin.getGame().getEventManager().post(ESpongeEventFactory.createMuteEventEnable(user, sanction, source, this.getCause()));
	}
	
	public void postDisable(UUID uuid, SanctionMute sanction, Optional<CommandSource> pardonSource) {
		this.plugin.getELogger().debug("Event SanctionMute.Disable : ("
				+ "uuid='" + uuid + "';"
				+ "expiration='" + sanction.getExpirationDate().orElse(-1L) + "')");
		Optional<EUser> user = this.plugin.getEServer().getEUser(uuid);
		if (user.isPresent()) {
			this.plugin.getGame().getEventManager().post(ESpongeEventFactory.createMuteEventDisable(user.get(), sanction, pardonSource, this.getCause()));
		}
	}
}
