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
import fr.evercraft.everapi.plugin.command.ECommand;
import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everapi.server.user.EUser;
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;
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
		return Text.builder("/" + this.getName() + " <" + EAMessages.ARGS_PLAYER.getString() + "> <" + EAMessages.ARGS_TYPE.getString() + ">")
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
				Optional<SanctionAuto.Reason> reason = this.plugin.getSanctionService().getReason(args.get(1));
				if (reason.isPresent()) {
					resultat = this.commandSanction(source, user.get(), reason.get());
				} else {
					ESMessages.SANCTION_ERROR_UNKNOWN.sender()
						.replace("<name>", args.get(0))
						.sendTo(source);
				}
			// Le joueur est introuvable
			} else {
				EAMessages.PLAYER_NOT_FOUND.sender()
					.prefix(ESMessages.PREFIX)
					.sendTo(source);
			}
			
		// Nombre d'argument incorrect
		} else {
			source.sendMessage(this.help(source));
		}
		return resultat;
	}
	
	private boolean commandSanction(final CommandSource staff, EUser user, final SanctionAuto.Reason reason) {
		// Le staff et le joueur sont identique
		if (staff.getIdentifier().equals(user.getIdentifier())) {
			ESMessages.SANCTION_ERROR_EQUALS.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return false;
		}
		
		// Le joueur a déjà une sanction en cours
		if (user.getAuto(reason).isPresent()) {
			ESMessages.SANCTION_ERROR_NOEMPTY.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return false;
		}
		
		long creation = System.currentTimeMillis();
		Optional<SanctionAuto> sanction = user.addSanction(reason, creation, staff);
		
		// Sanction annule
		if (!sanction.isPresent()) {
			ESMessages.SANCTION_ERROR_CANCEL.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return false;
		}
		
		ESMessages.SANCTION_STAFF.sender()
			.replace("<player>", user.getName())
			.replace("<reason>", sanction.get().getReason())
			.sendTo(staff);
		
		if(user instanceof EPlayer) {
			ESMessages.SANCTION_PLAYER.sender()
				.replace("<staff>", staff.getName())
				.replace("<reason>", sanction.get().getReason())
				.sendTo((EPlayer) user);
		}
		return true;
	}
}
