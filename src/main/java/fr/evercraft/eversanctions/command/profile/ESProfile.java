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
package fr.evercraft.eversanctions.command.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import fr.evercraft.everapi.EAMessage.EAMessages;
import fr.evercraft.everapi.message.format.EFormat;
import fr.evercraft.everapi.message.replace.EReplace;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.command.ECommand;
import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everapi.server.user.EUser;
import fr.evercraft.everapi.services.sanction.Sanction;
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;
import fr.evercraft.everapi.services.sanction.manual.SanctionManual;
import fr.evercraft.everapi.sponge.UtilsNetwork;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;

public class ESProfile extends ECommand<EverSanctions> {
	
	private static final Comparator<Sanction> COMPARATOR = 
			(Sanction o1, Sanction o2) -> {
				if (o1.isExpire() && !o2.isExpire()) return 1;
				if (!o1.isExpire() && o2.isExpire()) return -1;
				return o2.getCreationDate().compareTo(o1.getCreationDate());
			};
			
	public enum Type {
		BAN_PROFILE(Sanction.SanctionBanProfile.class),
		BAN_IP(Sanction.SanctionBanIp.class),
		BAN_PROFILE_AND_IP(SanctionAuto.SanctionBanProfileAndIp.class),
		MUTE(Sanction.SanctionMute.class),
		JAIL(Sanction.SanctionJail.class),
		JAIL_AND_MUTE(SanctionAuto.SanctionMuteAndJail.class);
		
		private Class<?> type;
		
		Type(final Class<?> type) {
			this.type = type;
		}
		
		public boolean predicate(Class<?> o) {
			return this.type.isAssignableFrom(o);
		}
	}
	
