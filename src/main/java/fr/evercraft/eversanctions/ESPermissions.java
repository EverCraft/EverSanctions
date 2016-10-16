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

import org.spongepowered.api.command.CommandSource;

import com.google.common.base.Preconditions;

import fr.evercraft.everapi.plugin.EnumPermission;

public enum ESPermissions implements EnumPermission {
	EVERSANCTIONS("plugin.command"),
	
	HELP("plugin.help"),
	RELOAD("plugin.reload"),
	
	PROFILE("profile.command"),
	PROFILE_OTHERS("profile.others"),
	
	BAN("ban.command"),
	BAN_UNLIMITED("ban.unlimited"),
	BAN_OFFLINE("ban.offline"),
	
	BANIP("banip.command"),
	BANIP_UNLIMITED("banip.unlimited"),
	BANIP_OFFLINE("banip.offline"),
	
	MUTE("mute.command"),
	MUTE_UNLIMITED("mute.unlimited"),
	MUTE_OFFLINE("mute.offline"),
	
	JAIL("jail.command"),
	JAIL_UNLIMITED("jail.command"),
	JAIL_OFFLINE("jail.offline"),
	
	JAILS("jails.command"),
	JAILS_ADD("jails.add"),
	JAILS_DELETE("jails.delete"),
	JAILS_LIST("jails.list"),
	JAILS_TELEPORT("jails.teleport"),
	JAILS_SETRADIUS("jails.setradius"),
	
	UNBAN("unban.command"),
	UNBANIP("unbanip.command"),
	UNMUTE("unmute.command"),
	UNJAIL("unjail.command");
	
	private final static String prefix = "eversanctions";
	
	private final String permission;
    
    private ESPermissions(final String permission) {   	
    	Preconditions.checkNotNull(permission, "La permission '" + this.name() + "' n'est pas définit");
    	
    	this.permission = permission;
    }

    public String get() {
		return ESPermissions.prefix + "." + this.permission;
	}
    
    public boolean has(CommandSource player) {
    	return player.hasPermission(this.get());
    }
}
