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
import fr.evercraft.everapi.server.user.EUser;
import fr.evercraft.everapi.services.sanction.Sanction;
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;

public class ESProfile extends ECommand<EverSanctions> {
	
	private static final Comparator<Sanction> COMPARATOR = 
			(Sanction o1, Sanction o2) -> {
				if (o1.isExpire() && !o2.isExpire()) return -1;
				if (!o1.isExpire() && o2.isExpire()) return 1;
				return o2.getCreationDate().compareTo(o1.getCreationDate());
			};
	
	public enum Type {
		BAN_PROFILE,
		BAN_IP,
		MUTE,
		JAIL;
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
		return Text.builder("/" + this.getName() + " <" + EAMessages.ARGS_PLAYER.get() + "|" + EAMessages.ARGS_IP.get() + "> [" + EAMessages.ARGS_TYPE.get() + "]")
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
		if (args.size() == 1) {
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
		valid.addAll(user.getAllSanctions());
		
		List<Text> list = new ArrayList<Text>();
		valid.forEach(sanction -> {
			String message = "";
			// Auto
			if (sanction instanceof SanctionAuto) {
				SanctionAuto auto = (SanctionAuto) sanction;
				
				if(auto.getOption().isPresent()) {
					if (sanction.isPardon()) {
						message = ESMessages.PROFILE_LINE_PARDON_AUTO_OPTION.get();
					} else if(sanction.isExpire()) {
						message = ESMessages.PROFILE_LINE_DISABLE_AUTO_OPTION.get();
					} else {
						message = ESMessages.PROFILE_LINE_ENABLE_AUTO_OPTION.get();
					}
				} else {
					if (sanction.isPardon()) {
						message = ESMessages.PROFILE_LINE_PARDON_AUTO.get();
					} else if(sanction.isExpire()) {
						message = ESMessages.PROFILE_LINE_DISABLE_AUTO.get();
					} else {
						message = ESMessages.PROFILE_LINE_ENABLE_AUTO.get();
					}
				}
			// Manual
			} else {
				if(sanction instanceof Sanction.SanctionBanIp || sanction instanceof Sanction.SanctionJail) {
					if (sanction.isPardon()) {
						message = ESMessages.PROFILE_LINE_PARDON_MANUAL_OPTION.get();
					} else if(sanction.isExpire()) {
						message = ESMessages.PROFILE_LINE_DISABLE_MANUAL_OPTION.get();
					} else {
						message = ESMessages.PROFILE_LINE_ENABLE_MANUAL_OPTION.get();
					}
				} else {
					if (sanction.isPardon()) {
						message = ESMessages.PROFILE_LINE_PARDON_MANUAL.get();
					} else if(sanction.isExpire()) {
						message = ESMessages.PROFILE_LINE_DISABLE_MANUAL.get();
					} else {
						message = ESMessages.PROFILE_LINE_ENABLE_MANUAL.get();
					}
				}
				message = message.replaceAll("<staff>", sanction.getSourceName())
								.replaceAll("<reason>", EChat.serialize(sanction.getReason()))
								.replaceAll("<duration>", this.plugin.getEverAPI().getManagerUtils().getDate().formatDateDiff(sanction.getCreationDate(), sanction.getExpirationDate().get()))
								.replaceAll("<creation_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getCreationDate()))
								.replaceAll("<creation_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getCreationDate()))
								.replaceAll("<creation_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getCreationDate()))
								.replaceAll("<expiration_time>", this.plugin.getEverAPI().getManagerUtils().getDate().parseTime(sanction.getExpirationDate().get()))
								.replaceAll("<expiration_date>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDate(sanction.getExpirationDate().get()))
								.replaceAll("<expiration_datetime>", this.plugin.getEverAPI().getManagerUtils().getDate().parseDateTime(sanction.getExpirationDate().get()));
			}
		});
		
		if (list.isEmpty()) {
			
		}
		
		return true;
	}
}