	public ESProfile(final EverSanctions plugin) {
        super(plugin, "profile");
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.PROFILE.get());
	} 

	@Override
	public Text description(final CommandSource source) {
		return ESMessages.PROFILE_DESCRIPTION.getText();
	}

	@Override
	public Text help(final CommandSource source) {
		return Text.builder("/" + this.getName() + " [" + EAMessages.ARGS_PLAYER.getString() + "|" + EAMessages.ARGS_IP.getString() + "] [" + EAMessages.ARGS_TYPE.getString() + "]")
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public Collection<String> tabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		List<String> suggests = new ArrayList<String>();
		if (args.size() == 1){
			suggests.addAll(this.getAllUsers(source));
		} else if (args.size() == 2) {
			for(Type type : Type.values()) {
				suggests.add(type.name());
			}
		}
		return suggests;
	}
	
	@Override
	public boolean execute(final CommandSource source, final List<String> args) throws CommandException {
		// Résultat de la commande :
		boolean resultat = false;
		
		// Nombre d'argument correct
		if (args.isEmpty()) {
			// Si la source est un joueur
			if (source instanceof EPlayer) {
				resultat = this.commandProfile(source, (EPlayer) source, Optional.empty());
			// La source n'est pas un joueur
			} else {
				EAMessages.COMMAND_ERROR_FOR_PLAYER.sender()
					.prefix(ESMessages.PREFIX)
					.sendTo(source);
			}
		} else if (args.size() == 1) {
			Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(args.get(0));
			// Le joueur existe
			if (user.isPresent()) {
				resultat = this.commandProfile(source, user.get(), Optional.empty());
			// Le joueur est introuvable
			} else {
				EAMessages.PLAYER_NOT_FOUND.sender()
					.prefix(ESMessages.PREFIX)
					.sendTo(source);
			}
		} else if (args.size() == 2) {
			Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(args.get(0));
			// Le joueur existe
			if (user.isPresent()) {
				try {
					Optional<Type> type = Optional.ofNullable(Type.valueOf(args.get(1).toUpperCase()));
					resultat = this.commandProfile(source, user.get(), type);
				} catch (IllegalArgumentException e) {
					ESMessages.PROFILE_ERROR_TYPE.sender()
						.replace("<type>", args.get(1))
						.sendTo(source);
				}
			// Le joueur est introuvable
			} else {
				EAMessages.PLAYER_NOT_FOUND.sender()
					.prefix(ESMessages.PREFIX)
					.sendTo(source);
			}
		} else {
			source.sendMessage(this.help(source));
		}
		return resultat;
	}
	
	private boolean commandProfile(final CommandSource staff, EUser user, final Optional<Type> type) {
		TreeSet<Sanction> valid = new TreeSet<Sanction>(ESProfile.COMPARATOR);

		if (type.isPresent()) {
			user.getAllSanctions().stream()
					.filter(sanction -> type.get().predicate(sanction.getClass()))
					.forEach(sanction -> valid.add(sanction));
		} else {
			valid.addAll(user.getAllSanctions());
		}
		
		List<Text> list = new ArrayList<Text>();
		valid.forEach(sanction -> {
			Text line_reason = null; 
			Text line_staff = null; 
			Text line_creation = null; 
			Text line_expiration = null; 
			Text line_ip = null; 
			Text line_jail = null; 
			Text line_pardon_staff = null; 
			Text line_pardon_reason = null; 
			Text line_pardon_date = null;
			
			line_staff = ESMessages.PROFILE_LINE_STAFF.getFormat().toText("<staff>", () -> sanction.getSourceName(this.plugin.getEServer()));
			line_reason = ESMessages.PROFILE_LINE_REASON.getFormat().toText("<reason>", EChat.serialize(sanction.getReason()));
			line_creation = ESMessages.PROFILE_LINE_CREATION.getFormat().toText(
								"<time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()),
								"<date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()),
								"<datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()));
			
			// Pardon
			if (sanction.isPardon()) {
				line_pardon_staff = ESMessages.PROFILE_LINE_PARDON_STAFF.getFormat().toText("<staff>", sanction.getPardonSourceName(this.plugin.getEServer()).get());
				line_pardon_reason = ESMessages.PROFILE_LINE_PARDON_REASON.getFormat().toText("<reason>", EChat.serialize(sanction.getPardonReason().get()));
				line_pardon_date = ESMessages.PROFILE_LINE_PARDON_DATE.getFormat().toText(
								"<time>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getPardonDate().get()),
								"<date>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getPardonDate().get()),
								"<datetime>", () -> this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getPardonDate().get()));
			}
			
			// BanIp
			if (sanction instanceof Sanction.SanctionBanIp) {
				line_ip = ESMessages.PROFILE_LINE_IP.getFormat().toText("<address>", () -> UtilsNetwork.getHostString(((Sanction.SanctionBanIp) sanction).getAddress()));
			}
			
			// Jail
			if (sanction instanceof Sanction.SanctionJail) {
				line_jail = ESMessages.PROFILE_LINE_JAIL.getFormat().toText("<jail>", ((Sanction.SanctionJail) sanction).getJailName());
			}
			
			// Expiration
			if (!sanction.isIndefinite()) {
				EFormat expiration = sanction.isIndefinite() ? ESMessages.PROFILE_LINE_EXPIRATION_UNLIMITED.getFormat() : ESMessages.PROFILE_LINE_EXPIRATION_TEMP.getFormat();
				line_expiration = expiration.toText(
						"<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(sanction.getCreationDate(), sanction.getExpirationDate().get()),
						"<time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getExpirationDate().get()),
						"<date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getExpirationDate().get()),
						"<datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getExpirationDate().get()));
			}
			
			EFormat message = null;
			Map<String, EReplace<?>> replaces = new HashMap<String, EReplace<?>>();
			// Manual
			if (sanction instanceof SanctionManual) {
				SanctionManual manual = (SanctionManual) sanction;
				
				if (sanction.isPardon()) {
					message = ESMessages.PROFILE_LINE_PARDON_MANUAL.getFormat();
				} else if(sanction.isExpire()) {
					message = ESMessages.PROFILE_LINE_DISABLE_MANUAL.getFormat();
				} else {
					message = ESMessages.PROFILE_LINE_ENABLE_MANUAL.getFormat();
				}
				
				replaces.put("<type>", EReplace.of(ESProfile.getType(manual)));
			// Auto
			} else if (sanction instanceof SanctionAuto) {
				SanctionAuto auto = (SanctionAuto) sanction;
				
				if (sanction.isPardon()) {
					message = ESMessages.PROFILE_LINE_PARDON_AUTO.getFormat();
				} else if(sanction.isExpire()) {
					message = ESMessages.PROFILE_LINE_DISABLE_AUTO.getFormat();
				} else {
					message = ESMessages.PROFILE_LINE_ENABLE_AUTO.getFormat();
				}
				
				
				replaces.put("<reason>", EReplace.of(ESProfile.getType(auto)));
				replaces.put("<level>", EReplace.of(String.valueOf(auto.getLevelNumber())));
			}
			
			replaces.put("<line_reason>", this.get(line_reason));
			replaces.put("<line_staff>", this.get(line_staff));
			replaces.put("<line_creation>", this.get(line_creation));
			replaces.put("<line_expiration>", this.get(line_expiration));
			replaces.put("<line_ip>", this.get(line_ip));
			replaces.put("<line_jail>", this.get(line_jail));
			replaces.put("<line_pardon_staff>", this.get(line_pardon_staff));
			replaces.put("<line_pardon_reason>", this.get(line_pardon_reason));
			replaces.put("<line_pardon_date>", this.get(line_pardon_date));
					
			if (message != null) {
				list.add(message.toText(replaces));
			}
		});
		
		if (list.isEmpty()) {
			list.add(ESMessages.PROFILE_EMPTY.getText());
		}
		
		EFormat title;
		Map<String, EReplace<?>> replaces = new HashMap<String, EReplace<?>>();
		replaces.put("<player>", EReplace.of(user.getName()));
		if (type.isPresent()) {
			if(user.getIdentifier().equals(staff.getIdentifier())) {
				title = ESMessages.PROFILE_TITLE_EQUALS_TYPE.getFormat();
			} else {
				title = ESMessages.PROFILE_TITLE_OTHERS_TYPE.getFormat();
			}
			replaces.put("<type>", EReplace.of(type.get().name()));
		} else {
			if(user.getIdentifier().equals(staff.getIdentifier())) {
				title = ESMessages.PROFILE_TITLE_EQUALS.getFormat();
			} else {
				title = ESMessages.PROFILE_TITLE_OTHERS.getFormat();
			}
		}
		
		this.plugin.getEverAPI().getManagerService().getEPagination().sendTo(
				title.toText(replaces).toBuilder()
					.onClick(TextActions.runCommand("/profile \"" + user.getName() + "\""))
					.build(), 
				list, staff);
		return true;
	}
	
	public EReplace<?> get(Text message) {
		return (message == null) ? EReplace.of("") : EReplace.of(message);
	}
	
	public static Text getType(SanctionAuto sanction) {
		if(sanction instanceof SanctionAuto.SanctionBanProfileAndIp) {
			return ESMessages.PROFILE_AUTO_BAN_PROFILE_AND_IP.getText();
		} else if(sanction instanceof SanctionAuto.SanctionBanProfile) {
			return ESMessages.PROFILE_AUTO_BAN_PROFILE.getText();
		} else if(sanction instanceof SanctionAuto.SanctionBanIp) {
			return ESMessages.PROFILE_AUTO_BAN_IP.getText();
		} else if(sanction instanceof SanctionAuto.SanctionMuteAndJail) {
			return ESMessages.PROFILE_AUTO_MUTE_AND_JAIL.getText();
		} else if(sanction instanceof SanctionAuto.SanctionMute) {
			return ESMessages.PROFILE_AUTO_MUTE.getText();
		} else if(sanction instanceof SanctionAuto.SanctionJail) {
			return ESMessages.PROFILE_AUTO_JAIL.getText();
		}
		return Text.EMPTY;
	}
	
	public static Text getType(SanctionManual sanction) {
		if(sanction instanceof Sanction.SanctionBanProfile) {
			return ESMessages.PROFILE_MANUAL_BAN_PROFILE.getText();
		} else if(sanction instanceof Sanction.SanctionBanIp) {
			return ESMessages.PROFILE_MANUAL_BAN_IP.getText();
		} else if(sanction instanceof Sanction.SanctionMute) {
			return ESMessages.PROFILE_MANUAL_MUTE.getText();
		} else if(sanction instanceof Sanction.SanctionJail) {
			return ESMessages.PROFILE_MANUAL_JAIL.getText();
		}
		return Text.EMPTY;
	}
}
