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
package fr.evercraft.eversanctions;

import java.util.Arrays;
import java.util.List;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;

import com.google.common.base.Preconditions;

import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.file.EMessage;
import fr.evercraft.everapi.plugin.file.EnumMessage;

public class ESMessage extends EMessage {

	public ESMessage(final EverSanctions plugin) {
		super(plugin, ESMessages.values());
	}
	
	public enum ESMessages implements EnumMessage {
		PREFIX("prefix",  							
				"[&4Ever&6&lSanctions&f] "),
		DESCRIPTION("description", 
				"Gestionnaire de sanction"),
		
		CONNECTION_BAN_UNLIMITED("connection.banUnlimited", "&c&lBanni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT]"),
		CONNECTION_BAN_TEMP("connection.banTemp", "&c&lBanni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT][RT]&cPendant encore : &7<duration>"),
		CONNECTION_BANIP_UNLIMITED("connection.banipUnlimited", "&c&lIP banni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT]"),
		CONNECTION_BANIP_TEMP("connection.banipTemp", "&c&lIP banni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT][RT]&cPendant encore : &7<time>"),
		
		// Ban		
		BAN_DESCRIPTION("ban.description", "Banni le joueur du serveur"),
		BAN_UNLIMITED_STAFF("ban.unlimitedStaff", "&7Vous avez banni définitivement &6<player> &7du serveur pour la raison : &6<reason>&7."),
		BAN_UNLIMITED_PLAYER("ban.unlimitedPlayer", "&c&lBanni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT]"),
		BAN_TEMP_STAFF("ban.tempStaff", "&7Vous avez banni &6<player> &7d'une durée de &7<duration> &7pour la raison : &6<reason>&7."),
		BAN_TEMP_PLAYER("ban.tempPlayer", "&c&lBanni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT][RT]&cPendant : &7<duration>"),
		BAN_ERROR_TIME("ban.errorTime", "&cErreur : '<time>' n'est pas une durée."),
		BAN_ERROR_REASON("ban.errorReason", "&cErreur : La raison est obligatoire."),
		BAN_ERROR_CANCEL("ban.errorCancel", "&cErreur : Impossible de bannir <player> pour le moment."),
		BAN_ERROR_EQUALS("ban.errorEquals", "&cErreur : Impossible de vous bannir vous-même."),
		BAN_ERROR_NOEMPTY("ban.errorNoEmpty", "&cErreur : &6<player> &cest déjà banni."),
		
		UNBAN_DESCRIPTION("unban.description", "Débanni le joueur du serveur"),
		UNBAN_STAFF("unban.staff", "&7Vous avez débanni &6<player>&7."),
		UNBAN_REASON_EMPTY("unban.reasonEmpty", "&cErreur : La raison est obligatoire."),
		UNBAN_CANCEL("unban.cancel", "&cErreur : Impossible de débannir <player> pour le moment."),
		
		// Banip
		BANIP_DESCRIPTION("banip.description", "Banni l'adresse IP du joueur"),
		
		MUTE_DESCRIPTION("mute.description", "Mute le joueur"),
		
		JAIL_DESCRIPTION("jail.description", "Emprisonne le joueur");
		
		private final String path;
	    private final Object french;
	    private final Object english;
	    private Object message;
	    
	    private ESMessages(final String path, final Object french) {   	
	    	this(path, french, french);
	    }
	    
	    private ESMessages(final String path, final Object french, final Object english) {
	    	Preconditions.checkNotNull(french, "Le message '" + this.name() + "' n'est pas définit");
	    	
	    	this.path = path;	    	
	    	this.french = french;
	    	this.english = english;
	    	this.message = french;
	    }

	    public String getName() {
			return this.name();
		}
	    
		public String getPath() {
			return this.path;
		}

		public Object getFrench() {
			return this.french;
		}

		public Object getEnglish() {
			return this.english;
		}
		
		public String get() {
			if (this.message instanceof String) {
				return (String) this.message;
			}
			return this.message.toString();
		}
			
		@SuppressWarnings("unchecked")
		public List<String> getList() {
			if (this.message instanceof List) {
				return (List<String>) this.message;
			}
			return Arrays.asList(this.message.toString());
		}
		
		public void set(Object message) {
			this.message = message;
		}

		public Text getText() {
			return EChat.of(this.get());
		}
		
		public TextColor getColor() {
			return EChat.getTextColor(this.get());
		}
	}
}
