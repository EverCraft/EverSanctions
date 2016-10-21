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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import fr.evercraft.everapi.EAMessage.EAMessages;
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
		return Text.builder("/" + this.getName() + " [" + EAMessages.ARGS_PLAYER.get() + "|" + EAMessages.ARGS_IP.get() + "] [" + EAMessages.ARGS_TYPE.get() + "]")
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
				source.sendMessage(ESMessages.PREFIX.getText().concat(EAMessages.COMMAND_ERROR_FOR_PLAYER.getText()));
			}
		} else if (args.size() == 1) {
			Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(args.get(0));
			// Le joueur existe
			if (user.isPresent()) {
				resultat = this.commandProfile(source, user.get(), Optional.empty());
			// Le joueur est introuvable
			} else {
				source.sendMessage(ESMessages.PREFIX.getText().concat(EAMessages.PLAYER_NOT_FOUND.getText()));
			}
		} else if (args.size() == 2) {
			Optional<EUser> user = this.plugin.getEServer().getOrCreateEUser(args.get(0));
			// Le joueur existe
			if (user.isPresent()) {
				try {
					Optional<Type> type = Optional.ofNullable(Type.valueOf(args.get(1)));
					resultat = this.commandProfile(source, user.get(), type);
				} catch (IllegalArgumentException e) {
					source.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.PROFILE_ERROR_TYPE.get()
							.replaceAll("<type>", args.get(1)))); 
				}
			// Le joueur est introuvable
			} else {
				source.sendMessage(ESMessages.PREFIX.getText().concat(EAMessages.PLAYER_NOT_FOUND.getText()));
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
			String line_type = ESMessages.PROFILE_LINE_TYPE.get();
			String line_reason = ESMessages.PROFILE_LINE_REASON.get();
			String line_staff = ESMessages.PROFILE_LINE_STAFF.get();
			String line_creation = ESMessages.PROFILE_LINE_CREATION.get();
			String line_expiration = sanction.isIndefinite() ? ESMessages.PROFILE_LINE_EXPIRATION_UNLIMITED.get() : ESMessages.PROFILE_LINE_EXPIRATION_TEMP.get();
			String line_ip = ESMessages.PROFILE_LINE_IP.get();
			String line_jail = ESMessages.PROFILE_LINE_JAIL.get();
			String line_pardon_staff = ESMessages.PROFILE_LINE_PARDON_STAFF.get();
			String line_pardon_reason = ESMessages.PROFILE_LINE_PARDON_REASON.get();
			String line_pardon_date = ESMessages.PROFILE_LINE_PARDON_DATE.get();
			
			line_staff = line_staff.replaceAll("<staff>", sanction.getSourceName());
			line_reason = line_reason.replaceAll("<reason>", EChat.serialize(sanction.getReason()));
			line_creation = line_creation
					.replaceAll("<time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
					.replaceAll("<date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
					.replaceAll("<datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()));
			
			// Pardon
			if (sanction.isPardon()) {
				line_pardon_staff = line_pardon_staff.replaceAll("<staff>", sanction.getPardonSourceName().get());
				line_pardon_reason = line_pardon_reason.replaceAll("<reason>", EChat.serialize(sanction.getPardonReason().get()));
				line_pardon_date = line_pardon_date
						.replaceAll("<time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getPardonDate().get()))
						.replaceAll("<date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getPardonDate().get()))
						.replaceAll("<datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getPardonDate().get()));
			}
			
			// BanIp
			if (sanction instanceof Sanction.SanctionBanIp) {
				line_ip = line_ip.replaceAll("<address>", UtilsNetwork.getHostString(((Sanction.SanctionBanIp) sanction).getAddress()));
			} else {
				line_ip = "";
			}
			
			// Jail
			if (sanction instanceof Sanction.SanctionJail) {
				line_jail = line_jail.replaceAll("<jail>", ((Sanction.SanctionJail) sanction).getJailName());
			} else {
				line_jail = "";
			}
			
			// Expiration
			if (!sanction.isIndefinite()) {
				line_expiration = line_expiration
						.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(sanction.getCreationDate(), sanction.getExpirationDate().get()))
						.replaceAll("<time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getExpirationDate().get()))
						.replaceAll("<date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getExpirationDate().get()))
						.replaceAll("<datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getExpirationDate().get()));
			}
			
			String message = "";
			// Manual
			if (sanction instanceof SanctionManual) {
				SanctionManual manual = (SanctionManual) sanction;
				
				if (sanction.isPardon()) {
					message = ESMessages.PROFILE_LINE_PARDON_MANUAL.get();
				} else if(sanction.isExpire()) {
					message = ESMessages.PROFILE_LINE_DISABLE_MANUAL.get();
				} else {
					message = ESMessages.PROFILE_LINE_ENABLE_MANUAL.get();
				}
				
				String value = this.getType(manual);
				line_type = line_type.replaceAll("<type>", value);
				message = message.replaceAll("<type>", value);
			// Auto
			} else if (sanction instanceof SanctionAuto) {
				SanctionAuto auto = (SanctionAuto) sanction;
				
				if (sanction.isPardon()) {
					message = ESMessages.PROFILE_LINE_PARDON_AUTO.get();
				} else if(sanction.isExpire()) {
					message = ESMessages.PROFILE_LINE_DISABLE_AUTO.get();
				} else {
					message = ESMessages.PROFILE_LINE_ENABLE_AUTO.get();
				}
				
				String value = this.getType(auto);
				Integer level = auto.getLevelNumber();
				line_type = line_type
						.replaceAll("<reason>", value)
						.replaceAll("<level>", level.toString());
				message = message
						.replaceAll("<reason>", value)
						.replaceAll("<level>", level.toString());
			}
			
			message = message
					.replaceAll("<line_type>", this.get(line_type))
					.replaceAll("<line_reason>", this.get(line_reason))
					.replaceAll("<line_staff>", this.get(line_staff))
					.replaceAll("<line_creation>", this.get(line_creation))
					.replaceAll("<line_expiration>", this.get(line_expiration))
					.replaceAll("<line_ip>", this.get(line_ip))
					.replaceAll("<line_jail>", this.get(line_jail))
					.replaceAll("<line_pardon_staff>", this.get(line_pardon_staff))
					.replaceAll("<line_pardon_reason>", this.get(line_pardon_reason))
					.replaceAll("<line_pardon_date>", this.get(line_pardon_date));
					
			list.addAll(EChat.of(Arrays.asList(message.split("\n"))));
		});
		
		if (list.isEmpty()) {
			list.add(ESMessages.PROFILE_EMPTY.getText());
		}
		
		String title;
		if (type.isPresent()) {
			if(user.getIdentifier().equals(staff.getIdentifier())) {
				title = ESMessages.PROFILE_TITLE_EQUALS_TYPE.get();
			} else {
				title = ESMessages.PROFILE_TITLE_OTHERS_TYPE.get();
			}
			title = title.replaceAll("<type>", type.get().name());
		} else {
			if(user.getIdentifier().equals(staff.getIdentifier())) {
				title = ESMessages.PROFILE_TITLE_EQUALS.get();
			} else {
				title = ESMessages.PROFILE_TITLE_OTHERS.get();
			}
		}
		
		this.plugin.getEverAPI().getManagerService().getEPagination().sendTo(
				EChat.of(title.replace("<player>", user.getName())).toBuilder()
					.onClick(TextActions.runCommand("/profile \"" + user.getName() + "\""))
					.build(), 
				list, staff);
		return true;
	}
	
	public String get(String message) {
		return (message.isEmpty()) ? "" : message;
	}
	
	public String getType(SanctionAuto sanction) {
		if(sanction instanceof SanctionAuto.SanctionBanProfileAndIp) {
			return ESMessages.PROFILE_AUTO_BAN_PROFILE_AND_IP.get();
		} else if(sanction instanceof SanctionAuto.SanctionBanProfile) {
			return ESMessages.PROFILE_AUTO_BAN_PROFILE.get();
		} else if(sanction instanceof SanctionAuto.SanctionBanIp) {
			return ESMessages.PROFILE_AUTO_BAN_IP.get();
		} else if(sanction instanceof SanctionAuto.SanctionMuteAndJail) {
			return ESMessages.PROFILE_AUTO_MUTE_AND_JAIL.get();
		} else if(sanction instanceof SanctionAuto.SanctionMute) {
			return ESMessages.PROFILE_AUTO_MUTE.get();
		} else if(sanction instanceof SanctionAuto.SanctionJail) {
			return ESMessages.PROFILE_AUTO_JAIL.get();
		}
		return "";
	}
	
	public String getType(SanctionManual sanction) {
		if(sanction instanceof Sanction.SanctionBanProfile) {
			return ESMessages.PROFILE_MANUAL_BAN_PROFILE.get();
		} else if(sanction instanceof Sanction.SanctionBanIp) {
			return ESMessages.PROFILE_MANUAL_BAN_IP.get();
		} else if(sanction instanceof Sanction.SanctionMute) {
			return ESMessages.PROFILE_MANUAL_MUTE.get();
		} else if(sanction instanceof Sanction.SanctionJail) {
			return ESMessages.PROFILE_MANUAL_JAIL.get();
		}
		return "";
	}
}
