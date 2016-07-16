package fr.evercraft.everkits;

import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

import fr.evercraft.everapi.plugin.EPlugin;
import fr.evercraft.everkits.command.sub.EKReload;

@Plugin(id = "fr.evercraft.everkits", 
		name = "EverKits", 
		version = "1.2", 
		description = "Manage Kit",
		url = "http://evercraft.fr/",
		authors = {"rexbut"},
		dependencies = {
		    @Dependency(id = "fr.evercraft.everapi", version = "1.2")
		})
public class EverKits extends EPlugin {
	private EKConfig configs;
	
	private EKMessage messages;
	
	@Override
	protected void onPreEnable() {		
		this.configs = new EKConfig(this);
		
		this.messages = new EKMessage(this);
		
		this.getGame().getEventManager().registerListeners(this, new EKListener(this));
	}
	
	@Override
	protected void onCompleteEnable() {
		EKCommand command = new EKCommand(this);
		
		command.add(new EKReload(this, command));
	}

	protected void onReload(){
		this.reloadConfigurations();
	}
	
	protected void onDisable() {
	}

	/*
	 * Accesseurs
	 */
	
	public EKMessage getMessages(){
		return this.messages;
	}
	
	public EKConfig getConfigs() {
		return this.configs;
	}
}
