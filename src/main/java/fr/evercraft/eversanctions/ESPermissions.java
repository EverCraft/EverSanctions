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

import fr.evercraft.everapi.plugin.EnumPermission;
import fr.evercraft.everapi.plugin.file.EnumMessage;
import fr.evercraft.eversanctions.ESMessage.ESMessages;

public enum ESPermissions implements EnumPermission {
	EVERSANCTIONS("commands.execute", ESMessages.PERMISSIONS_COMMANDS_EXECUTE),
	HELP("commands.help", ESMessages.PERMISSIONS_COMMANDS_HELP),
	RELOAD("commands.reload", ESMessages.PERMISSIONS_COMMANDS_RELOAD),
	
	PROFILE("commands.profile.execute", ESMessages.PERMISSIONS_COMMANDS_PROFILE_EXECUTE),
	PROFILE_OTHERS("commands.profile.others", ESMessages.PERMISSIONS_COMMANDS_PROFILE_OTHERS),
	
	
	// Auto
	SANCTIONS("commands.sanctions.execute", ESMessages.PERMISSIONS_COMMANDS_SANCTIONS_EXECUTE),
	
	SANCTION("commands.sanction.execute", ESMessages.PERMISSIONS_COMMANDS_SANCTION_EXECUTE),
	SANCTION_OFFLINE("commands.sanction.offline", ESMessages.PERMISSIONS_COMMANDS_SANCTION_OFFLINE),
	
	UNSANCTION("commands.unsanction.execute", ESMessages.PERMISSIONS_COMMANDS_UNSANCTION_EXECUTE),
	
	
	// Manual
	BAN("commands.ban.execute", ESMessages.PERMISSIONS_COMMANDS_BAN_EXECUTE),
	BAN_UNLIMITED("commands.ban.unlimited", ESMessages.PERMISSIONS_COMMANDS_BAN_UNLIMITED),
	BAN_OFFLINE("commands.ban.offline", ESMessages.PERMISSIONS_COMMANDS_BAN_OFFLINE),
	
	BANIP("commands.banip.execute", ESMessages.PERMISSIONS_COMMANDS_BANIP_EXECUTE),
	BANIP_UNLIMITED("commands.banip.unlimited", ESMessages.PERMISSIONS_COMMANDS_BANIP_UNLIMITED),
	BANIP_OFFLINE("commands.banip.offline", ESMessages.PERMISSIONS_COMMANDS_BANIP_OFFLINE),
	
	MUTE("commands.mute.execute", ESMessages.PERMISSIONS_COMMANDS_MUTE_EXECUTE),
	MUTE_UNLIMITED("commands.mute.unlimited", ESMessages.PERMISSIONS_COMMANDS_MUTE_UNLIMITED),
	MUTE_OFFLINE("commands.mute.offline", ESMessages.PERMISSIONS_COMMANDS_MUTE_OFFLINE),
	
	JAIL("commands.jail.execute", ESMessages.PERMISSIONS_COMMANDS_JAIL_EXECUTE),
	JAIL_UNLIMITED("commands.jail.unlimited", ESMessages.PERMISSIONS_COMMANDS_JAIL_UNLIMITED),
	JAIL_OFFLINE("commands.jail.offline", ESMessages.PERMISSIONS_COMMANDS_JAIL_OFFLINE),
	
	JAILS("commands.jails.execute", ESMessages.PERMISSIONS_COMMANDS_JAILS_EXECUTE),
	JAILS_ADD("commands.jails.add", ESMessages.PERMISSIONS_COMMANDS_JAILS_ADD),
	JAILS_DELETE("commands.jails.delete", ESMessages.PERMISSIONS_COMMANDS_JAILS_DELETE),
	JAILS_LIST("commands.jails.list", ESMessages.PERMISSIONS_COMMANDS_JAILS_LIST),
	JAILS_TELEPORT("commands.jails.teleport", ESMessages.PERMISSIONS_COMMANDS_JAILS_TELEPORT),
	JAILS_SETRADIUS("commands.jails.setradius", ESMessages.PERMISSIONS_COMMANDS_JAILS_SETRADIUS),
	
	UNBAN("commands.unban.execute", ESMessages.PERMISSIONS_COMMANDS_UNBAN_EXECUTE),
	UNBANIP("commands.unbanip.execute", ESMessages.PERMISSIONS_COMMANDS_UNBANIP_EXECUTE),
	UNMUTE("commands.unmute.execute", ESMessages.PERMISSIONS_COMMANDS_UNMUTE_EXECUTE),
	UNJAIL("commands.unjail.execute", ESMessages.PERMISSIONS_COMMANDS_UNJAIL_EXECUTE);
	
	private static final String PREFIX = "eversanctions";
	
	private final String permission;
	private final EnumMessage message;
	private final boolean value;
    
    private ESPermissions(final String permission, final EnumMessage message) {
    	this(permission, message, false);
    }
    
    private ESPermissions(final String permission, final EnumMessage message, final boolean value) {   	    	
    	this.permission = PREFIX + "." + permission;
    	this.message = message;
    	this.value = value;
    }

    @Override
    public String get() {
    	return this.permission;
	}

	@Override
	public boolean getDefault() {
		return this.value;
	}

	@Override
	public EnumMessage getMessage() {
		return this.message;
	}
}
