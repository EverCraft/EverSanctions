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
package fr.evercraft.eversanctions.command.ban;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import fr.evercraft.everapi.EAMessage.EAMessages;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.command.ECommand;
import fr.evercraft.everapi.server.user.EUser;
import fr.evercraft.everapi.services.sanction.manual.SanctionManualProfile;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;

public class ESUnBan extends ECommand<EverSanctions> {
	
	public ESUnBan(final EverSanctions plugin) {
        super(plugin, "unban", "pardon");
        
    	// TODO Remove command : pardon
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.UNBAN.get());
	}

	@Override
	public Text description(final CommandSource source) {
		return ESMessages.UNBAN_DESCRIPTION.getText();
	}

	@Override
	public Text help(final CommandSource source) {
		return Text.builder("/" + this.getName() + " <" + EAMessages.ARGS_USER.getString() + "> <" + EAMessages.ARGS_REASON.getString() + ">")
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public Collection<String> tabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		if (args.size() == 1){
			return this.getAllUsers(args.get(0));
		} else if (args.size() == 2) {
			return Arrays.asList("reason...");
		}
		return Arrays.asList();
	}
	
	@Override
	protected List<String> getArg(final String arg) {
		List<String> args = super.getArg(arg);
		// Le message est transformer en un seul argument
		if (args.size() > 2) {
			List<String> args_send = new ArrayList<String>();
			args_send.add(args.get(0));
			args_send.add(Pattern.compile("^[ \"]*" + args.get(0) + "[ \"][ ]*").matcher(arg).replaceAll(""));
			return args_send;
		}
		return args;
	}
	
	@Override
	public CompletableFuture<Boolean> execute(final CommandSource source, final List<String> args) throws CommandException {
		// Nombre d'argument correct
		if (args.size() == 2) {
			
			Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(args.get(0));
			// Le joueur existe
			if (user.isPresent()){
				return this.commandPardonBan(source, user.get(), args.get(1));
			// Le joueur est introuvable
			} else {
				EAMessages.PLAYER_NOT_FOUND.sender()
					.prefix(ESMessages.PREFIX)
					.replace("<player>", args.get(0))
					.sendTo(source);
			}
			
		// Nombre d'argument incorrect
		} else {
			source.sendMessage(this.help(source));
		}
		
		return CompletableFuture.completedFuture(false);
	}
	
	private CompletableFuture<Boolean> commandPardonBan(final CommandSource staff, EUser user, String reason_string) {
		// Le staff et le joueur sont identique
		if (staff.getIdentifier().equals(user.getIdentifier())) {
			ESMessages.UNBAN_ERROR_EQUALS.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		Text reason = EChat.of(reason_string);
		if (reason.isEmpty()) {
			ESMessages.UNBAN_ERROR_REASON.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		// Le joueur n'a pas de ban en cours
		if (!user.getManual(SanctionManualProfile.Type.BAN_PROFILE).isPresent()) {
			ESMessages.UNBAN_ERROR_EMPTY.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		// Si l'event a été cancel
		Optional<SanctionManualProfile.Ban> pardon = user.pardonBan(System.currentTimeMillis(),  reason, staff);
		if (!pardon.isPresent()) {
			ESMessages.UNBAN_CANCEL.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		ESMessages.UNBAN_STAFF.sender()
			.replace("<reason>", reason_string)
			.replace("<player>", user.getName())
			.sendTo(staff);
		return CompletableFuture.completedFuture(true);
	}
}
