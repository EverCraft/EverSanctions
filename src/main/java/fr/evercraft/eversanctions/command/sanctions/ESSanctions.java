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
package fr.evercraft.eversanctions.command.sanctions;

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
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;

public class ESSanctions extends ECommand<EverSanctions> {

	public ESSanctions(final EverSanctions plugin) {
        super(plugin, "sanctions");
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.SANCTIONS.get());
	} 

	@Override
	public Text description(final CommandSource source) {
		return ESMessages.SANCTIONS_DESCRIPTION.getText();
	}

	@Override
	public Text help(final CommandSource source) {
		return Text.builder("/" + this.getName() + " [" + EAMessages.ARGS_TYPE.get() + "]")
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public List<String> tabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		List<String> suggests = new ArrayList<String>();
		if (args.size() == 1){
			this.plugin.getSanctionService().getAllReasons().forEach(sanction -> suggests.add(sanction.getName()));
		}
		return suggests;
	}
	
	@Override
	public boolean execute(final CommandSource source, final List<String> args) throws CommandException {
		// Résultat de la commande :
		boolean resultat = false;
		
		// Nombre d'argument correct
		if (args.isEmpty()) {
			resultat = this.commandSanctions(source);
		} else if (args.size() == 1) {
			Optional<SanctionAuto.Reason> reason = this.plugin.getSanctionService().getReason(args.get(0).toUpperCase());
			if (reason.isPresent()) {
				resultat = this.commandSanctions(source, reason.get());
			} else {
				source.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.SANCTIONS_REASON_UNKNOWN.get()
						.replaceAll("<name>", args.get(0))));
			}
		} else {
			source.sendMessage(this.help(source));
		}
		return resultat;
	}
	
	private boolean commandSanctions(final CommandSource staff) {
		List<Text> list = new ArrayList<Text>();
		this.plugin.getSanctionService().getAllReasons().forEach(reason -> {
			list.add(EChat.of(ESMessages.SANCTIONS_LIST_LINE.get()
					.replaceAll("<name>", reason.getName())
					.replaceAll("<count>", String.valueOf(reason.getLevels().size())))
				.toBuilder()
				.onClick(TextActions.runCommand("/sanctions \"" + reason.getName() + "\""))
				.onHover(TextActions.showText(EChat.of(ESMessages.SANCTIONS_LIST_LINE_HOVER.get()
					.replaceAll("<name>", reason.getName()))))
				.build());
		});
		
		if (list.isEmpty()) {
			list.add(ESMessages.SANCTIONS_LIST_EMPTY.getText());
		}
		
		this.plugin.getEverAPI().getManagerService().getEPagination().sendTo(
				EChat.of(ESMessages.SANCTIONS_LIST_TITLE.get()).toBuilder()
					.onClick(TextActions.runCommand("/sanctions"))
					.build(), 
				list, staff);
		return true;
	}
	
	private boolean commandSanctions(final CommandSource staff, final SanctionAuto.Reason reason) {
		List<Text> list = new ArrayList<Text>();
		reason.getLevels().forEach((num, level) -> {
			String message = "";
			if (level.isIndefinite()) {
				if (level.getJail().isPresent()) {
					message = ESMessages.SANCTIONS_REASON_LINE_UNLIMITED_JAIL.get()
							.replaceAll("<jail>", level.getJail().get().getName());
				} else {
					message = ESMessages.SANCTIONS_REASON_LINE_UNLIMITED.get();
				}
			} else {
				if (level.getJail().isPresent()) {
					message = ESMessages.SANCTIONS_REASON_LINE_TEMP_JAIL.get()
							.replaceAll("<jail>", level.getJail().get().getName());
				} else {
					message = ESMessages.SANCTIONS_REASON_LINE_TEMP.get();
				}
				message = message.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDuration(level.getDuration().get()).orElse("ERROR"));
			}
			
			list.add(EChat.of(message
					.replaceAll("<num>", String.valueOf(num))
					.replaceAll("<type>", this.getType(level.getType()))
					.replaceAll("<reason>", level.getReason())
					.replaceAll("<count>", String.valueOf(reason.getLevels().size()))));
		});
		
		if (list.isEmpty()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + EAMessages.COMMAND_ERROR.get()));
			return false;
		}
		
		this.plugin.getEverAPI().getManagerService().getEPagination().sendTo(
				EChat.of(ESMessages.SANCTIONS_REASON_TITLE.get()
						.replaceAll("<name>", reason.getName())
						.replaceAll("<count>", String.valueOf(reason.getLevels().size()))).toBuilder()
					.onClick(TextActions.runCommand("/sanctions \"" + reason.getName() + "\""))
					.build(), 
				list, staff);
		return true;
	}
	
	public String getType(SanctionAuto.Type type) {
		if(type.equals(SanctionAuto.Type.BAN_PROFILE_AND_IP)) {
			return ESMessages.PROFILE_AUTO_BAN_PROFILE_AND_IP.get();
		} else if(type.equals(SanctionAuto.Type.BAN_PROFILE)) {
			return ESMessages.PROFILE_AUTO_BAN_PROFILE.get();
		} else if(type.equals(SanctionAuto.Type.BAN_IP)) {
			return ESMessages.PROFILE_AUTO_BAN_IP.get();
		} else if(type.equals(SanctionAuto.Type.MUTE_AND_JAIL)) {
			return ESMessages.PROFILE_AUTO_MUTE_AND_JAIL.get();
		} else if(type.equals(SanctionAuto.Type.MUTE)) {
			return ESMessages.PROFILE_AUTO_MUTE.get();
		} else if(type.equals(SanctionAuto.Type.JAIL)) {
			return ESMessages.PROFILE_AUTO_JAIL.get();
		}
		return "";
	}
}
