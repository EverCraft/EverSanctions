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
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import com.google.common.net.InetAddresses;

import fr.evercraft.everapi.EAMessage.EAMessages;
import fr.evercraft.everapi.message.replace.EReplace;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.command.ECommand;
import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everapi.server.user.EUser;
import fr.evercraft.everapi.services.sanction.SanctionIpSubject;
import fr.evercraft.everapi.services.sanction.SanctionService;
import fr.evercraft.everapi.sponge.UtilsDate;
import fr.evercraft.everapi.sponge.UtilsNetwork;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;

public class ESBanIp extends ECommand<EverSanctions> {
	
	public ESBanIp(final EverSanctions plugin) {
        super(plugin, "banip", "ban-ip");
        
     // TODO Remove command : ban-ip
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.BANIP.get());
	} 

	@Override
	public Text description(final CommandSource source) {
		return ESMessages.BANIP_DESCRIPTION.getText();
	}

	@Override
	public Text help(final CommandSource source) {
		return Text.builder("/" + this.getName() + " <" + EAMessages.ARGS_USER.getString() + "|" + EAMessages.ARGS_IP.getString() + "> "
				+ "<" + EAMessages.ARGS_TIME.getString() + "> <" + EAMessages.ARGS_REASON.getString() + ">")
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public Collection<String> tabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		if (args.size() == 1){
			return this.getAllUsers(args.get(0));
		} else if (args.size() == 2) {
			return Arrays.asList(SanctionService.UNLIMITED, "\"1mo 7d 12h\"", "1h");
		} else if (args.size() == 3) {
			return Arrays.asList("reason...");
		}
		return Arrays.asList();
	}
	
	@Override
	protected List<String> getArg(final String arg) {
		List<String> args = super.getArg(arg);
		// Le message est transformer en un seul argument
		if (args.size() > 3) {
			List<String> args_send = new ArrayList<String>();
			args_send.add(args.get(0));
			args_send.add(args.get(1));
			args_send.add(Pattern.compile("^[ \"]*" + args.get(0) + "[ \"]*" + args.get(1) + "[ \"][ ]*").matcher(arg).replaceAll(""));
			return args_send;
		}
		return args;
	}
	
	@Override
	public boolean execute(final CommandSource source, final List<String> args) throws CommandException {
		// Résultat de la commande :
		boolean resultat = false;
		
		// Nombre d'argument correct
		if (args.size() == 3) {
			
			Optional<InetAddress> address = UtilsNetwork.getHost(args.get(0));
			if (InetAddresses.isInetAddress(args.get(0)) && address.isPresent()) {
				Optional<SanctionIpSubject> subject = this.plugin.getSanctionService().get(address.get());
				if (subject.isPresent()) {
					resultat = this.commandBanIP(source, subject.get(), args.get(1), args.get(2));
				} else {
					source.sendMessage(ESMessages.PREFIX.getText().concat(EAMessages.COMMAND_ERROR.getText()));
				}
			} else {
				Optional<EUser> user = this.plugin.getEServer().getEUser(args.get(0));
				// Le joueur existe
				if (user.isPresent()){
					resultat = this.commandBanIP(source, user.get(), args.get(1), args.get(2));
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
	
	/*
	 * Ip
	 */
	
	private boolean commandBanIP(final CommandSource staff, final SanctionIpSubject subject, final String time_string, final String reason) {
		// Le staff et le joueur sont identique
		if (staff instanceof EPlayer && UtilsNetwork.equals(((EPlayer) staff).getConnection().getAddress(), UtilsNetwork.getSocketAddress(subject.getAddress()))) {
			ESMessages.BANIP_IP_ERROR_EQUALS.sender()
				.replace("<address>", subject.getIdentifier())
				.sendTo(staff);
			return false;
		}
		
		// Le joueur a déjà un ban en cours
		if (this.plugin.getSanctionService().isBanned(subject.getAddress())) {
			ESMessages.BANIP_IP_ERROR_NOEMPTY.sender()
				.replace("<address>", subject.getIdentifier())
				.sendTo(staff);
			return false;
		}
		
		// Aucune raison
		if (reason.isEmpty()) {
			ESMessages.BANIP_IP_ERROR_REASON.sender()
				.replace("<address>", subject.getIdentifier())
				.sendTo(staff);
			return false;
		}
		
		long creation = System.currentTimeMillis();
			
		// Ban définitif
		if (time_string.equalsIgnoreCase(SanctionService.UNLIMITED)) {
			return this.commandUnlimitedBanIP(staff, subject, creation, reason);
		}
		
		Optional<Long> time = UtilsDate.parseDuration(creation, time_string, true);
		
		// Temps incorrect
		if (!time.isPresent()) {
			EAMessages.IS_NOT_TIME.sender()
				.prefix(ESMessages.PREFIX)
				.replace("<time>", time_string)
				.sendTo(staff);
			return false;
		}
		
		// Ban tempotaire
		return this.commandTempBanIP(staff, subject, creation, time.get(), reason);
	}
	
	private boolean commandUnlimitedBanIP(final CommandSource staff, final SanctionIpSubject subject, final long creation, final String reason) {
		// Ban annulé
		if (!subject.ban(creation, Optional.empty(), EChat.of(reason), staff)) {
			ESMessages.BANIP_IP_ERROR_CANCEL.sender()
				.replace("<address>", subject.getIdentifier())
				.sendTo(staff);
			return false;
		}
		
		ESMessages.BANIP_IP_UNLIMITED_STAFF.sender()
			 .replace("<reason>", reason)
			 .replace("<address>", subject.getIdentifier())
			 .sendTo(staff);
		
		this.plugin.getEServer().getOnlineEPlayers().stream()
			.filter(player -> UtilsNetwork.equals(player.getConnection().getAddress(), subject.getSocketAddress()))
			.forEach(player ->
				player.kick(ESMessages.BANIP_IP_UNLIMITED_PLAYER.getFormat().toText(
						"<staff>", staff.getIdentifier(),
						"<reason>", reason))
			);
		return true;
	}
	
	private boolean commandTempBanIP(final CommandSource staff, final SanctionIpSubject subject, final long creation, final long expiration, final String reason) {
		if (!subject.ban(creation, Optional.of(expiration), EChat.of(reason), staff)) {
			ESMessages.BANIP_IP_ERROR_CANCEL.sender()
				.replace("<address>", subject.getIdentifier())
				.sendTo(staff);
			return false;
		}
		
		Map<String, EReplace<?>> replaces = new HashMap<String, EReplace<?>>();
		replaces.put("<staff>", EReplace.of(staff.getIdentifier()));
		replaces.put("<reason>", EReplace.of(reason));
		replaces.put("<address>", EReplace.of(subject.getIdentifier()));
		replaces.put("<duration>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(creation, expiration)));
		replaces.put("<time>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration)));
		replaces.put("<date>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration)));
		replaces.put("<datetime>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration)));
		
		ESMessages.BANIP_IP_TEMP_STAFF.sender()
			.replaceString(replaces)
			.sendTo(staff);
		
		this.plugin.getEServer().getOnlineEPlayers().stream()
			.filter(player -> UtilsNetwork.equals(player.getConnection().getAddress(), subject.getSocketAddress()))
			.forEach(player -> player.kick(ESMessages.BANIP_IP_TEMP_PLAYER.getFormat().toText2(replaces)));
		return true;
	}
	
	/*
	 * Player
	 */
	
	private boolean commandBanIP(final CommandSource staff, EUser user, final String time_string, final String reason) {		
		Optional<InetAddress> last = user.getLastIP();
		// Aucune adresse IP
		if (!last.isPresent()) {
			ESMessages.BANIP_PLAYER_ERROR_IP.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return false;
		}
		
		Optional<SanctionIpSubject> subject = this.plugin.getSanctionService().get(last.get());
		// Impossible de récupérer le subject IP
		if (!subject.isPresent()) {
			EAMessages.COMMAND_ERROR.sender()
				.prefix(ESMessages.PREFIX)
				.sendTo(staff);
			return false;
		}
		
		// Le staff et le joueur sont identique
		if (staff instanceof EPlayer && UtilsNetwork.equals(((EPlayer) staff).getConnection().getAddress(), UtilsNetwork.getSocketAddress(last.get()))) {
			ESMessages.BANIP_PLAYER_ERROR_EQUALS.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return false;
		}
		
		// Le joueur a déjà un ban en cours
		if (user.isBanIp(last.get())) {
			ESMessages.BANIP_PLAYER_ERROR_NOEMPTY.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return false;
		}
		
		// Aucune raison
		if (reason.isEmpty()) {
			ESMessages.BANIP_PLAYER_ERROR_REASON.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return false;
		}

		long creation = System.currentTimeMillis();
		// Ban définitif
		if (time_string.equalsIgnoreCase(SanctionService.UNLIMITED)) {
			return this.commandUnlimitedPlayerBanIP(staff, user, last.get(), creation, reason);
		}
		
		Optional<Long> time = UtilsDate.parseDuration(creation, time_string, true);
		
		// Temps incorrect
		if (!time.isPresent()) {
			EAMessages.IS_NOT_TIME.sender()
				.prefix(ESMessages.PREFIX)
				.replace("<time>", time_string)
				.sendTo(staff);
			return false;
		}
		
		// Ban tempotaire
		return this.commandTempPlayerBanIP(staff, user, last.get(), creation, time.get(), reason);
	}
	
	private boolean commandUnlimitedPlayerBanIP(final CommandSource staff, final EUser user, final InetAddress address, final long creation, final String reason) {
		Map<String, EReplace<?>> replaces = new HashMap<String, EReplace<?>>();
		replaces.put("<player>", EReplace.of(user.getName()));
		replaces.put("<staff>", EReplace.of(staff.getName()));
		replaces.put("<reason>", EReplace.of(reason));
		replaces.put("<address>", EReplace.of(() -> UtilsNetwork.getHostString(address)));
		
		// Ban annulé
		if (!user.banIp(address, creation, Optional.empty(), EChat.of(reason), staff)) {
			ESMessages.BANIP_PLAYER_ERROR_CANCEL.sender()
				.replaceString(replaces)
				.sendTo(staff);
			return false;
		}
		
		ESMessages.BANIP_PLAYER_UNLIMITED_STAFF.sender()
			.replaceString(replaces)
			.sendTo(staff);

		InetSocketAddress socket = UtilsNetwork.getSocketAddress(address);
		this.plugin.getEServer().getOnlineEPlayers().stream()
			.filter(player -> UtilsNetwork.equals(player.getConnection().getAddress(), socket))
			.forEach(player -> player.kick(ESMessages.BANIP_PLAYER_UNLIMITED_PLAYER.getFormat().toText2(replaces)));
		
		return true;
	}
	
	private boolean commandTempPlayerBanIP(final CommandSource staff, final EUser user, final InetAddress address, final long creation, final long expiration, final String reason) {
		Map<String, EReplace<?>> replaces = new HashMap<String, EReplace<?>>();
		replaces.put("<player>", EReplace.of(user.getName()));
		replaces.put("<staff>", EReplace.of(staff.getName()));
		replaces.put("<reason>", EReplace.of(reason));
		replaces.put("<address>", EReplace.of(() -> UtilsNetwork.getHostString(address)));
		replaces.put("<duration>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(creation, expiration)));
		replaces.put("<time>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration)));
		replaces.put("<date>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration)));
		replaces.put("<datetime>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration)));
		
		if (!user.banIp(address, creation, Optional.of(expiration), EChat.of(reason), staff)) {
			ESMessages.BANIP_PLAYER_ERROR_CANCEL.sender()
				.replaceString(replaces)
				.sendTo(staff);
			return false;
		}
		
		ESMessages.BANIP_PLAYER_TEMP_STAFF.sender()
			.replaceString(replaces)
			.sendTo(staff);
		
		InetSocketAddress socket = UtilsNetwork.getSocketAddress(address);
		this.plugin.getEServer().getOnlineEPlayers().stream()
			.filter(player -> UtilsNetwork.equals(player.getConnection().getAddress(), socket))
			.forEach(player -> player.kick(ESMessages.BANIP_PLAYER_TEMP_PLAYER.getFormat().toText2(replaces)));
		
		return true;
	}
}
