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
package fr.evercraft.eversanctions.command.jails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import fr.evercraft.everapi.EAMessage.EAMessages;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.command.ESubCommand;
import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.command.jail.ESJail;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.EJail;

public class ESJailsDelete extends ESubCommand<EverSanctions> {
	
	public ESJailsDelete(final EverSanctions plugin, final ESJails command) {
        super(plugin, command, "delete");
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.JAILS_DELETE.get());
	}

	@Override
	public Text description(final CommandSource source) {
		return ESMessages.JAILS_DELETE_DESCRIPTION.getText();
	}

	@Override
	public Text help(final CommandSource source) {
		return Text.builder("/" + this.getName() + " <" + EAMessages.ARGS_JAIL.getString() + ">")
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public Collection<String> tabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		if (args.size() == 1) {
			List<String> suggests = new ArrayList<String>();
			this.plugin.getJailService().getAll().forEach(jail -> suggests.add(jail.getName()));
			return suggests;
		}
		return Arrays.asList();
	}
	
	@Override
	public CompletableFuture<Boolean> execute(final CommandSource source, final List<String> args) throws CommandException {
		if (args.size() == 1) {
			return this.commandJailDelete((EPlayer) source, args.get(0)); 
		} else if (args.size() == 2 && args.get(1).equalsIgnoreCase("confirmation")) {
			return this.commandJailDeleteConfirmation((EPlayer) source, args.get(0)); 
		// Nombre d'argument incorrect
		} else {
			source.sendMessage(this.help(source));
		}
		
		return CompletableFuture.completedFuture(false);
	}
	
	private CompletableFuture<Boolean> commandJailDelete(final EPlayer player, final String jail_name) {
		String name = EChat.fixLength(jail_name, this.plugin.getEverAPI().getConfigs().getMaxCaractere());
		
		Optional<EJail> jail = this.plugin.getJailService().getEJail(name);
		// Le serveur a une prison qui porte ce nom
		if (jail.isPresent()) {
			ESMessages.JAILS_DELETE_CONFIRMATION.sender()
				.replace("{jail}", () -> ESJail.getButtonJail(jail.get()))
				.replace("{confirmation}", () -> this.getButtonConfirmation(name))
				.sendTo(player);
		// Le serveur n'a pas de prison qui porte ce nom
		} else {
			ESMessages.JAIL_UNKNOWN.sender()
				.replace("{jail}", name)
				.sendTo(player);
		}
		return CompletableFuture.completedFuture(false);
	}
	
	private CompletableFuture<Boolean> commandJailDeleteConfirmation(final EPlayer player, final String jail_name) {
		String name = EChat.fixLength(jail_name, this.plugin.getEverAPI().getConfigs().getMaxCaractere());
		
		Optional<EJail> jail = this.plugin.getJailService().getEJail(name);
		// Le serveur a une prison qui porte ce nom
		if (jail.isPresent()) {
			// Si la prison a bien été supprimer
			if (this.plugin.getJailService().remove(name)) {
				ESMessages.JAILS_DELETE_DELETE.sender()
					.replace("{jail}", () -> ESJail.getButtonJail(jail.get()))
					.sendTo(player);
				return CompletableFuture.completedFuture(true);
			// La prison n'a pas été supprimer
			} else {
				ESMessages.JAILS_DELETE_CANCEL.sender()
					.replace("{jail}", name)
					.sendTo(player);
			}
		// Le serveur n'a pas de prison qui porte ce nom
		} else {
			ESMessages.JAIL_UNKNOWN.sender()
				.replace("{jail}", name)
				.sendTo(player);
		}
		return CompletableFuture.completedFuture(false);
	}
	
	private Text getButtonConfirmation(final String name){
		return ESMessages.JAILS_DELETE_CONFIRMATION_VALID.getText().toBuilder()
					.onHover(TextActions.showText(ESMessages.JAILS_DELETE_CONFIRMATION_VALID_HOVER.getFormat()
							.toText("{jail}", name)))
					.onClick(TextActions.runCommand("/jails delete \"" + name + "\" confirmation"))
					.build();
	}
}
