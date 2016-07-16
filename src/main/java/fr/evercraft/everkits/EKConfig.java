package fr.evercraft.everkits;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import fr.evercraft.everapi.plugin.file.EConfig;
import fr.evercraft.everapi.plugin.file.EMessage;

public class EKConfig extends EConfig {

	public EKConfig(final EverKits plugin) {
		super(plugin);
	}
	
	public void reload() {
		super.reload();
		this.plugin.getLogger().setDebug(this.isDebug());
	}
	
	@Override
	public void loadDefault() {
		addDefault("debug", false, "Displays plugin performance in the logs");
		addDefault("language", EMessage.ENGLISH, "Select language messages", "Examples : ", "  French : FR_fr", "  English : EN_en");
		
		addDefault("enable-format", true);
		addDefault("enable-icons", true);
		
		ConfigurationNode replaces = this.get("replaces");
		if(replaces.getValue() == null) {
			replaces.getNode("[<3]").setValue("\u2764");
			replaces.getNode("[check]").setValue("\u2714");
			replaces.getNode("[*]").setValue("\u2716");
			replaces.getNode("[ARROW]").setValue("\u279c");
			replaces.getNode("[X]").setValue("\u2588");
			replaces.getNode("[RT]").setValue("\n");
		}
		
		addDefault("format-default", "<DISPLAYNAME_FORMAT> &7:&f <MESSAGE>");
		
		ConfigurationNode formats = this.get("format-groups");
		if(formats.getValue() == null) {
			formats.getNode("Admin").setValue("&f[&4Admin&f] <DISPLAYNAME_FORMAT> &7:&f <MESSAGE>");
			formats.getNode("Moderator").setValue("&f[&5Moderator&f] <DISPLAYNAME_FORMAT> &7:&f <MESSAGE>");
		}
	}
	
	public Map<String, String> getReplaces() {
		Map<String, String> replaces = new HashMap<String, String>();
		for(Entry<Object, ? extends CommentedConfigurationNode> node : this.get("replaces").getChildrenMap().entrySet()) {
			if(node.getKey() instanceof String) {
				replaces.put((String) node.getKey(), node.getValue().getString(""));
			}
		}
		return replaces;
	}
	
	public Map<String, String> getFormatGroups() {
		Map<String, String> replaces = new HashMap<String, String>();
		for(Entry<Object, ? extends CommentedConfigurationNode> node : this.get("format-groups").getChildrenMap().entrySet()) {
			String value = node.getValue().getString(null);
			if(node.getKey() instanceof String && value != null) {
				replaces.put((String) node.getKey(), value);
			}
		}
		return replaces;
	}
	
	public String getFormatDefault() {
		return this.get("format-default").getString("<<NAME>> <MESSAGE>");
	}
	
	public boolean enableFormat() {
		return this.get("enable-format").getBoolean(true);
	}
	
	public boolean enableIcons() {
		return this.get("enable-icons").getBoolean(true);
	}
}
