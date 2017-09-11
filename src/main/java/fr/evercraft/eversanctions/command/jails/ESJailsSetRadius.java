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

public class ESJailsSetRadius extends ESubCommand<EverSanctions> {
	
	public ESJailsSetRadius(final EverSanctions plugin, final ESJails command) {
        super(plugin, command, "setradius");
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.JAILS_SETRADIUS.get());
	}

	@Override
	public Text description(final CommandSource source) {
		return ESMessages.JAILS_SETRADIUS_DESCRIPTION.getText();
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
			List<String> suggests = new ArrayList<String>();
			this.plugin.getJailService().getAll().forEach(jail -> suggests.add(jail.getName()));
			return suggests;
		} else if (args.size() == 2) {
			return Arrays.asList("10");
		}
		return Arrays.asList();
	}
	
	@Override
	public CompletableFuture<Boolean> execute(final CommandSource source, final List<String> args) throws CommandException {
		if (args.size() == 1) {
			return this.commandJailSetRadius((EPlayer) source, args.get(0), Optional.empty());
		} else if (args.size() == 2) {
			return this.commandJailSetRadius((EPlayer) source, args.get(0), Optional.of(args.get(1)));
		// Nombre d'argument incorrect
		} else {
			source.sendMessage(this.help(source));
		}
		
		return CompletableFuture.completedFuture(false);
	}
	
	private CompletableFuture<Boolean> commandJailSetRadius(final CommandSource staff, final String jail_name, final Optional<String> radius_string) {
		String name = EChat.fixLength(jail_name, this.plugin.getEverAPI().getConfigs().getMaxCaractere());
		
		Optional<EJail> jail = this.plugin.getJailService().getEJail(name);
		if (!jail.isPresent()) {
			ESMessages.JAIL_UNKNOWN.sender()
				.replace("{jail}", jail_name)
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		if (!radius_string.isPresent()) {
			return this.commandJailSetRadius(staff, jail.get()); 
		} else {
			try {
				return this.commandJailSetRadius(staff, jail.get(), Integer.parseInt(radius_string.get())); 
			} catch (NumberFormatException e) {
				EAMessages.IS_NOT_NUMBER.sender()
					.prefix(ESMessages.PREFIX)
					.replace("{number}", radius_string.get())
					.sendTo(staff);
			}
		}
		return CompletableFuture.completedFuture(false);
	}
	
	private CompletableFuture<Boolean> commandJailSetRadius(final CommandSource staff, final EJail jail) {
		if (jail.update(Optional.empty())) {
			ESMessages.JAILS_SETRADIUS_DEFAULT.sender()
				.replace("{radius}", String.valueOf(jail.getRadius()))
				.replace("{jail}", () -} ESJail.getButtonJail(jail))
				.sendTo(staff);
			return CompletableFuture.completedFuture(true);
		} else {
			ESMessages.JAILS_SETRADIUS_CANCEL_DEFAULT.sender()
				.replace("{radius}", String.valueOf(jail.getRadius()))
				.replace("{jail}", jail.getName())
				.sendTo(staff);
		}
		return CompletableFuture.completedFuture(false);
	}

	private CompletableFuture<Boolean> commandJailSetRadius(final CommandSource staff, final EJail jail, final int radius) {
		if (jail.update(Optional.of(radius))) {
			ESMessages.JAILS_SETRADIUS_VALUE.sender()
				.replace("{radius}", String.valueOf(jail.getRadius()))
				.replace("{jail}", () -} ESJail.getButtonJail(jail))
				.sendTo(staff);
			return CompletableFuture.completedFuture(true);
		} else {
			ESMessages.JAILS_SETRADIUS_CANCEL_VALUE.sender()
				.replace("{radius}", String.valueOf(jail.getRadius()))
				.replace("{jail}", jail.getName())
				.sendTo(staff);
		}
		return CompletableFuture.completedFuture(false);
	}
}
