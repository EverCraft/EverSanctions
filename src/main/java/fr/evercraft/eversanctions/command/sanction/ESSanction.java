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
package fr.evercraft.eversanctions.command.sanction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import fr.evercraft.everapi.EAMessage.EAMessages;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.command.ECommand;
import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everapi.server.user.EUser;
import fr.evercraft.everapi.services.sanction.SanctionService;
import fr.evercraft.everapi.services.sanction.manual.SanctionManualProfile;
import fr.evercraft.everapi.sponge.UtilsDate;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;

public class ESSanction extends ECommand<EverSanctions> {
	
	public ESSanction(final EverSanctions plugin) {
        super(plugin, "sanction");
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.SANCTION.get());
	} 

	@Override
	public Text description(final CommandSource source) {
		return ESMessages.SANCTION_DESCRIPTION.getText();
	}

	@Override
	public Text help(final CommandSource source) {
		return Text.builder("/" + this.getName() + " <" + EAMessages.ARGS_PLAYER.get() + "> <" + EAMessages.ARGS_TYPE.get() + ">")
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public List<String> tabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		List<String> suggests = new ArrayList<String>();
		if (args.size() == 1){
			suggests.addAll(this.getAllUsers(source));
		} else if (args.size() == 2) {
			this.plugin.getSanctionService().getAllReasons().forEach(sanction -> suggests.add(sanction.getName()));
		}
		return suggests;
	}
	
	@Override
	public boolean execute(final CommandSource source, final List<String> args) throws CommandException {
		// Résultat de la commande :
		boolean resultat = false;
		
		// Nombre d'argument correct
		if (args.size() == 2) {
			
			Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(args.get(0));
			// Le joueur existe
			if (user.isPresent()){
				resultat = this.commandMute(source, user.get(), args.get(1), args.get(2));
			// Le joueur est introuvable
			} else {
				source.sendMessage(ESMessages.PREFIX.getText().concat(EAMessages.PLAYER_NOT_FOUND.getText()));
			}
			
		// Nombre d'argument incorrect
		} else {
			source.sendMessage(this.help(source));
		}
		return resultat;
	}
	
	private boolean commandMute(final CommandSource staff, EUser user, final String time_string, final String reason) {
		// Le staff et le joueur sont identique
		if (staff.getIdentifier().equals(user.getIdentifier())) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.MUTE_ERROR_EQUALS.get()
				.replaceAll("<player>", user.getName())));
			return false;
		}
		
		// Le joueur a déjà un mute en cours
		if (user.getManual(SanctionManualProfile.Type.MUTE).isPresent()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.MUTE_ERROR_NOEMPTY.get()
				.replaceAll("<player>", user.getName())));
			return false;
		}
		
		// Aucune raison
		if (reason.isEmpty()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.MUTE_ERROR_REASON.get()
						.replaceAll("<player>", user.getName())));
			return false;
		}
		
		long creation = System.currentTimeMillis();
			
		// Ban définitif
		if (time_string.equalsIgnoreCase(SanctionService.UNLIMITED)) {
			return this.commandUnlimitedMute(staff, user, creation, reason);
		}
		
		Optional<Long> time = UtilsDate.parseDuration(creation, time_string, true);
		
		// Temps incorrect
		if (!time.isPresent()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + EAMessages.IS_NOT_TIME.get()
				.replaceAll("<time>", time_string)));
			return false;
		}
		
		// Ban tempotaire
		return this.commandTempMute(staff, user, creation, time.get(), reason);
	}
	
	private boolean commandUnlimitedMute(final CommandSource staff, final EUser user, final long creation, final String reason) {
		// Ban annulé
		if (!user.mute(creation, Optional.empty(), EChat.of(reason), staff)) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.MUTE_ERROR_CANCEL.get()
						.replaceAll("<player>", user.getName())));
			return false;
		}
		
		staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.MUTE_UNLIMITED_STAFF.get()
			 .replaceAll("<reason>", reason)
			 .replaceAll("<player>", user.getName())));
		
		if(user instanceof EPlayer) {
			EPlayer player = (EPlayer) user;
			player.sendMessage(EChat.of(ESMessages.MUTE_UNLIMITED_PLAYER.get()
					.replaceAll("<staff>", staff.getName())
					.replaceAll("<reason>", reason)));
		}
		return true;
	}
	
	private boolean commandTempMute(final CommandSource staff, final EUser user, final long creation, final long expiration, final String reason) {
		if (!user.mute(creation, Optional.of(expiration), EChat.of(reason), staff)) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.MUTE_ERROR_CANCEL.get()
						.replaceAll("<player>", user.getName())));
			return false;
		}
		
		staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.MUTE_TEMP_STAFF.get()
			 .replaceAll("<player>", user.getName())
			 .replaceAll("<reason>", reason)
			 .replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(creation, expiration))
			 .replaceAll("<time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration))
			 .replaceAll("<date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration))
			 .replaceAll("<datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration))));
		
		if(user instanceof EPlayer) {
			EPlayer player = (EPlayer) user;
			player.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.MUTE_TEMP_PLAYER.get()
				 .replaceAll("<staff>", staff.getName())
				 .replaceAll("<reason>", reason)
				 .replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(creation, expiration))
				 .replaceAll("<time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration))
				 .replaceAll("<date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration))
				 .replaceAll("<datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration))));
		}
		return true;
	}
}
