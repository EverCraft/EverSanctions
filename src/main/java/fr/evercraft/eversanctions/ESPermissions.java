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
	EVERSANCTIONS("commands.execute"),
	HELP("commands.help"),
	RELOAD("commands.reload"),
	
	PROFILE("commands.profile.execute"),
	PROFILE_OTHERS("commands.profile.others"),
	
	
	// Auto
	SANCTIONS("commands.sanctions.execute"),
	
	SANCTION("commands.sanction.execute"),
	SANCTION_OFFLINE("commands.sanction.offline"),
	
	UNSANCTION("commands.unsanction.execute"),
	
	
	// Manual
	BAN("commands.ban.execute"),
	BAN_UNLIMITED("commands.ban.unlimited"),
	BAN_OFFLINE("commands.ban.offline"),
	
	BANIP("commands.banip.execute"),
	BANIP_UNLIMITED("commands.banip.unlimited"),
	BANIP_OFFLINE("commands.banip.offline"),
	
	MUTE("commands.mute.execute"),
	MUTE_UNLIMITED("commands.mute.unlimited"),
	MUTE_OFFLINE("commands.mute.offline"),
	
	JAIL("commands.jail.execute"),
	JAIL_UNLIMITED("commands.jail.execute"),
	JAIL_OFFLINE("commands.jail.offline"),
	
	JAILS("commands.jails.execute"),
	JAILS_ADD("commands.jails.add"),
	JAILS_DELETE("commands.jails.delete"),
	JAILS_LIST("commands.jails.list"),
	JAILS_TELEPORT("commands.jails.teleport"),
	JAILS_SETRADIUS("commands.jails.setradius"),
	
	UNBAN("commands.unban.execute"),
	UNBANIP("commands.unbanip.execute"),
	UNMUTE("commands.unmute.execute"),
	UNJAIL("commands.unjail.execute");
	
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
