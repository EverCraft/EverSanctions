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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import fr.evercraft.everapi.EAMessage.EAMessages;
import fr.evercraft.everapi.message.replace.EReplace;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.command.ECommand;
import fr.evercraft.everapi.server.location.VirtualTransform;
import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everapi.server.user.EUser;
import fr.evercraft.everapi.services.jail.Jail;
import fr.evercraft.everapi.services.sanction.SanctionService;
import fr.evercraft.everapi.services.sanction.manual.SanctionManualProfile;
import fr.evercraft.everapi.sponge.UtilsDate;
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
		return Text.builder("/" + this.getName() + " <" + EAMessages.ARGS_USER.getString() + "> <" + EAMessages.ARGS_JAIL.getString() + "> "
				+ "<" + EAMessages.ARGS_TIME.getString() + "> <" + EAMessages.ARGS_REASON.getString() + ">")
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public Collection<String> tabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		if (args.size() == 1) {
			return this.getAllUsers(args.get(0));
		} else if (args.size() == 2) {
			List<String> suggests = new ArrayList<String>(); 
			this.plugin.getJailService().getAll().forEach(jail -> suggests.add(jail.getName()));
			if (suggests.isEmpty()) {
				source.sendMessage(ESMessages.PREFIX.getText().concat(ESMessages.JAIL_EMPTY.getText()));
			}
			return suggests;
		} else if (args.size() == 3) {
			return Arrays.asList(SanctionService.UNLIMITED, "\"1mo 7d 12h\"", "1h");
		} else if (args.size() == 4) {
			return Arrays.asList("reason...");
		}
		return Arrays.asList();
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
	public CompletableFuture<Boolean> execute(final CommandSource source, final List<String> args) throws CommandException {
		// Nombre d'argument correct
		if (args.size() == 4) {
			
			Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(args.get(0));
			// Le joueur existe
			if (user.isPresent()){
				return this.commandJail(source, user.get(), args.get(1), args.get(2), args.get(3));
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
		return CompletableFuture.completedFuture(false);
	}
	
	private CompletableFuture<Boolean> commandJail(final CommandSource staff, EUser user, final String jail_string, final String time_string, final String reason) {
		// Le staff et le joueur sont identique
		if (staff.getIdentifier().equals(user.getIdentifier())) {
			ESMessages.JAIL_ERROR_EQUALS.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		// Le joueur a déjà un sanction jail en cours
		if (user.getManual(SanctionManualProfile.Type.JAIL).isPresent()) {
			ESMessages.JAIL_ERROR_NOEMPTY.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		Optional<Jail> jail = this.plugin.getJailService().get(jail_string);
		// Aucune prison avec ce nom
		if (!jail.isPresent()) {
			ESMessages.JAIL_UNKNOWN.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		// Aucune raison
		if (reason.isEmpty()) {
			ESMessages.JAIL_ERROR_REASON.sender()
				.replace("<player>", user.getName())
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		long creation = System.currentTimeMillis();
			
		// Jail définitif
		if (time_string.equalsIgnoreCase(SanctionService.UNLIMITED)) {
			return this.commandUnlimitedJail(staff, user, jail.get(), creation, reason);
		}
		
		Optional<Long> time = UtilsDate.parseDuration(creation, time_string, true);
		
		// Temps incorrect
		if (!time.isPresent()) {
			EAMessages.IS_NOT_TIME.sender()
				.prefix(ESMessages.PREFIX)
				.replace("<time>", time_string)
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		// Ban tempotaire
		return this.commandTempJail(staff, user, jail.get(), creation, time.get(), reason);
	}
	
	private CompletableFuture<Boolean> commandUnlimitedJail(final CommandSource staff, final EUser user, final Jail jail, final long creation, final String reason) {
		// Jail annulé
		if (!user.jail(jail, creation, Optional.empty(), EChat.of(reason), staff)) {
			ESMessages.JAIL_ERROR_CANCEL_UNLIMITED.sender()
				.replace("<player>", user.getName())
				.replace("<jail>", () -> ESJail.getButtonJail(jail))
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		ESMessages.JAIL_UNLIMITED_STAFF.sender()
			.replace("<player>", user.getName())
			.replace("<reason>", reason)
			.replace("<jail>", () -> ESJail.getButtonJail(jail))
			.sendTo(staff);
		
		if(user instanceof EPlayer) {
			EPlayer player = (EPlayer) user;
			player.teleport(jail.getTransform(), true);
			ESMessages.JAIL_UNLIMITED_PLAYER.sender()
				.replace("<staff>", staff.getName())
				.replace("<reason>", reason)
				.replace("<jail>", () -> ESJail.getButtonJail(jail))
				.sendTo(player);
		}
		return CompletableFuture.completedFuture(true);
	}
	
	private CompletableFuture<Boolean> commandTempJail(final CommandSource staff, final EUser user, final Jail jail, final long creation, final long expiration, final String reason) {
		Map<String, EReplace<?>> replaces = new HashMap<String, EReplace<?>>();
		replaces.put("<player>", EReplace.of(user.getName()));
		replaces.put("<staff>", EReplace.of(staff.getName()));
		replaces.put("<reason>", EReplace.of(reason));
		replaces.put("<jail>", EReplace.of(() -> ESJail.getButtonJail(jail)));
		replaces.put("<duration>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(creation, expiration)));
		replaces.put("<time>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(expiration)));
		replaces.put("<date>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(expiration)));
		replaces.put("<datetime>", EReplace.of(() -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(expiration)));
		
		if (!user.jail(jail, creation, Optional.of(expiration), EChat.of(reason), staff)) {
			ESMessages.JAIL_ERROR_CANCEL_TEMP.sender()
				.replaceString(replaces)
				.sendTo(staff);
			return CompletableFuture.completedFuture(false);
		}
		
		ESMessages.JAIL_TEMP_STAFF.sender()
			.replaceString(replaces)
			.sendTo(staff);
		
		if(user instanceof EPlayer) {
			EPlayer player = (EPlayer) user;
			player.teleport(jail.getTransform(), true);
			ESMessages.JAIL_TEMP_PLAYER.sender()
				.replaceString(replaces)
				.sendTo(player);
		}
		return CompletableFuture.completedFuture(true);
	}
	
	public static Text getButtonJail(final Jail jail) {
		Location<World> location = jail.getTransform().getLocation();
		Map<String, EReplace<?>> replaces = new HashMap<String, EReplace<?>>();
		replaces.put("<world>", EReplace.of(location.getExtent().getName()));
		replaces.put("<x>", EReplace.of(String.valueOf(location.getBlockX())));
		replaces.put("<y>", EReplace.of(String.valueOf(location.getBlockY())));
		replaces.put("<z>", EReplace.of(String.valueOf(location.getBlockZ())));
		replaces.put("<jail>", EReplace.of(jail.getName()));
		replaces.put("<name>", EReplace.of(jail.getName()));
		replaces.put("<radius>", EReplace.of(String.valueOf(jail.getRadius())));
		
		return ESMessages.JAIL_NAME.getFormat().toText2(replaces).toBuilder()
					.onHover(TextActions.showText(ESMessages.JAIL_NAME_HOVER.getFormat().toText2(replaces)))
					.build();
	}
	
	public static Text getButtonJail(final EJail jail) {
		VirtualTransform location = jail.getVirtualTransform();
		Map<String, EReplace<?>> replaces = new HashMap<String, EReplace<?>>();
		replaces.put("<world>", EReplace.of(location.getWorldName()));
		replaces.put("<x>", EReplace.of(String.valueOf(location.getPosition().getFloorX())));
		replaces.put("<y>", EReplace.of(String.valueOf(location.getPosition().getFloorY())));
		replaces.put("<z>", EReplace.of(String.valueOf(location.getPosition().getFloorZ())));
		replaces.put("<jail>", EReplace.of(jail.getName()));
		replaces.put("<name>", EReplace.of(jail.getName()));
		replaces.put("<radius>", EReplace.of(String.valueOf(jail.getRadius())));
		
		return ESMessages.JAIL_NAME.getFormat().toText2(replaces).toBuilder()
				.onHover(TextActions.showText(ESMessages.JAIL_NAME_HOVER.getFormat().toText2(replaces)))
				.build();
	}
}
