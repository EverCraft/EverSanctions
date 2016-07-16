package fr.evercraft.everkits;

import org.spongepowered.api.command.CommandSource;

import com.google.common.base.Preconditions;

import fr.evercraft.everapi.plugin.EnumPermission;

public enum EKPermissions implements EnumPermission {
	EVERKITS("command"),
	
	HELP("help"),
	RELOAD("reload");
	
	private final static String prefix = "everkits";
	
	private final String permission;
    
    private EKPermissions(final String permission) {   	
    	Preconditions.checkNotNull(permission, "La permission '" + this.name() + "' n'est pas définit");
    	
    	this.permission = permission;
    }

    public String get() {
		return EKPermissions.prefix + "." + this.permission;
	}
    
    public boolean has(CommandSource player) {
    	return player.hasPermission(this.get());
    }
}
