/*
 * This file is part of EverEssentials.
 *
 * EverEssentials is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EverEssentials is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EverEssentials.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.evercraft.eversanctions.command;

import java.util.HashSet;

import fr.evercraft.everapi.plugin.command.ECommand;
import fr.evercraft.everapi.plugin.command.EReloadCommand;
import fr.evercraft.eversanctions.ESCommand;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.command.ban.*;
import fr.evercraft.eversanctions.command.banip.*;
import fr.evercraft.eversanctions.command.jail.*;
import fr.evercraft.eversanctions.command.jails.*;
import fr.evercraft.eversanctions.command.mute.*;
import fr.evercraft.eversanctions.command.profile.*;
import fr.evercraft.eversanctions.command.sanction.*;
import fr.evercraft.eversanctions.command.sanctions.*;
import fr.evercraft.eversanctions.command.sub.ESReload;

public class ESManagerCommands extends HashSet<ECommand<EverSanctions>> {
	
	private static final long serialVersionUID = -1;

	private final EverSanctions plugin;
	
	private final ESCommand command;
	
	public ESManagerCommands(EverSanctions plugin){
		super();
		
		this.plugin = plugin;
		
		this.command = new ESCommand(this.plugin);
		this.command.add(new ESReload(this.plugin, this.command));
		
		load();
	}
	
	public void load() {		
		register(new ESProfile(this.plugin));
		register(new ESSanctions(this.plugin));
		register(new ESSanction(this.plugin));
		register(new ESUnSanction(this.plugin));
				
		register(new ESBan(this.plugin));
		register(new ESUnBan(this.plugin));
		
		register(new ESBanIp(this.plugin));
		register(new ESUnBanIp(this.plugin));
		
		register(new ESJail(this.plugin));
		register(new ESUnJail(this.plugin));
		
		register(new ESMute(this.plugin));
		register(new ESUnMute(this.plugin));
		
		ESJails jail = new ESJails(this.plugin);
		jail.add(new ESJailsAdd(this.plugin, jail));
		jail.add(new ESJailsDelete(this.plugin, jail));
		jail.add(new ESJailsList(this.plugin, jail));
		jail.add(new ESJailsSetRadius(this.plugin, jail));
		jail.add(new ESJailsTeleport(this.plugin, jail));
		register(jail);
	}
	
	public void reload(){
		for (ECommand<EverSanctions> command : this) {
			if (command instanceof EReloadCommand) {
				((EReloadCommand<EverSanctions>) command).reload();
			}
		}
	}
	
	private void register(ECommand<EverSanctions> command) {
		this.command.add(command);
		this.add(command);
	}
}
