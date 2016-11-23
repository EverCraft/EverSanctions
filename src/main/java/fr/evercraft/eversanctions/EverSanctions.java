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
import org.spongepowered.api.service.ban.BanService;

import fr.evercraft.everapi.EverAPI;
import fr.evercraft.everapi.exception.PluginDisableException;
import fr.evercraft.everapi.plugin.EPlugin;
import fr.evercraft.everapi.services.jail.JailService;
import fr.evercraft.everapi.services.sanction.SanctionService;
import fr.evercraft.eversanctions.command.ESManagerCommands;
import fr.evercraft.eversanctions.event.ESManagerEvents;
import fr.evercraft.eversanctions.service.EBanService;
import fr.evercraft.eversanctions.service.EJailService;

@Plugin(id = "eversanctions", 
		name = "EverSanctions", 
		version = EverAPI.VERSION, 
		description = "Manage Sanctions",
		url = "http://evercraft.fr/",
		authors = {"rexbut"},
		dependencies = {
		    @Dependency(id = "everapi", version = EverAPI.VERSION),
		    @Dependency(id = "spongeapi", version = EverAPI.SPONGEAPI_VERSION)
		})
public class EverSanctions extends EPlugin<EverSanctions> {
	private ESConfig configs;
	private ESMessage messages;
	private ESDataBase database;
	
	private EBanService ban_service;
	private EJailService jail_service;
	
	private ESManagerCommands commands;
	private ESManagerEvents events;
	
	@Override
	protected void onPreEnable() throws PluginDisableException {		
		this.configs = new ESConfig(this);
		this.messages = new ESMessage(this);
		
		this.database = new ESDataBase(this);
		this.events = new ESManagerEvents(this);
	}
	
	@Override
	protected void onEnable() throws PluginDisableException {
		this.ban_service = new EBanService(this);
		this.jail_service = new EJailService(this);
		
		this.getGame().getServiceManager().setProvider(this, BanService.class, this.ban_service);
		this.getGame().getServiceManager().setProvider(this, SanctionService.class, this.ban_service);
		this.getGame().getServiceManager().setProvider(this, JailService.class, this.jail_service);
	}
	
	@Override
	protected void onCompleteEnable() {		
		this.commands = new ESManagerCommands(this);
		this.getGame().getEventManager().registerListeners(this, new ESListener(this));
	}
	
	@Override
	protected void onStartServer() {
		this.ban_service.reload();
	}

	protected void onReload() throws PluginDisableException {
		this.reloadConfigurations();
		
		this.database.reload();
		this.ban_service.reload();
		this.jail_service.reload();
		this.commands.reload();
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
	
	public ESManagerCommands getManagerCommands() {
		return this.commands;
	}
	
	public ESManagerEvents getManagerEvents() {
		return this.events;
	}
}