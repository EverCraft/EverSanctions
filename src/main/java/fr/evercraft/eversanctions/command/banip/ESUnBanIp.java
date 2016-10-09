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
import java.util.List;
import java.util.Optional;
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
        
        // TODO Remove command : pardon-ip
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
		return Text.builder("/" + this.getName() + " <" + EAMessages.ARGS_PLAYER.get() + "|" + EAMessages.ARGS_IP.get() + "> <" + EAMessages.ARGS_REASON.get() + ">")
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public List<String> tabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		List<String> suggests = new ArrayList<String>();
		if (args.size() == 1) {
			for (Ban.Profile ban : this.plugin.getSanctionService().getProfileBans()) {
				suggests.add(ban.getProfile().getName().orElse(ban.getProfile().getUniqueId().toString()));
			}
			for (Ban.Ip ban : this.plugin.getSanctionService().getIpBans()) {
				suggests.add(UtilsNetwork.getHostString(ban.getAddress()));
			}
		} else if (args.size() == 2) {
			suggests.add("reason...");
		}
		return suggests;
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
	public boolean execute(final CommandSource source, final List<String> args) throws CommandException {
		// Résultat de la commande :
		boolean resultat = false;
		
		// Nombre d'argument correct
		if (args.size() == 2) {
			
			Optional<InetAddress> address = UtilsNetwork.getHost(args.get(0));
			if (InetAddresses.isInetAddress(args.get(0)) && address.isPresent()) {
				Optional<SanctionIpSubject> subject = this.plugin.getSanctionService().get(address.get());
				if (subject.isPresent()) {
					resultat = this.commandUnBanIP(source, subject.get(), args.get(1));
				} else {
					source.sendMessage(ESMessages.PREFIX.getText().concat(EAMessages.COMMAND_ERROR.getText()));
				}
			} else {
				Optional<EUser> user = this.plugin.getEServer().getEUser(args.get(0));
				// Le joueur existe
				if (user.isPresent()){
					resultat = this.commandUnBanIP(source, user.get(), args.get(1));
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
	
	private boolean commandUnBanIP(final CommandSource staff, SanctionIpSubject subject, String reason_string) {
		// Le staff et le joueur sont identique
		if (staff instanceof EPlayer && UtilsNetwork.equals(((EPlayer) staff).getConnection().getAddress(), UtilsNetwork.getSocketAddress(subject.getAddress()))) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.UNBANIP_IP_ERROR_EQUALS.get()
				.replaceAll("<address>", subject.getIdentifier())));
			return false;
		}
		
		Text reason = EChat.of(reason_string);
		if (reason.isEmpty()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.UNBANIP_IP_ERROR_REASON.get()
				.replaceAll("<address>", subject.getIdentifier())));
			return false;
		}
		
		// Le joueur n'a pas de ban en cours
		if (!subject.isBanManual()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.UNBANIP_IP_ERROR_EMPTY.get()
			.replaceAll("<address>", subject.getIdentifier())));
			return false;
		}
		
		// Si l'event a été cancel
		if (!subject.pardonBan(System.currentTimeMillis(),  reason, staff).isEmpty()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.UNBANIP_IP_CANCEL.get()
			.replaceAll("<address>", subject.getIdentifier())));
			return false;
		}
		
		staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.UNBANIP_IP_STAFF.get()
			.replaceAll("<reason>", reason_string)
			.replaceAll("<address>", subject.getIdentifier())));
		return true;
	}
	
	private boolean commandUnBanIP(final CommandSource staff, EUser user, String reason_string) {
		// Le staff et le joueur sont identique
		if (staff.getIdentifier().equals(user.getIdentifier())) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.UNBANIP_PLAYER_ERROR_EQUALS.get()
				.replaceAll("<player>", user.getName())));
			return false;
		}
		
		Text reason = EChat.of(reason_string);
		if (reason.isEmpty()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.UNBANIP_PLAYER_ERROR_REASON.get()
						.replaceAll("<player>", user.getName())));
			return false;
		}
		
		// Le joueur n'a pas de ban en cours
		if (!user.isBanIp()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.UNBANIP_PLAYER_ERROR_EMPTY.get()
				.replaceAll("<player>", user.getName())));
			return false;
		}
		
		// Si l'event a été cancel
		if (user.pardonBanIp(System.currentTimeMillis(),  reason, staff).isEmpty()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.UNBANIP_PLAYER_CANCEL.get()
						.replaceAll("<player>", user.getName())));
			return false;
		}
		
		staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.UNBANIP_PLAYER_STAFF.get()
			.replaceAll("<reason>", reason_string)
			.replaceAll("<player>", user.getName())));
		return true;
	}
}
