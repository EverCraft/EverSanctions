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
import java.util.List;
import java.util.Optional;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import fr.evercraft.everapi.EAMessage.EAMessages;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.command.ECommand;
import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everapi.server.user.EUser;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;

public class ESBan extends ECommand<EverSanctions> {
	
	public ESBan(final EverSanctions plugin) {
        super(plugin, "ban", "tempban");
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.BAN.get());
	}

	@Override
	public Text description(final CommandSource source) {
		return ESMessages.BAN_DESCRIPTION.getText();
	}

	@Override
	public Text help(final CommandSource source) {
		return Text.builder("/" + this.getName() + " <" + EAMessages.ARGS_PLAYER.get() + ">")
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public List<String> tabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		List<String> suggests = new ArrayList<String>();
		if (args.size() == 1){
			suggests.addAll(this.getAllUsers(source));
		}
		return suggests;
	}
	
	@Override
	public boolean execute(final CommandSource source, final List<String> args) throws CommandException {
		// Résultat de la commande :
		boolean resultat = false;
		
		// Nombre d'argument correct
		if (args.size() == 2) {
			
			Optional<EUser> user = this.plugin.getEServer().getEUser(args.get(0));
			// Le joueur existe
			if (user.isPresent()){
				resultat = this.commandBan(source, user.get(), args.get(1));
			// Le joueur est introuvable
			} else {
				Optional<GameProfile> gameprofile = this.plugin.getEServer().getGameProfile(args.get(0));
				// Le joueur existe
				if (gameprofile.isPresent()){
					resultat = this.commandBan(source, this.plugin.getEServer().getOrCreateEUser(gameprofile.get()), args.get(1));
				// Le joueur est introuvable
				} else {
					source.sendMessage(ESMessages.PREFIX.getText().concat(EAMessages.PLAYER_NOT_FOUND.getText()));
				}
			}
			
		// Nombre d'argument incorrect
		} else {
			source.sendMessage(this.help(source));
		}
		return resultat;
	}
	
	private boolean commandBan(final CommandSource staff, EUser user, String reason_string) {
		Text reason = EChat.of(reason_string);
		if (reason.isEmpty()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BAN_ERROR_REASON.get()
						.replaceAll("<player>", user.getName())));
			return false;
		}
		
		if (!user.ban(Optional.empty(), reason, staff.getIdentifier())) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BAN_ERROR_CANCEL.get()
						.replaceAll("<player>", user.getName())));
			return false;
		}
		
		staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BAN_UNLIMITED_STAFF.get()
			 .replaceAll("<player>", user.getName())));
		
		if(user instanceof EPlayer) {
			EPlayer player = (EPlayer) user;
			player.kick(EChat.of(ESMessages.BAN_UNLIMITED_PLAYER.get()
					.replaceAll("<staff>", staff.getIdentifier())
					.replaceAll("<reason>", reason_string)));
		}
		return true;
	}
}
