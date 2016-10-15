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
		
		CONNECTION_BAN_UNLIMITED("connection.banUnlimited", 		"&c&lBanni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT]"),
		CONNECTION_BAN_TEMP("connection.banTemp", 					"&c&lBanni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT][RT]&cPendant encore : &7<duration>"),
		CONNECTION_BANIP_UNLIMITED("connection.banipUnlimited", 	"&c&lAdresse IP banni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT]"),
		CONNECTION_BANIP_TEMP("connection.banipTemp", 				"&c&lAdresse IP banni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT][RT]&cPendant encore : &7<duration>"),
		
		// Ban		
		BAN_DESCRIPTION("ban.description", 							"Banni le joueur du serveur"),
		BAN_UNLIMITED_STAFF("ban.unlimitedStaff", 					"&7Vous avez banni définitivement &6<player> &7du serveur pour la raison : &6<reason>"),
		BAN_UNLIMITED_PLAYER("ban.unlimitedPlayer", 				"&c&lBanni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT]"),
		BAN_TEMP_STAFF("ban.tempStaff", 							"&7Vous avez banni &6<player> &7pendant une durée de &7<duration> &7pour la raison : &6<reason>"),
		BAN_TEMP_PLAYER("ban.tempPlayer", 							"&c&lBanni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT][RT]&cPendant : &7<duration>"),
		BAN_ERROR_REASON("ban.errorReason", 						"&cErreur : La raison est obligatoire."),
		BAN_ERROR_CANCEL("ban.errorCancel", 						"&cErreur : Impossible de bannir &6<player> &cpour le moment."),
		BAN_ERROR_EQUALS("ban.errorEquals", 						"&cErreur : Impossible de vous bannir vous-même."),
		BAN_ERROR_NOEMPTY("ban.errorNoEmpty", 						"&cErreur : &6<player> &cest déjà banni."),
		
		// UnBan
		UNBAN_DESCRIPTION("unban.description", 						"Débanni le joueur du serveur"),
		UNBAN_STAFF("unban.staff", 									"&7Vous avez débanni &6<player>&7."),
		UNBAN_ERROR_REASON("unban.errorReason", 					"&cErreur : La raison est obligatoire."),
		UNBAN_ERROR_EQUALS("unban.errorEquals", 					"&cErreur : Impossible de vous débannir vous-même."),
		UNBAN_ERROR_EMPTY("unban.errorEmpty", 						"&cErreur : &6<player> &cn'est pas banni."),
		UNBAN_CANCEL("unban.cancel", 								"&cErreur : Impossible de débannir &6<player> &cpour le moment."),
		
		// Banip
		BANIP_DESCRIPTION("banip.description", 						"Banni l'adresse IP du joueur"),
		
		BANIP_IP_UNLIMITED_STAFF("banip.ip.unlimitedStaff", 		"&7Vous avez banni définitivement l'adresse IP &6<address> &7du serveur pour la raison : &6<reason>"),
		BANIP_IP_UNLIMITED_PLAYER("banip.ip.unlimitedPlayer", 		"&c&lAdresse IP banni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT]"),
		BANIP_IP_TEMP_STAFF("banip.ip.tempStaff", 					"&7Vous avez banni l'adresse IP&6<address> &7pendant une durée de &7<duration> &7pour la raison : &6<reason>"),
		BANIP_IP_TEMP_PLAYER("banip.ip.tempPlayer", 				"&c&lAdresse IP banni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT][RT]&cPendant : &7<duration>"),
		BANIP_IP_ERROR_REASON("banip.ip.errorReason", 				"&cErreur : La raison est obligatoire."),
		BANIP_IP_ERROR_CANCEL("banip.ip.errorCancel", 				"&cErreur : Impossible de bannir l'adresse IP <address> pour le moment."),
		BANIP_IP_ERROR_EQUALS("banip.ip.errorEquals", 				"&cErreur : Impossible de bannir votre propre adresse IP."),
		BANIP_IP_ERROR_NOEMPTY("banip.ip.errorNoEmpty", 			"&cErreur : L'addresse IP &6<player> &cest déjà banni."),
		
		BANIP_PLAYER_UNLIMITED_STAFF("banip.player.unlimitedStaff", "&7Vous avez banni définitivement l'adresse IP de &6<player> &7du serveur pour la raison : &6<reason>"),
		BANIP_PLAYER_UNLIMITED_PLAYER("banip.player.unlimitedPlayer", "&c&lAdresse IP banni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT]"),
		BANIP_PLAYER_TEMP_STAFF("banip.player.tempStaff", 			"&7Vous avez banni &6<player> &7d'une durée de &7<duration> &7pour la raison : &6<reason>"),
		BANIP_PLAYER_TEMP_PLAYER("banip.player.tempPlayer", 		"&c&lAdresse IP banni du serveur par <staff>[RT][RT]&cRaison : &7<reason>[RT][RT]&cPendant : &7<duration>"),
		BANIP_PLAYER_ERROR_REASON("banip.player.errorReason", 		"&cErreur : La raison est obligatoire."),
		BANIP_PLAYER_ERROR_CANCEL("banip.player.errorCancel", 		"&cErreur : Impossible de bannir l'adresse de &6<player>&c pour le moment."),
		BANIP_PLAYER_ERROR_EQUALS("banip.player.errorEquals", 		"&cErreur : Impossible de bannir votre propre adresse IP."),
		BANIP_PLAYER_ERROR_NOEMPTY("banip.player.errorNoEmpty", 	"&cErreur : L'adresse IP de &6<player> &cest déjà banni."),
		BANIP_PLAYER_ERROR_IP("banip.player.errorIP", 				"&cErreur : Aucune adresse IP connu de &6<player>&c."),
		
		// UnBanIp
		UNBANIP_DESCRIPTION("unbanip.description", "Débanni l'adresse IP du serveur"),
		
		UNBANIP_IP_STAFF("unbanip.ip.staff", "&7Vous avez débanni l'adresse IP &6<address>&7."),
		UNBANIP_IP_ERROR_REASON("unbanip.ip.errorReason", "&cErreur : La raison est obligatoire."),
		UNBANIP_IP_ERROR_EQUALS("unbanip.ip.errorEquals", "&cErreur : Impossible de débannir votre propre adresse IP."),
		UNBANIP_IP_ERROR_EMPTY("unbanip.ip.errorEmpty", "&cErreur : L'adresse IP  &6<address> &cn'est pas banni."),
		UNBANIP_IP_CANCEL("unbanip.ip.cancel", "&cErreur : Impossible de débannir l'adresse IP &6<address> &cpour le moment."),
		
		UNBANIP_PLAYER_STAFF("unbanip.player.staff", "&7Vous avez débanni l'adresse IP de &6<player>&7."),
		UNBANIP_PLAYER_ERROR_REASON("unbanip.player.errorReason", "&cErreur : La raison est obligatoire."),
		UNBANIP_PLAYER_ERROR_EQUALS("unbanip.player.errorEquals", "&cErreur : Impossible de débannir votre propre adresse IP."),
		UNBANIP_PLAYER_ERROR_EMPTY("unbanip.player.errorEmpty", "&cErreur : L'adresse IP de &6<player> &cn'est pas banni."),
		UNBANIP_PLAYER_CANCEL("unbanip.player.cancel", "&cErreur : Impossible de débannir l'adresse IP de &6<player> &cpour le moment."),
		
		// Jail	
		JAIL_DESCRIPTION("jail.description", 										"Emprisonne le joueur"),
		JAIL_UNLIMITED_STAFF("jail.unlimitedStaff", 								"&7Vous avez emprissonner définitivement &6<player> &7dans la prison &6<jail> &7pour la raison : &6<reason>"),
		JAIL_UNLIMITED_PLAYER("jail.unlimitedPlayer", 								"&c&lVous avez été emprissonner par &6<staff> &c&lpour la raison : &6<reason>"),
		JAIL_TEMP_STAFF("jail.tempStaff", 											"&7Vous avez emprissonner &6<player> &7pendant une durée de &7<duration> &7dans la prison &6<jail> &7pour la raison : &6<reason>"),
		JAIL_TEMP_PLAYER("jail.tempPlayer", 										"&c&lVous avez été emprissonner par &6<staff> &c&lpendant une durée &6<duration> &c&lpour la raison : &6<reason>"),
		JAIL_ERROR_REASON("jail.errorReason", 										"&cErreur : La raison est obligatoire."),
		JAIL_ERROR_CANCEL_UNLIMITED("jail.errorCancelUnlimited", 					"&cErreur : Impossible d'emprissonner &6<player> &cpour le moment."),
		JAIL_ERROR_CANCEL_TEMP("jail.errorCancelTemp", 								"&cErreur : Impossible d'emprissonner &6<player> &cpour le moment."),
		JAIL_ERROR_EQUALS("jail.errorEquals", 										"&cErreur : Impossible de vous emprissonner vous-même."),
		JAIL_ERROR_NOEMPTY("jail.errorNoEmpty", 									"&cErreur : &6<player> &cest déjà emprissonner."),
		
		JAIL_DISABLE_COMMAND_TEMP("jail.disableCommandTemp", 						"&cVous ne pouvez pas utiliser cette commande quand vous êtes en prison."),
		JAIL_DISABLE_COMMAND_UNLIMITED("jail.disableCommandUnlimited", 				"&cVous ne pouvez pas utiliser cette commande quand vous êtes en prison."),
		JAIL_DISABLE_MOVE_TEMP("jail.disableMoveTemp", 								"&cNe vous éloignez pas de votre prison."),
		JAIL_DISABLE_MOVE_UNLIMITED("jail.disableMoveUnlimited", 					"&cNe vous éloignez pas de votre prison."),
		JAIL_CONNECTION_TEMP("jail.connectionTemp", 								"&c&lVous êtes emprissonner définitivement par &6<staff> &c&lpour la raison : &6<reason>"),
		JAIL_CONNECTION_UNLIMITED("jail.connectionUnlimited", 						"&c&lVous êtes emprissonner par &6<staff> &c&lpendant une durée &6<duration> &c&lpour la raison : &6<reason>"),
		
		// UnJail
		UNJAIL_DESCRIPTION("unjail.description", 									"Libéré le joueur d'une prison"),
		UNJAIL_STAFF("unjail.staff", 												"&7Vous avez libéré &6<player>&7 de prison."),
		UNJAIL_PLAYER("unjail.player", 												"&6<staff> &7vous as libéré de prison."),
		UNJAIL_ERROR_REASON("unjail.errorReason", 									"&cErreur : La raison est obligatoire."),
		UNJAIL_ERROR_EQUALS("unjail.errorEquals", 									"&cErreur : Impossible de vous libéré vous-même."),
		UNJAIL_ERROR_EMPTY("unjail.errorEmpty", 									"&cErreur : &6<player> &cn'est pas emprisonné."),
		UNJAIL_CANCEL("unjail.cancel", 												"&cErreur : Impossible de libérer &6<player> &cpour le moment."),

		JAIL_NAME("jail.name", 														"&6&l<name>"),
		JAIL_NAME_HOVER("jail.nameHover", 											"&cRayon : &6<radius> block(s)[RT]&cMonde : &6<world>[RT]&cX : &6<x>[RT]&cY : &6<y>[RT]&cZ : &6<z>"),
		JAIL_UNKNOWN("jail.unknown", 												"&cErreur : Impossible de trouver un prison avec le nom &6<jail>&c."),
		JAIL_EMPTY("jail.empty",													"&cErreur : Il y a aucune prison défini sur le serveur."),
		
		// Mute		
		MUTE_DESCRIPTION("mute.description", 										"Mute le joueur"),
		MUTE_UNLIMITED_STAFF("mute.unlimitedStaff", 								"&7Vous avez définitivement rendu muet &6<player> &7pour la raison : &6<reason>"),
		MUTE_UNLIMITED_PLAYER("mute.unlimitedPlayer", 								"&6<staff> &c&lvous a définitivement rendu muet pour la raison : &6<reason>[RT]"),
		MUTE_TEMP_STAFF("mute.tempStaff", 											"&7Vous avez rendu muet &6<player> &7pendant une durée de &7<duration> &7pour la raison : &6<reason>"),
		MUTE_TEMP_PLAYER("mute.tempPlayer", 										"&6<staff> &c&lvous a rendu muet pendant &6<duration> &c&lpour la raison : &6<reason>"),
		MUTE_ERROR_REASON("mute.errorReason", 										"&cErreur : La raison est obligatoire."),
		MUTE_ERROR_CANCEL("mute.errorCancel", 										"&cErreur : Impossible de bannir &6<player> &cpour le moment."),
		MUTE_ERROR_EQUALS("mute.errorEquals", 										"&cErreur : Impossible de vous bannir vous-même."),
		MUTE_ERROR_NOEMPTY("mute.errorNoEmpty", 									"&cErreur : &6<player> &cest déjà banni."),
		
		MUTE_DISABLE_CHAT_TEMP("mute.disableChatTemp",								"&cVous êtes encore muet pendant &6<duration> &cpour &6<reason>&c."),
		MUTE_DISABLE_CHAT_UNLIMITED("mute.disableChatUnlimited",					"&cVous muet indéfiniment &cpour &6<reason>&c."),
		MUTE_DISABLE_COMMAND_TEMP("mute.disableCommandTemp", 						"&cVous ne pouvez pas utiliser cette commande quand vous êtes muet."),
		MUTE_DISABLE_COMMAND_UNLIMITED("mute.disableCommandUnlimited", 				"&cVous ne pouvez pas utiliser cette commande quand vous êtes muet."),
		MUTE_CONNECTION_TEMP("mute.connectionTemp", 								"&c&lVous êtes définitivement muet pour avoir : &6<reason>[RT]"),
		MUTE_CONNECTION_UNLIMITED("mute.connectionUnlimited", 						"&c&lVous êtes définitivement muet pour avoir pendant encore &6<duration> &c&lpour avoir : &6<reason>"),
		
		// UnMute
		UNMUTE_DESCRIPTION("unmute.description", 									"Débanni le joueur du serveur"),
		UNMUTE_STAFF("unmute.staff", 												"&7Vous avez débanni &6<player>&7."),
		UNMUTE_ERROR_REASON("unmute.errorReason", 									"&cErreur : La raison est obligatoire."),
		UNMUTE_ERROR_EQUALS("unmute.errorEquals", 									"&cErreur : Impossible de vous débannir vous-même."),
		UNMUTE_ERROR_EMPTY("unmute.errorEmpty", 									"&cErreur : &6<player> &cn'est pas banni."),
		UNMUTE_CANCEL("unmute.cancel", 												"&cErreur : Impossible de débannir &6<player> &cpour le moment."),
		
		// Jails
		JAILS_DESCRIPTION("jails.description", 										"Gestion des prisons"),
		
		JAILS_LIST_DESCRIPTION("jails.list.description", 							"Affiche la liste des prisons"),
		JAILS_LIST_EMPTY("jails.list.empty", 										"&7Aucune prison"),
		JAILS_LIST_TITLE("jails.list.title", 										"&aListe des prisons"),
		JAILS_LIST_LINE_DELETE("jails.list.lineDelete", 							"    &6&l➤  &6<jail> &7: <teleport> <delete>"),
		JAILS_LIST_LINE_DELETE_ERROR_WORLD("jails.list.lineDeleteErrorWorld", 		"    &6&l➤  &6<jail> &7: <delete>"),
		JAILS_LIST_LINE("jails.list.line", 											"    &6&l➤  &6<jail> &7: <teleport>"),
		JAILS_LIST_TELEPORT("jails.list.teleport", 									"&a&nTéléporter"),
		JAILS_LIST_TELEPORT_HOVER("jails.list.teleportHover", 						"&cCliquez ici pour vous téléporter à la prison &6<jail>&c."),
		JAILS_LIST_DELETE("jails.list.delete", 										"&c&nSupprimer"),
		JAILS_LIST_DELETE_HOVER("jails.list.deleteHover", 							"&cCliquez ici pour supprimer la prison &6<jail>&c."),
		
		JAILS_TELEPORT_DESCRIPTION("jails.teleport.description", 					"Téléporte à une prison"),
		JAILS_TELEPORT_PLAYER("jails.teleport.player", 								"&7Vous avez été téléporté à la prison &6<jail>&7."),
		JAILS_TELEPORT_PLAYER_ERROR("jails.teleport.playerError", 					"&cErreur : Impossible de trouver un position pour vous téléportez à la prison &6<jail>&c."),
		
		JAILS_DELETE_DESCRIPTION("jails.delete.description", 						"Supprime une prison"),
		JAILS_DELETE_CONFIRMATION("jails.delete.confirmation", 						"&7Souhaitez-vous vraiment supprimer la prison &6<jail> &7: <confirmation>"),
		JAILS_DELETE_CONFIRMATION_VALID("jails.delete.confirmationValid", 			"&2&nConfirmer"),
		JAILS_DELETE_CONFIRMATION_VALID_HOVER("jails.delete.confirmationValidHover","&cCliquez ici pour supprimer la prison &6<jail>&c."),
		JAILS_DELETE_DELETE("jails.delete.delete", 									"&7Vous avez supprimé la prison &6<jail>&7."),
		JAILS_DELETE_CANCEL("jails.delete.cancel", 									"&cErreur : Impossible de supprimé la &6<jail> &cpour le moment."),
		
		JAILS_ADD_DESCRIPTION("jails.add.description", 								"Crée une prison"),
		JAILS_ADD_REPLACE("jails.add.replace", 										"&7Vous avez redéfini la prison &6<jail>&7."),
		JAILS_ADD_NEW("jails.add.new", 												"&7Vous avez défini la prison &6<jail>&7."),
		JAILS_ADD_CANCEL_REPLACE("jails.add.cancelReplace", 						"&cErreur : Impossible de redéfinir la prison &6<jail> &4pour le moment."),
		JAILS_ADD_CANCEL_NEW("jails.add.cancelNew", 								"&cErreur : Impossible de définir la prison &6<jail> &4pour le moment."),
		
		JAILS_SETRADIUS_DESCRIPTION("jails.setradius.description", 					"Modifie le rayon d'une prison"),
		JAILS_SETRADIUS_DEFAULT("jails.setradius.default", 							"&7Vous avez défini le rayon de la prison <jail> &7avec la valeur par défault (&6<radius> block(s)&7)"),
		JAILS_SETRADIUS_VALUE("jails.setradius.value", 								"&7Vous avez défini le rayon de la prison <jail> &7à &6<radius> block(s)&7."),
		JAILS_SETRADIUS_CANCEL_DEFAULT("jails.setradius.cancelDefault", 			"&cErreur : Impossible de définir le rayon de la prison <jail> &cpour le moment."),
		JAILS_SETRADIUS_CANCEL_VALUE("jails.setradius.cancelValue", 				"&cErreur : Impossible de définir le rayon de la prison <jail> &cpour le moment.");
		
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
