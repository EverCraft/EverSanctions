package fr.evercraft.eversanctions.command.jails;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.command.EParentCommand;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;

public class ESJails extends EParentCommand<EverSanctions> {
	
	public ESJails(final EverSanctions plugin) {
        super(plugin, "jails");
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.JAILS.get());
	}

	@Override
	public Text description(final CommandSource source) {
		return EChat.of(ESMessages.JAILS_DESCRIPTION.get());
	}

	@Override
	public boolean testPermissionHelp(final CommandSource source) {
		return true;
	}
}