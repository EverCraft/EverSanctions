package fr.evercraft.eversanctions.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.profile.GameProfile;
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
import fr.evercraft.everapi.text.ETextBuilder;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;

public class ESBan extends ECommand<EverSanctions> {
	
	public ESBan(final EverSanctions plugin) {
        super(plugin, "back", "return");
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.BAN.get());
	}

	@Override
	public Text description(final CommandSource source) {
		return ESMessages.BAN_DESCRIPTION.getText();
	}

	@Override
	public Text help(final CommandSource source) {
		return Text.builder("/" + this.getName() + " " + EAMessages.ARGS_PLAYER.get())
				.onClick(TextActions.suggestCommand("/" + this.getName()))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public List<String> tabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		return new ArrayList<String>();
	}
	
	@Override
	public boolean execute(final CommandSource source, final List<String> args) throws CommandException {
		// Résultat de la commande :
		boolean resultat = false;
		
		// Nombre d'argument correct
		if (args.size() == 2) {
			
			Optional<EUser> user = this.plugin.getEServer().getEUser(args.get(0));
			// Le joueur existe
			if (user.isPresent()){
				resultat = this.commandBan(source, user.get(), args.get(1));
			// Le joueur est introuvable
			} else {
				Optional<GameProfile> gameprofile = this.plugin.getEServer().getGameProfile(args.get(0));
				// Le joueur existe
				if (gameprofile.isPresent()){
					resultat = this.commandBan(source, this.plugin.getEServer().getOrCreateEUser(gameprofile.get()), args.get(1));
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
	
	private boolean commandBan(final CommandSource staff, EUser user, String reason_string) {
		Text reason = EChat.of(reason_string);
		if (reason.isEmpty()) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BAN_REASON_EMPTY.get()
						.replaceAll("<player>", user.getName())));
			return false;
		}
		
		if (!user.ban(Optional.empty(), reason, staff.getIdentifier())) {
			staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BAN_CANCEL.get()
						.replaceAll("<player>", user.getName())));
			return false;
		}
		
		staff.sendMessage(EChat.of(ESMessages.PREFIX.get() + ESMessages.BAN_STAFF.get()
			 .replaceAll("<player>", user.getName())));
		return true;
	}
}
