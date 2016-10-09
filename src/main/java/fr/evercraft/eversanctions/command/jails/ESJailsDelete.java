package fr.evercraft.eversanctions.command.jails;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import fr.evercraft.everapi.EAMessage.EAMessages;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.command.ESubCommand;
import fr.evercraft.everapi.server.player.EPlayer;
import fr.evercraft.everapi.text.ETextBuilder;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.command.jail.ESJail;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.EJail;

public class ESJailsDelete extends ESubCommand<EverSanctions> {
	
	public ESJailsDelete(final EverSanctions plugin, final ESJails command) {
        super(plugin, command, "delete");
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.JAILS_DELETE.get());
	}

	@Override
	public Text description(final CommandSource source) {
		return EChat.of(ESMessages.JAILS_DELETE_DESCRIPTION.get());
	}

	@Override
	public Text help(final CommandSource source) {
		return Text.builder("/" + this.getName() + " <" + EAMessages.ARGS_JAIL.get() + ">")
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public List<String> subTabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		List<String> suggests = new ArrayList<String>();
		if (args.size() == 1) {
			this.plugin.getJailService().getAll().forEach(jail -> suggests.add(jail.getName()));
		}
		return suggests;
	}
	
	@Override
	public boolean subExecute(final CommandSource source, final List<String> args) throws CommandException {
		// Résultat de la commande :
		boolean resultat = false;
		
		if (args.size() == 1) {
			resultat = this.commandJailDelete((EPlayer) source, args.get(0)); 
		} else if (args.size() == 2 && args.get(1).equalsIgnoreCase("confirmation")) {
			resultat = this.commandJailDeleteConfirmation((EPlayer) source, args.get(0)); 
		// Nombre d'argument incorrect
		} else {
			source.sendMessage(this.help(source));
		}
		
		return resultat;
	}
	
	private boolean commandJailDelete(final EPlayer player, final String jail_name) {
		String name = EChat.fixLength(jail_name, this.plugin.getEverAPI().getConfigs().getMaxCaractere());
		
		Optional<EJail> jail = this.plugin.getJailService().getEJail(name);
		// Le serveur a une prison qui porte ce nom
		if (jail.isPresent()) {
			player.sendMessage(ETextBuilder.toBuilder(ESMessages.PREFIX.get())
					.append(ESMessages.JAILS_DELETE_CONFIRMATION.get())
					.replace("<jail>", ESJail.getButtonJail(jail.get()))
					.replace("<confirmation>", this.getButtonConfirmation(name))
					.build());
		// Le serveur n'a pas de prison qui porte ce nom
		} else {
			player.sendMessage(ESMessages.PREFIX.get() + ESMessages.JAIL_UNKNOWN.get().replaceAll("<jail>", name));
		}
		return false;
	}
	
	private boolean commandJailDeleteConfirmation(final EPlayer player, final String jail_name) {
		String name = EChat.fixLength(jail_name, this.plugin.getEverAPI().getConfigs().getMaxCaractere());
		
		Optional<EJail> jail = this.plugin.getJailService().getEJail(name);
		// Le serveur a une prison qui porte ce nom
		if (jail.isPresent()) {
			// Si la prison a bien été supprimer
			if (this.plugin.getJailService().remove(name)) {
				player.sendMessage(ETextBuilder.toBuilder(ESMessages.PREFIX.get())
						.append(ESMessages.JAILS_DELETE_DELETE.get())
						.replace("<jail>", ESJail.getButtonJail(jail.get()))
						.build());
				return true;
			// La prison n'a pas été supprimer
			} else {
				player.sendMessage(ESMessages.PREFIX.get() + ESMessages.JAILS_DELETE_CANCEL.get().replaceAll("<jail>", name));
			}
		// Le serveur n'a pas de prison qui porte ce nom
		} else {
			player.sendMessage(ESMessages.PREFIX.get() + ESMessages.JAIL_UNKNOWN.get().replaceAll("<jail>", name));
		}
		return false;
	}
	
	private Text getButtonConfirmation(final String name){
		return ESMessages.JAILS_DELETE_CONFIRMATION_VALID.getText().toBuilder()
					.onHover(TextActions.showText(EChat.of(ESMessages.JAILS_DELETE_CONFIRMATION_VALID_HOVER.get()
							.replaceAll("<jail>", name))))
					.onClick(TextActions.runCommand("/jails delete \"" + name + "\" confirmation"))
					.build();
	}
}