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
import fr.evercraft.everapi.services.sanction.JailService;
import fr.evercraft.everapi.services.sanction.SanctionService;
import fr.evercraft.eversanctions.command.ban.ESBan;
import fr.evercraft.eversanctions.command.ban.ESUnBan;
import fr.evercraft.eversanctions.command.banip.ESBanIp;
import fr.evercraft.eversanctions.command.banip.ESUnBanIp;
import fr.evercraft.eversanctions.command.jail.ESJail;
import fr.evercraft.eversanctions.command.jail.ESUnJail;
import fr.evercraft.eversanctions.command.jails.ESJails;
import fr.evercraft.eversanctions.command.jails.ESJailsAdd;
import fr.evercraft.eversanctions.command.jails.ESJailsDelete;
import fr.evercraft.eversanctions.command.jails.ESJailsList;
import fr.evercraft.eversanctions.command.jails.ESJailsSetRadius;
import fr.evercraft.eversanctions.command.jails.ESJailsTeleport;
import fr.evercraft.eversanctions.command.mute.ESMute;
import fr.evercraft.eversanctions.command.mute.ESUnMute;
import fr.evercraft.eversanctions.command.sub.ESReload;
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
	}
	
	@Override
	protected void onEnable() throws PluginDisableException {
		this.ban_service = new EBanService(this);
		this.jail_service = new EJailService(this);
		
		this.getGame().getServiceManager().setProvider(this, BanService.class, this.ban_service);
		this.getGame().getServiceManager().setProvider(this, SanctionService.class, this.ban_service);
		this.getGame().getServiceManager().setProvider(this, JailService.class, this.jail_service);
		
		this.getGame().getEventManager().registerListeners(this, new ESListener(this));
	}
	
	@Override
	protected void onCompleteEnable() {		
		ESCommand command = new ESCommand(this);
		command.add(new ESReload(this, command));
		
		new ESBan(this);
		new ESUnBan(this);
		
		new ESBanIp(this);
		new ESUnBanIp(this);
		
		new ESJail(this);
		new ESUnJail(this);
		
		new ESMute(this);
		new ESUnMute(this);
		
		ESJails jail = new ESJails(this);
		jail.add(new ESJailsAdd(this, jail));
		jail.add(new ESJailsDelete(this, jail));
		jail.add(new ESJailsList(this, jail));
		jail.add(new ESJailsSetRadius(this, jail));
		jail.add(new ESJailsTeleport(this, jail));
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