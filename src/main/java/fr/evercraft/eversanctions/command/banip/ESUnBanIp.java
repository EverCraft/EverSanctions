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
package fr.evercraft.eversanctions.command.banip;

import java.net.InetAddress;
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
import org.spongepowered.api.util.ban.Ban;

import com.google.common.net.InetAddresses;

import fr.evercraft.everapi.EAMessage.EAMessages;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.command.ECommand;
import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everapi.server.user.EUser;
import fr.evercraft.everapi.services.sanction.SanctionIpSubject;
import fr.evercraft.everapi.sponge.UtilsNetwork;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;

public class ESUnBanIp extends ECommand<EverSanctions> {
	
	public ESUnBanIp(final EverSanctions plugin) {
        super(plugin, "unbanip", "pardon-ip");
        
        this.plugin.getGame().getCommandManager().get("pardon-ip").ifPresent(command ->
        	this.plugin.getGame().getCommandManager().removeMapping(command));
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
		return Text.builder("/" + this.getName() + " <" + EAMessages.ARGS_USER.getString() + "|" + EAMessages.ARGS_IP.getString() + "> <" + EAMessages.ARGS_REASON.getString() + ">")
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public Collection<String> tabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		if (args.size() == 1) {
			List<String> suggests = new ArrayList<String>();
			for (Ban.Profile ban : this.plugin.getSanctionService().getProfileBans()) {
				suggests.add(ban.getProfile().getName().orElse(ban.getProfile().getUniqueId().toString()));
			}
			for (Ban.Ip ban : this.plugin.getSanctionService().getIpBans()) {
				suggests.add(UtilsNetwork.getHostString(ban.getAddress()));
			}
			return suggests;
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
			
			Optional<InetAddress> address = UtilsNetwork.getHost(args.get(0));
			if (InetAddresses.isInetAddress(args.get(0)) && address.isPresent()) {
				Optional<SanctionIpSubject> subject = this.plugin.getSanctionService().get(address.get());
				if (subject.isPresent()) {
					return this.commandUnBanIP(source, subject.get(), args.get(1));
				} else {
					EAMessages.COMMAND_ERROR.sender()
						.prefix(ESMessages.PREFIX)
						.sendTo(source);
				}
			} else {
				Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(args.get(0));
				// Le joueur existe
				if (user.isPresent()){
					return this.commandUnBanIP(source, user.get(), args.get(1));
				// Le joueur est introuvable
				} else {
					EAMessages.PLAYER_NOT_FOUND.sender()
						.prefix(ESMessages.PREFIX)
						.replace("<player>", args.get(0))
						.sendTo(source);
				}
			}

		// Nombre d'argument incorrect
		} else {
			source.sendMessage(this.help(source));
		}
		
		return CompletableFuture.completedFuture(false);
	}
	
	private CompletableFuture<Boolean> commandUnBanIP(final CommandSource staff, SanctionIpSubject subject, String reason_string) {
		// Le staff et le joueur sont identique
		if (staff instanceof EPlayer && UtilsNetwork.equals(((EPlayer) staff).getConnection().getAddress(), UtilsNetwork.getSocketAddress(subject.getAddress()))) {
			ESMessages.UNBANIP_IP_ERROR_EQUALS.sender()
				.replace("<address>", subject.getIdentifier())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		Text reason = EChat.of(reason_string);
		if (reason.isEmpty()) {
			ESMessages.UNBANIP_IP_ERROR_REASON.sender()
				.replace("<address>", subject.getIdentifier())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		// Le joueur n'a pas de ban en cours
		if (!subject.isBanManual()) {
			ESMessages.UNBANIP_IP_ERROR_EMPTY.sender()
				.replace("<address>", subject.getIdentifier())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		// Si l'event a été cancel
		if (!subject.pardonBan(System.currentTimeMillis(),  reason, staff).isEmpty()) {
			ESMessages.UNBANIP_IP_CANCEL.sender()
				.replace("<address>", subject.getIdentifier())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		ESMessages.UNBANIP_IP_STAFF.sender()
			.replace("<reason>", reason_string)
			.replace("<address>", subject.getIdentifier())
			.sendTo(staff);
		return CompletableFuture.completedFuture(true);
	}
	
	private CompletableFuture<Boolean> commandUnBanIP(final CommandSource staff, EUser user, String reason_string) {
		// Le staff et le joueur sont identique
		if (staff.getIdentifier().equals(user.getIdentifier())) {
			ESMessages.UNBANIP_PLAYER_ERROR_EQUALS.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		Text reason = EChat.of(reason_string);
		if (reason.isEmpty()) {
			ESMessages.UNBANIP_PLAYER_ERROR_REASON.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		// Le joueur n'a pas de ban en cours
		if (!user.isBanIp()) {
			ESMessages.UNBANIP_PLAYER_ERROR_EMPTY.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		// Si l'event a été cancel
		if (user.pardonBanIp(System.currentTimeMillis(),  reason, staff).isEmpty()) {
			ESMessages.UNBANIP_PLAYER_CANCEL.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		ESMessages.UNBANIP_PLAYER_STAFF.sender()
			.replace("<reason>", reason_string)
			.replace("<player>", user.getName())
			.sendTo(staff);
		return CompletableFuture.completedFuture(true);
	}
}
