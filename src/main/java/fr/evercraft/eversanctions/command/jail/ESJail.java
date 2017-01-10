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
package fr.evercraft.eversanctions.command.jail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import fr.evercraft.everapi.EAMessage.EAMessages;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.command.ECommand;
import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everapi.server.user.EUser;
import fr.evercraft.everapi.services.jail.Jail;
import fr.evercraft.everapi.services.sanction.SanctionService;
import fr.evercraft.everapi.services.sanction.manual.SanctionManualProfile;
import fr.evercraft.everapi.sponge.UtilsDate;
import fr.evercraft.everapi.text.ETextBuilder;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.EJail;

public class ESJail extends ECommand<EverSanctions> {
	
	public ESJail(final EverSanctions plugin) {
        super(plugin, "jail");
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.JAIL.get());
	} 

	@Override
	public Text description(final CommandSource source) {
		return ESMessages.JAIL_DESCRIPTION.getText();
	}

	@Override
	public Text help(final CommandSource source) {
		return Text.builder("/" + this.getName() + " <" + EAMessages.ARGS_PLAYER.getString() + "> <" + EAMessages.ARGS_JAIL.getString() + "> "
				+ "<" + EAMessages.ARGS_TIME.getString() + "> <" + EAMessages.ARGS_REASON.getString() + ">")
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public List<String> tabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		List<String> suggests = new ArrayList<String>(); 
		if (args.size() == 1) {
			suggests.addAll(this.getAllUsers(source));
		} else if (args.size() == 2) {
			this.plugin.getJailService().getAll().forEach(jail -> suggests.add(jail.getName()));
			if (suggests.isEmpty()) {
				source.sendMessage(ESMessages.PREFIX.getText().concat(ESMessages.JAIL_EMPTY.getText()));
			}
		} else if (args.size() == 3) {
			suggests.add(SanctionService.UNLIMITED);
			suggests.add("\"1mo 7d 12h\"");
			suggests.add("1h");
		} else if (args.size() == 4) {
			suggests.add("reason...");
		}
		return suggests;
	}
	
	@Override
	protected List<String> getArg(final String arg) {
		List<String> args = super.getArg(arg);
		// Le message est transformer en un seul argument
		if (args.size() > 4) {
			List<String> args_send = new ArrayList<String>();
			args_send.add(args.get(0));
			args_send.add(args.get(1));
			args_send.add(args.get(2));
			args_send.add(Pattern.compile("^[ \"]*" + args.get(0) + "[ \"]*" + args.get(1) + "[ \"]*" + args.get(2) + "[ \"][ ]*").matcher(arg).replaceAll(""));
			return args_send;
		}
		return args;
	}
	
	@Override
	public boolean execute(final CommandSource source, final List<String> args) throws CommandException {
		// Résultat de la commande :
		boolean resultat = false;
		
		// Nombre d'argument correct
		if (args.size() == 4) {
			
			Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(args.get(0));
			// Le joueur existe
			if (user.isPresent()){
				resultat = this.commandJail(source, user.get(), args.get(1), args.get(2), args.get(3));
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
	
	private boolean commandJail(final CommandSource staff, EUser user, final String jail_string, final String time_string, final String reason) {
		// Le staff et le joueur sont identique
		if (staff.getIdentifier().equals(user.getIdentifier())) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.JAIL_ERROR_EQUALS.get()
				.replaceAll("<player>", user.getName())));
			return false;
		}
		
		// Le joueur a déjà un sanction jail en cours
		if (user.getManual(SanctionManualProfile.Type.JAIL).isPresent()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.JAIL_ERROR_NOEMPTY.get()
				.replaceAll("<player>", user.getName())));
			return false;
		}
		
		Optional<Jail> jail = this.plugin.getJailService().get(jail_string);
		// Aucune prison avec ce nom
		if (!jail.isPresent()) {
			staff.sendMessage(ESMessages.PREFIX.getText().concat(ESMessages.JAIL_UNKNOWN.getText()));
			return false;
		}
		
		// Aucune raison
		if (reason.isEmpty()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.JAIL_ERROR_REASON.get()
						.replaceAll("<player>", user.getName())));
			return false;
		}
		
		long creation = System.currentTimeMillis();
			
		// Jail définitif
		if (time_string.equalsIgnoreCase(SanctionService.UNLIMITED)) {
			return this.commandUnlimitedJail(staff, user, jail.get(), creation, reason);
		}
		
		Optional<Long> time = UtilsDate.parseDuration(creation, time_string, true);
		
		// Temps incorrect
		if (!time.isPresent()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + EAMessages.IS_NOT_TIME.get()
				.replaceAll("<time>", time_string)));
			return false;
		}
		
		// Ban tempotaire
		return this.commandTempJail(staff, user, jail.get(), creation, time.get(), reason);
	}
	
	private boolean commandUnlimitedJail(final CommandSource staff, final EUser user, final Jail jail, final long creation, final String reason) {
		// Jail annulé
		if (!user.jail(jail, creation, Optional.empty(), EChat.of(reason), staff)) {
			staff.sendMessage(ETextBuilder.toBuilder(ESMessages.PREFIX.get())
					.append(ESMessages.JAIL_ERROR_CANCEL_UNLIMITED.get()
							.replaceAll("<player>", user.getName()))
					.replace("<jail>", ESJail.getButtonJail(jail))
					.build());
			return false;
		}
		
		staff.sendMessage(ETextBuilder.toBuilder(ESMessages.PREFIX.get())
				.append(ESMessages.JAIL_UNLIMITED_STAFF.get()
						.replaceAll("<player>", user.getName())
						.replaceAll("<reason>", reason))
				.replace("<jail>", ESJail.getButtonJail(jail))
				.build());
		
		if(user instanceof EPlayer) {
			EPlayer player = (EPlayer) user;
			player.teleport(jail.getTransform(), true);
			player.sendMessage(ETextBuilder.toBuilder(ESMessages.PREFIX.get())
					.append(ESMessages.JAIL_UNLIMITED_PLAYER.get()
							.replaceAll("<staff>", staff.getName())
							.replaceAll("<reason>", reason))
					.replace("<jail>", ESJail.getButtonJail(jail))
					.build());
		}
		return true;
	}
	
	private boolean commandTempJail(final CommandSource staff, final EUser user, final Jail jail, final long creation, final long expiration, final String reason) {
		if (!user.jail(jail, creation, Optional.of(expiration), EChat.of(reason), staff)) {
			staff.sendMessage(ETextBuilder.toBuilder(ESMessages.PREFIX.get())
					.append(ESMessages.JAIL_ERROR_CANCEL_TEMP.get()
							.replaceAll("<player>", user.getName()))
					.replace("<jail>", ESJail.getButtonJail(jail))
					.build());
			return false;
		}
		
		staff.sendMessage(ETextBuilder.toBuilder(ESMessages.PREFIX.get())
				.append(ESMessages.JAIL_TEMP_STAFF.get()
						.replaceAll("<player>", user.getName())
						.replaceAll("<reason>", reason)
						.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(creation, expiration))
						.replaceAll("<time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration))
						.replaceAll("<date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration))
						.replaceAll("<datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration)))
				.replace("<jail>", ESJail.getButtonJail(jail))
				.build());
		
		if(user instanceof EPlayer) {
			EPlayer player = (EPlayer) user;
			player.teleport(jail.getTransform(), true);
			player.sendMessage(ETextBuilder.toBuilder(ESMessages.PREFIX.get())
					.append(ESMessages.JAIL_TEMP_PLAYER.get()
							.replaceAll("<staff>", staff.getName())
							.replaceAll("<reason>", reason)
							.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(creation, expiration))
							.replaceAll("<time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration))
							.replaceAll("<date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration))
							.replaceAll("<datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration)))
					.replace("<jail>", ESJail.getButtonJail(jail))
					.build());
		}
		return true;
	}
	
	public static Text getButtonJail(final Jail jail) {
		Location<World> location = jail.getTransform().getLocation();
		return EChat.of(ESMessages.JAIL_NAME.get().replaceAll("<name>", jail.getName())).toBuilder()
					.onHover(TextActions.showText(EChat.of(ESMessages.JAIL_NAME_HOVER.get()
							.replaceAll("<jail>", jail.getName())
							.replaceAll("<radius>", String.valueOf(jail.getRadius()))
							.replaceAll("<world>", location.getExtent().getName())
							.replaceAll("<x>", String.valueOf(location.getBlockX()))
							.replaceAll("<y>", String.valueOf(location.getBlockY()))
							.replaceAll("<z>", String.valueOf(location.getBlockZ())))))
					.build();
	}
	
	public static Text getButtonJail(final EJail jail) {
		return EChat.of(ESMessages.JAIL_NAME.get().replaceAll("<name>", jail.getName())).toBuilder()
					.onHover(TextActions.showText(EChat.of(ESMessages.JAIL_NAME_HOVER.get()
							.replaceAll("<jail>", jail.getName())
							.replaceAll("<radius>", String.valueOf(jail.getRadius()))
							.replaceAll("<world>", jail.getLocationSQL().getWorldName())
							.replaceAll("<x>", jail.getLocationSQL().getX().toString())
							.replaceAll("<y>", jail.getLocationSQL().getY().toString())
							.replaceAll("<z>", jail.getLocationSQL().getZ().toString()))))
					.build();
	}
}
