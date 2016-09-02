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
package fr.evercraft.eversanctions.service.subject;

import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import org.spongepowered.api.text.Text;

import fr.evercraft.everapi.services.sanction.SubjectUserSanction;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.auto.EAuto;
import fr.evercraft.eversanctions.service.manual.EManual;

public class EUserSubject implements SubjectUserSanction {
	
	private final EverSanctions plugin;
	private final UUID uuid;
	
	private final ConcurrentSkipListSet<EManual> manual;
	private final ConcurrentSkipListSet<EAuto> auto;
	
	private boolean ban;
	private boolean mute;
	private boolean jail;

	public EUserSubject(final EverSanctions plugin, final UUID uuid) {
		this.plugin = plugin;
		this.uuid = uuid;
		
		this.manual = new ConcurrentSkipListSet<EManual>((EManual o1, EManual o2) -> o1.getCreationDate().compareTo(o2.getCreationDate()));
		this.auto = new ConcurrentSkipListSet<EAuto>((EAuto o1, EAuto o2) -> o1.getCreationDate().compareTo(o2.getCreationDate()));
	}
	
	public void reload() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isBan() {
		return this.ban;
	}

	@Override
	public boolean isMute() {
		return this.mute;
	}

	@Override
	public boolean isJail() {
		return this.jail;
	}

	public boolean addBan(long creation, long duration, Text reason, final String source) {
		return false;
	}
	
	public boolean pardon(Text reason, String source) {
		return false;
	}

}
