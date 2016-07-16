package fr.evercraft.eversanctions;

import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

import fr.evercraft.everapi.plugin.EPlugin;
import fr.evercraft.eversanctions.command.sub.ESReload;

@Plugin(id = "fr.evercraft.everkits", 
		name = "EverKits", 
		version = "1.2", 
		description = "Manage Kit",
		url = "http://evercraft.fr/",
		authors = {"rexbut"},
		dependencies = {
		    @Dependency(id = "fr.evercraft.everapi", version = "1.2")
		})
public class EverSanctions extends EPlugin {
	private ESConfig configs;
	
	private ESMessage messages;
	
	@Override
	protected void onPreEnable() {		
		this.configs = new ESConfig(this);
		
		this.messages = new ESMessage(this);
		
		this.getGame().getEventManager().registerListeners(this, new ESListener(this));
	}
	
	@Override
	protected void onCompleteEnable() {
		ESCommand command = new ESCommand(this);
		
		command.add(new ESReload(this, command));
	}

	protected void onReload(){
		this.reloadConfigurations();
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
}
