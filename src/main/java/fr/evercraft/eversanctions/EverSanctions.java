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
package fr.evercraft.eversanctions;

import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

import fr.evercraft.everapi.exception.PluginDisableException;
import fr.evercraft.everapi.plugin.EPlugin;
import fr.evercraft.eversanctions.command.sub.ESReload;
import fr.evercraft.eversanctions.service.EBanService;
import fr.evercraft.eversanctions.service.EJailService;

@Plugin(id = "fr.evercraft.eversanctions", 
		name = "EverSanctions", 
		version = "1.2", 
		description = "Manage Sanctions",
		url = "http://evercraft.fr/",
		authors = {"rexbut"},
		dependencies = {
		    @Dependency(id = "fr.evercraft.everapi", version = "1.2")
		})
public class EverSanctions extends EPlugin {
	private ESConfig configs;
	private ESMessage messages;
	
	private ESDataBase database;
	
	private EBanService ban_service;
	private EJailService jail_service;
	
	@Override
	protected void onPreEnable() throws PluginDisableException {		
		this.configs = new ESConfig(this);
		this.messages = new ESMessage(this);
		
		this.database = new ESDataBase(this);
		
		this.ban_service = new EBanService(this);
		this.jail_service = new EJailService(this);
		
		this.getGame().getEventManager().registerListeners(this, new ESListener(this));
	}
	
	@Override
	protected void onCompleteEnable() {
		ESCommand command = new ESCommand(this);
		
		command.add(new ESReload(this, command));
	}

	protected void onReload() throws PluginDisableException{
		this.reloadConfigurations();
		
		this.database.reload();
	}
	
	protected void onDisable() {
	}

	/*
	 * Accesseurs
	 */
	
	public ESMessage getMessages(){
		return this.messages;
	}
	
	public ESConfig getConfigs() {
		return this.configs;
	}

	public ESDataBase getDataBase() {
		return this.database;
	}

	public EBanService getSanctionService() {
		return this.ban_service;
	}
	
	public EJailService getJailService() {
		return this.jail_service;
	}
}