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
import fr.evercraft.everapi.services.jail.Jail;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.command.jail.ESJail;
import fr.evercraft.eversanctions.service.EJail;

public class ESJailsAdd extends ESubCommand<EverSanctions> {
	
	public ESJailsAdd(final EverSanctions plugin, final ESJails command) {
        super(plugin, command, "add");
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.JAILS_ADD.get());
	}

	@Override
	public Text description(final CommandSource source) {
		return ESMessages.JAILS_ADD_DESCRIPTION.getText();
	}

	@Override
	public Text help(final CommandSource source) {
		return Text.builder("/" + this.getName() + " <" + EAMessages.ARGS_JAIL.getString() + "> [" + EAMessages.ARGS_RADIUS.getString() + "]")
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public Collection<String> tabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		if (args.size() == 1) {
			return Arrays.asList("jail...");
		} else if (args.size() == 2) {
			return Arrays.asList(String.valueOf(this.plugin.getConfigs().getJailRadius()));
		}
		return Arrays.asList();
	}
	
	@Override
	public CompletableFuture<Boolean> execute(final CommandSource source, final List<String> args) throws CommandException {
		if (args.size() == 1) {
			// Si la source est un joueur
			if (source instanceof EPlayer) {
				return this.commandJailSet((EPlayer) source, args.get(0)); 
			// La source n'est pas un joueur
			} else {
				EAMessages.COMMAND_ERROR_FOR_PLAYER.sender()
					.prefix(ESMessages.PREFIX)
					.sendTo(source);
			}
		} else if (args.size() == 2) {
			// Si la source est un joueur
			if (source instanceof EPlayer) {
				try {
					return this.commandJailSet((EPlayer) source, args.get(0), Integer.parseInt(args.get(1))); 
				} catch (NumberFormatException e) {
					EAMessages.IS_NOT_NUMBER.sender()
						.prefix(ESMessages.PREFIX)
						.replace("{number}", args.get(1))
						.sendTo(source);
				}
			// La source n'est pas un joueur
			} else {
				EAMessages.COMMAND_ERROR_FOR_PLAYER.sender()
					.prefix(ESMessages.PREFIX)
					.sendTo(source);
			}
		// Nombre d'argument incorrect
		} else {
			source.sendMessage(this.help(source));
		}
		
		return CompletableFuture.completedFuture(false);
	}
	
	private CompletableFuture<Boolean> commandJailSet(final EPlayer player, final String jail_name) {
		String name = EChat.fixLength(jail_name, this.plugin.getEverAPI().getConfigs().getMaxCaractere());
		
		Optional<EJail> jail = this.plugin.getJailService().getEJail(name);
		if (jail.isPresent()) {
			if (jail.get().update(player.getTransform())) {
				ESMessages.JAILS_ADD_REPLACE.sender()
					.replace("{jail}", () -> ESJail.getButtonJail(jail.get()))
					.sendTo(player);
				return CompletableFuture.completedFuture(true);
			} else {
				ESMessages.JAILS_ADD_CANCEL_REPLACE.sender()
					.replace("{jail}", name)
					.sendTo(player);
			}
		} else {
			Optional<Jail> jail_new = this.plugin.getJailService().add(name, player.getTransform(), Optional.empty());
			if (jail_new.isPresent()) {
				ESMessages.JAILS_ADD_NEW.sender()
					.replace("{jail}", () -> ESJail.getButtonJail(jail.get()))
					.sendTo(player);
				return CompletableFuture.completedFuture(true);
			} else {
				ESMessages.JAILS_ADD_CANCEL_NEW.sender()
					.replace("{jail}", name)
					.sendTo(player);
			}
		}
		return CompletableFuture.completedFuture(false);
	}

	private CompletableFuture<Boolean> commandJailSet(final EPlayer player, final String jail_name, final int radius) {
		String name = EChat.fixLength(jail_name, this.plugin.getEverAPI().getConfigs().getMaxCaractere());
		
		Optional<EJail> jail = this.plugin.getJailService().getEJail(name);
		if (jail.isPresent()) {
			if (jail.get().update(player.getTransform(), Optional.of(radius))) {
				ESMessages.JAILS_ADD_REPLACE.sender()
					.replace("{jail}", () -> ESJail.getButtonJail(jail.get()))
					.sendTo(player);
				return CompletableFuture.completedFuture(true);
			} else {
				ESMessages.JAILS_ADD_CANCEL_REPLACE.sender()
					.replace("{jail}", name)
					.sendTo(player);
			}
		} else {
			Optional<Jail> jail_new = this.plugin.getJailService().add(name, player.getTransform(), Optional.of(radius));
			if (jail_new.isPresent()) {
				ESMessages.JAILS_ADD_NEW.sender()
					.replace("{jail}", () -> ESJail.getButtonJail(jail.get()))
					.sendTo(player);
				return CompletableFuture.completedFuture(true);
			} else {
				ESMessages.JAILS_ADD_CANCEL_NEW.sender()
					.replace("{jail}", name)
					.sendTo(player);
			}
		}
		return CompletableFuture.completedFuture(false);
	}
}
