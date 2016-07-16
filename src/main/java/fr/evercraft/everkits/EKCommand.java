package fr.evercraft.everkits;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

import fr.evercraft.everapi.plugin.command.EParentCommand;
import fr.evercraft.everkits.EKMessage.EKMessages;

public class EKCommand extends EParentCommand<EverKits> {
	
	public EKCommand(final EverKits plugin) {
        super(plugin, "everkits");
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(EKPermissions.EVERKITS.get());
	}

	@Override
	public Text description(final CommandSource source) {
		return EKMessages.DESCRIPTION.getText();
	}

	@Override
	public boolean testPermissionHelp(final CommandSource source) {
		return source.hasPermission(EKPermissions.HELP.get());
	}
}
