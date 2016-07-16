package fr.evercraft.eversanctions;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

import fr.evercraft.everapi.plugin.command.EParentCommand;
import fr.evercraft.eversanctions.ESMessage.EKMessages;

public class ESCommand extends EParentCommand<EverSanctions> {
	
	public ESCommand(final EverSanctions plugin) {
        super(plugin, "eversanctions");
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.EVERSANCTIONS.get());
	}

	@Override
	public Text description(final CommandSource source) {
		return EKMessages.DESCRIPTION.getText();
	}

	@Override
	public boolean testPermissionHelp(final CommandSource source) {
		return source.hasPermission(ESPermissions.HELP.get());
	}
}
