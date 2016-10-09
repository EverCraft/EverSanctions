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
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import com.google.common.net.InetAddresses;

import fr.evercraft.everapi.EAMessage.EAMessages;
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
		return Text.builder("/" + this.getName() + " <" + EAMessages.ARGS_PLAYER.get() + "|" + EAMessages.ARGS_IP.get() + "> "
				+ "<" + EAMessages.ARGS_TIME.get() + "> <" + EAMessages.ARGS_REASON.get() + ">")
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
			suggests.add(SanctionService.UNLIMITED);
			suggests.add("\"1mo 7d 12h\"");
			suggests.add("1h");
		} else if (args.size() == 3) {
			suggests.add("reason...");
		}
		return suggests;
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
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_IP_ERROR_EQUALS.get()
				.replaceAll("<address>", subject.getIdentifier())));
			return false;
		}
		
		// Le joueur a déjà un ban en cours
		if (this.plugin.getSanctionService().isBanned(subject.getAddress())) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_IP_ERROR_NOEMPTY.get()
				.replaceAll("<address>", subject.getIdentifier())));
			return false;
		}
		
		// Aucune raison
		if (reason.isEmpty()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_IP_ERROR_REASON.get()
				.replaceAll("<address>", subject.getIdentifier())));
			return false;
		}
		
		long creation = System.currentTimeMillis();
			
		// Ban définitif
		if (time_string.equalsIgnoreCase(SanctionService.UNLIMITED)) {
			return this.commandUnlimitedBanIP(staff, subject, creation, reason);
		}
		
		Optional<Long> time = UtilsDate.parseDateDiff(creation, time_string, true);
		
		// Temps incorrect
		if (!time.isPresent()) {
			staff.sendMessage(this.help(staff));
			return false;
		}
		
		// Ban tempotaire
		return this.commandTempBanIP(staff, subject, creation, time.get(), reason);
	}
	
	private boolean commandUnlimitedBanIP(final CommandSource staff, final SanctionIpSubject subject, final long creation, final String reason) {
		// Ban annulé
		if (!subject.ban(creation, Optional.empty(), EChat.of(reason), staff)) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_IP_ERROR_CANCEL.get()
				.replaceAll("<address>", subject.getIdentifier())));
			return false;
		}
		
		staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_IP_UNLIMITED_STAFF.get()
			 .replaceAll("<reason>", reason)
			 .replaceAll("<address>", subject.getIdentifier())));
		
		this.plugin.getEServer().getOnlineEPlayers().stream()
			.filter(player -> UtilsNetwork.equals(player.getConnection().getAddress(), subject.getSocketAddress()))
			.forEach(player ->
				player.kick(EChat.of(ESMessages.BANIP_IP_UNLIMITED_PLAYER.get()
						.replaceAll("<staff>", staff.getIdentifier())
						.replaceAll("<reason>", reason)))
			);
		return true;
	}
	
	private boolean commandTempBanIP(final CommandSource staff, final SanctionIpSubject subject, final long creation, final long expiration, final String reason) {
		if (!subject.ban(creation, Optional.of(expiration), EChat.of(reason), staff)) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_IP_ERROR_CANCEL.get()
				.replaceAll("<address>", subject.getIdentifier())));
			return false;
		}
		
		staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_IP_TEMP_STAFF.get()
			 .replaceAll("<reason>", reason)
			 .replaceAll("<address>", subject.getIdentifier())
			 .replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(creation, expiration))
			 .replaceAll("<time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration))
			 .replaceAll("<date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration))
			 .replaceAll("<datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration))));
		
		this.plugin.getEServer().getOnlineEPlayers().stream()
			.filter(player -> UtilsNetwork.equals(player.getConnection().getAddress(), subject.getSocketAddress()))
			.forEach(player ->
				player.kick(EChat.of(ESMessages.BANIP_IP_TEMP_PLAYER.get()
						.replaceAll("<staff>", staff.getIdentifier())
						.replaceAll("<address>", subject.getIdentifier())
						.replaceAll("<reason>", reason)
						.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(creation, expiration))
						.replaceAll("<time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration))
						.replaceAll("<date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration))
						.replaceAll("<datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration))))
			);
		return true;
	}
	
	/*
	 * Player
	 */
	
	private boolean commandBanIP(final CommandSource staff, EUser user, final String time_string, final String reason) {		
		Optional<InetAddress> last = user.getLastIP();
		// Aucune adresse IP
		if (!last.isPresent()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_PLAYER_ERROR_IP.get()
			.replaceAll("<player>", user.getName())));
			return false;
		}
		
		Optional<SanctionIpSubject> subject = this.plugin.getSanctionService().get(last.get());
		// Impossible de récupérer le subject IP
		if (!subject.isPresent()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + EAMessages.COMMAND_ERROR.get()));
			return false;
		}
		
		// Le staff et le joueur sont identique
		if (staff instanceof EPlayer && UtilsNetwork.equals(((EPlayer) staff).getConnection().getAddress(), UtilsNetwork.getSocketAddress(last.get()))) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_PLAYER_ERROR_EQUALS.get()
				.replaceAll("<player>", user.getName())));
			return false;
		}
		
		// Le joueur a déjà un ban en cours
		if (user.isBanIp(last.get())) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_PLAYER_ERROR_NOEMPTY.get()
				.replaceAll("<player>", user.getName())));
			return false;
		}
		
		// Aucune raison
		if (reason.isEmpty()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_PLAYER_ERROR_REASON.get()
						.replaceAll("<player>", user.getName())));
			return false;
		}

		long creation = System.currentTimeMillis();
		// Ban définitif
		if (time_string.equalsIgnoreCase(SanctionService.UNLIMITED)) {
			return this.commandUnlimitedPlayerBanIP(staff, user, last.get(), creation, reason);
		}
		
		Optional<Long> time = UtilsDate.parseDateDiff(creation, time_string, true);
		
		// Temps incorrect
		if (!time.isPresent()) {
			staff.sendMessage(this.help(staff));
			return false;
		}
		
		// Ban tempotaire
		return this.commandTempPlayerBanIP(staff, user, last.get(), creation, time.get(), reason);
	}
	
	private boolean commandUnlimitedPlayerBanIP(final CommandSource staff, final EUser user, final InetAddress address, final long creation, final String reason) {
		// Ban annulé
		if (!user.banIp(address, creation, Optional.empty(), EChat.of(reason), staff)) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_PLAYER_ERROR_CANCEL.get()
					.replaceAll("<player>", user.getName())
					.replaceAll("<address>", UtilsNetwork.getHostString(address))));
			return false;
		}
		
		staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_PLAYER_UNLIMITED_STAFF.get()
				.replaceAll("<reason>", reason)
				.replaceAll("<player>", user.getName())
				.replaceAll("<address>", UtilsNetwork.getHostString(address))));

		InetSocketAddress socket = UtilsNetwork.getSocketAddress(address);
		this.plugin.getEServer().getOnlineEPlayers().stream()
		.filter(player -> UtilsNetwork.equals(player.getConnection().getAddress(), socket))
		.forEach(player ->
			player.kick(EChat.of(ESMessages.BANIP_PLAYER_UNLIMITED_PLAYER.get()
					.replaceAll("<staff>", staff.getName())
					.replaceAll("<reason>", reason)
					.replaceAll("<address>", UtilsNetwork.getHostString(address))))
		);
		
		return true;
	}
	
	private boolean commandTempPlayerBanIP(final CommandSource staff, final EUser user, final InetAddress address, final long creation, final long expiration, final String reason) {
		if (!user.banIp(address, creation, Optional.of(expiration), EChat.of(reason), staff)) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_PLAYER_ERROR_CANCEL.get()
						.replaceAll("<player>", user.getName())
						.replaceAll("<address>", UtilsNetwork.getHostString(address))));
			return false;
		}
		
		staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BANIP_PLAYER_TEMP_STAFF.get()
			 .replaceAll("<player>", user.getName())
			 .replaceAll("<address>", UtilsNetwork.getHostString(address))
			 .replaceAll("<reason>", reason)
			 .replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(creation, expiration))
			 .replaceAll("<time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration))
			 .replaceAll("<date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration))
			 .replaceAll("<datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration))));
		

		InetSocketAddress socket = UtilsNetwork.getSocketAddress(address);
		this.plugin.getEServer().getOnlineEPlayers().stream()
		.filter(player -> UtilsNetwork.equals(player.getConnection().getAddress(), socket))
		.forEach(player ->
			player.kick(EChat.of(ESMessages.BANIP_PLAYER_TEMP_PLAYER.get()
					.replaceAll("<staff>", staff.getName())
					.replaceAll("<address>", UtilsNetwork.getHostString(address))
					.replaceAll("<reason>", reason)
					.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(creation, expiration))
					.replaceAll("<time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration))
					.replaceAll("<date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration))
					.replaceAll("<datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration))))
		);
		return true;
	}
}
