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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import fr.evercraft.everapi.EAMessage.EAMessages;
import fr.evercraft.everapi.message.replace.EReplace;
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
		return Text.builder("/" + this.getName() + " [" + EAMessages.ARGS_TYPE.getString() + "]")
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public Collection<String> tabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		if (args.size() == 1) {
			List<String> suggests = new ArrayList<String>();
			this.plugin.getSanctionService().getAllReasons().forEach(sanction -> suggests.add(sanction.getName()));
			return suggests;
		}
		return Arrays.asList();
	}
	
	@Override
	public CompletableFuture<Boolean> execute(final CommandSource source, final List<String> args) throws CommandException {
		// Nombre d'argument correct
		if (args.isEmpty()) {
			return this.commandSanctions(source);
		} else if (args.size() == 1) {
			Optional<SanctionAuto.Reason> reason = this.plugin.getSanctionService().getReason(args.get(0));
			if (reason.isPresent()) {
				return this.commandSanctions(source, reason.get());
			} else {
				ESMessages.SANCTIONS_REASON_UNKNOWN.sender()
					.replace("<name>", args.get(0))
					.sendTo(source);
			}
		} else {
			source.sendMessage(this.help(source));
		}
		return CompletableFuture.completedFuture(false);
	}
	
	private CompletableFuture<Boolean> commandSanctions(final CommandSource staff) {
		List<Text> list = new ArrayList<Text>();
		this.plugin.getSanctionService().getAllReasons().forEach(reason -> {
			list.add(ESMessages.SANCTIONS_LIST_LINE.getFormat().toText(
						"<name>", reason.getName(),
						"<count>", String.valueOf(reason.getLevels().size()))
				.toBuilder()
				.onClick(TextActions.runCommand("/sanctions \"" + reason.getName() + "\""))
				.onHover(TextActions.showText(ESMessages.SANCTIONS_LIST_LINE_HOVER.getFormat()
					.toText("<name>", reason.getName())))
				.build());
		});
		
		if (list.isEmpty()) {
			list.add(ESMessages.SANCTIONS_LIST_EMPTY.getText());
		}
		
		this.plugin.getEverAPI().getManagerService().getEPagination().sendTo(
				ESMessages.SANCTIONS_LIST_TITLE.getText().toBuilder()
					.onClick(TextActions.runCommand("/sanctions"))
					.build(), 
				list, staff);
		return CompletableFuture.completedFuture(true);
	}
	
	private CompletableFuture<Boolean> commandSanctions(final CommandSource staff, final SanctionAuto.Reason reason) {
		List<Text> list = new ArrayList<Text>();
		reason.getLevels().forEach((num, level) -> {
			
			Map<String, EReplace<?>> replaces = new HashMap<String, EReplace<?>>();
			replaces.put("<num>", EReplace.of(String.valueOf(num)));
			replaces.put("<type>", EReplace.of(this.getType(level.getType())));
			replaces.put("<reason>", EReplace.of(level.getReason()));
			replaces.put("<count>", EReplace.of(String.valueOf(reason.getLevels().size())));
			
			ESMessages message = null;
			if (level.isIndefinite()) {
				if (level.getJail().isPresent()) {
					message = ESMessages.SANCTIONS_REASON_LINE_UNLIMITED_JAIL;
					replaces.put("<jail>", EReplace.of(level.getJail().get().getName()));
				} else {
					message = ESMessages.SANCTIONS_REASON_LINE_UNLIMITED;
				}
			} else {
				if (level.getJail().isPresent()) {
					message = ESMessages.SANCTIONS_REASON_LINE_TEMP_JAIL;
					replaces.put("<jail>", EReplace.of(level.getJail().get().getName()));
				} else {
					message = ESMessages.SANCTIONS_REASON_LINE_TEMP;
				}
				replaces.put("<duration>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDuration(level.getDuration().get()).orElse("ERROR")));
			}
			
			if (message != null) {
				list.add(message.getFormat().toText2(replaces));
			}
		});
		
		if (list.isEmpty()) {
			EAMessages.COMMAND_ERROR.sender()
				.prefix(ESMessages.PREFIX)
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		this.plugin.getEverAPI().getManagerService().getEPagination().sendTo(
				ESMessages.SANCTIONS_REASON_TITLE.getFormat().toText(
							"<name>", reason.getName(),
							"<count>", String.valueOf(reason.getLevels().size())).toBuilder()
					.onClick(TextActions.runCommand("/sanctions \"" + reason.getName() + "\""))
					.build(), 
				list, staff);
		return CompletableFuture.completedFuture(true);
	}
	
	public Text getType(SanctionAuto.Type type) {
		if(type.equals(SanctionAuto.Type.BAN_PROFILE_AND_IP)) {
			return ESMessages.PROFILE_AUTO_BAN_PROFILE_AND_IP.getText();
		} else if(type.equals(SanctionAuto.Type.BAN_PROFILE)) {
			return ESMessages.PROFILE_AUTO_BAN_PROFILE.getText();
		} else if(type.equals(SanctionAuto.Type.BAN_IP)) {
			return ESMessages.PROFILE_AUTO_BAN_IP.getText();
		} else if(type.equals(SanctionAuto.Type.MUTE_AND_JAIL)) {
			return ESMessages.PROFILE_AUTO_MUTE_AND_JAIL.getText();
		} else if(type.equals(SanctionAuto.Type.MUTE)) {
			return ESMessages.PROFILE_AUTO_MUTE.getText();
		} else if(type.equals(SanctionAuto.Type.JAIL)) {
			return ESMessages.PROFILE_AUTO_JAIL.getText();
		}
		return Text.EMPTY;
	}
}
