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
package fr.evercraft.eversanctions.command.jails;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.command.ESubCommand;
import fr.evercraft.everapi.server.location.LocationSQL;
import fr.evercraft.everapi.text.ETextBuilder;
import fr.evercraft.eversanctions.ESMessage.ESMessages;
import fr.evercraft.eversanctions.command.jail.ESJail;
import fr.evercraft.eversanctions.ESPermissions;
import fr.evercraft.eversanctions.EverSanctions;
import fr.evercraft.eversanctions.service.EJail;

public class ESJailsList extends ESubCommand<EverSanctions> {
	
	public ESJailsList(final EverSanctions plugin, final ESJails command) {
        super(plugin, command, "list");
    }
	
	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission(ESPermissions.JAILS_LIST.get());
	}

	@Override
	public Text description(final CommandSource source) {
		return EChat.of(ESMessages.JAILS_LIST_DESCRIPTION.get());
	}

	@Override
	public Text help(final CommandSource source) {
		return Text.builder("/" + this.getName())
				.onClick(TextActions.suggestCommand("/" + this.getName() + " "))
				.color(TextColors.RED)
				.build();
	}
	
	@Override
	public List<String> subTabCompleter(final CommandSource source, final List<String> args) throws CommandException {
		return new ArrayList<String>();
	}
	
	@Override
	public boolean subExecute(final CommandSource source, final List<String> args) throws CommandException {
		// Résultat de la commande :
		boolean resultat = false;
		
		if (args.size() == 0) {
			resultat = this.commandJailList(source);
		// Nombre d'argument incorrect
		} else {
			source.sendMessage(this.help(source));
		}
		
		return resultat;
	}
	
	public boolean commandJailList(final CommandSource player) throws CommandException {
		TreeSet<EJail> jails = new TreeSet<EJail>((o1, o2) -> o1.getName().compareTo(o2.getName()));
		jails.addAll(this.plugin.getJailService().getAllEJail());
		
		List<Text> lists = new ArrayList<Text>();
		if (player.hasPermission(ESPermissions.JAILS_DELETE.get())) {
			for (EJail jail : jails) {
				if (jail.getTransform() != null) {
					lists.add(ETextBuilder.toBuilder(ESMessages.JAILS_LIST_LINE_DELETE.get())
						.replace("<jail>", ESJail.getButtonJail(jail))
						.replace("<radius>", String.valueOf(jail.getRadius()))
						.replace("<teleport>", this.getButtonTeleport(jail.getName(), jail.getTransform()))
						.replace("<delete>", this.getButtonDelete(jail.getName(), jail.getLocationSQL()))
						.build());
				} else {
					lists.add(ETextBuilder.toBuilder(ESMessages.JAILS_LIST_LINE_DELETE_ERROR_WORLD.get())
						.replace("<jail>", ESJail.getButtonJail(jail))
						.replace("<radius>", String.valueOf(jail.getRadius()))
						.replace("<delete>", this.getButtonDelete(jail.getName(), jail.getLocationSQL()))
						.build());
				}
			}
		} else {
			for (EJail jail : jails) {
				if (jail.getTransform() != null) {
					lists.add(ETextBuilder.toBuilder(ESMessages.JAILS_LIST_LINE.get())
						.replace("<jail>", ESJail.getButtonJail(jail))
						.replace("<radius>", String.valueOf(jail.getRadius()))
						.replace("<teleport>", this.getButtonTeleport(jail.getName(), jail.getTransform()))
						.build());
				}
			}
		}
		
		if (lists.size() == 0) {
			lists.add(ESMessages.JAILS_LIST_EMPTY.getText());
		}
		
		this.plugin.getEverAPI().getManagerService().getEPagination().sendTo(ESMessages.JAILS_LIST_TITLE.getText().toBuilder()
				.onClick(TextActions.runCommand(this.getName())).build(), lists, player);			
		return false;
	}
	

	private Text getButtonTeleport(final String name, final Transform<World> location){
		return ESMessages.JAILS_LIST_TELEPORT.getText().toBuilder()
					.onHover(TextActions.showText(EChat.of(ESMessages.JAILS_LIST_TELEPORT_HOVER.get()
							.replaceAll("<jail>", name))))
					.onClick(TextActions.runCommand("/jails teleport \"" + name + "\""))
					.build();
	}
	
	private Text getButtonDelete(final String name, final LocationSQL location){
		return ESMessages.JAILS_LIST_DELETE.getText().toBuilder()
					.onHover(TextActions.showText(EChat.of(ESMessages.JAILS_LIST_DELETE_HOVER.get()
							.replaceAll("<jail>", name))))
					.onClick(TextActions.runCommand("/jails delete \"" + name + "\""))
					.build();
	}
}