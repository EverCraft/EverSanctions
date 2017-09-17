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

import com.google.common.base.Preconditions;

import fr.evercraft.everapi.message.EMessageBuilder;
import fr.evercraft.everapi.message.EMessageFormat;
import fr.evercraft.everapi.message.format.EFormatString;
import fr.evercraft.everapi.plugin.file.EMessage;
import fr.evercraft.everapi.plugin.file.EnumMessage;
import fr.evercraft.everapi.services.sanction.SanctionService;

public class ESMessage extends EMessage<EverSanctions> {

	public ESMessage(final EverSanctions plugin) {
		super(plugin, ESMessages.values());
	}
	
	public enum ESMessages implements EnumMessage {
		PREFIX(									"[&4Ever&6&lSanctions&f] "),
		DESCRIPTION(							"Gestionnaire de sanction"),
		
		CONNECTION_BAN_UNLIMITED(				"&c&lBanni du serveur par {staff}[RT][RT]&cRaison : &7{reason}[RT]"),
		CONNECTION_BAN_TEMP(					"&c&lBanni du serveur par {staff}[RT][RT]&cRaison : &7{reason}[RT][RT]&cPendant encore : &7{duration}"),
		CONNECTION_BANIP_UNLIMITED(				"&c&lAdresse IP banni du serveur par {staff}[RT][RT]&cRaison : &7{reason}[RT]"),
		CONNECTION_BANIP_TEMP(					"&c&lAdresse IP banni du serveur par {staff}[RT][RT]&cRaison : &7{reason}[RT][RT]&cPendant encore : &7{duration}"),
		
		// Profile
		PROFILE_DESCRIPTION(					"Liste des sanctions d'un joueur"),

		PROFILE_LINE_ENABLE_MANUAL(				"    &6&l➤ &a{type} :[RT]{line_reason}{line_staff}{line_ip}{line_jail}{line_creation}{line_expiration}"),
		PROFILE_LINE_ENABLE_AUTO(				"    &6&l➤ &a{reason} : niveau {level}[RT]{line_reason}{line_staff}{line_ip}{line_jail}{line_creation}{line_expiration}"),
		
		PROFILE_LINE_PARDON_MANUAL(				"    &6&l➤ &c{type} :[RT]{line_reason}{line_staff}{line_ip}{line_jail}{line_creation}{line_expiration}"
											  + "[RT]        &cAnnulation :[RT]{line_pardon_reason}{line_pardon_staff}{line_pardon_date}"),
		PROFILE_LINE_PARDON_AUTO(				"    &6&l➤ &c{reason} : niveau {level}[RT]{line_reason}{line_staff}{line_ip}{line_jail}{line_creation}{line_expiration}"
						 					  + "[RT]        &cAnnulation :[RT]{line_pardon_reason}{line_pardon_staff}{line_pardon_date}"),
		
		PROFILE_LINE_DISABLE_MANUAL(			"    &6&l➤ &c{type} :[RT]{line_reason}{line_staff}{line_ip}{line_jail}{line_creation}{line_expiration}"),
		PROFILE_LINE_DISABLE_AUTO(				"    &6&l➤ &c{reason} : niveau {level}[RT]{line_reason}{line_staff}{line_ip}{line_jail}{line_creation}{line_expiration}"),
		
		PROFILE_LINE_TYPE(						"        &7Type : {type}[RT]"),
		PROFILE_LINE_REASON(					"        &7Raison : {reason}[RT]"),
		PROFILE_LINE_STAFF(						"        &7Par : {staff}[RT]"),
		PROFILE_LINE_CREATION(					"        &7Création : {datetime}[RT]"),
		PROFILE_LINE_EXPIRATION_TEMP(			"        &7Expiration : {datetime}"),
		PROFILE_LINE_EXPIRATION_UNLIMITED(		"        &7Expiration : Aucune"),
		PROFILE_LINE_IP(						"        &7Ip : {address}[RT]"),
		PROFILE_LINE_JAIL(						"        &7Jail : {jail}[RT]"),
		PROFILE_LINE_PARDON_STAFF(				"            &cPar : {staff}[RT]"),
		PROFILE_LINE_PARDON_REASON(				"            &cRaison : {reason}[RT]"),
		PROFILE_LINE_PARDON_DATE(	 			"            &cDate : {datetime}"),
		
		PROFILE_AUTO_BAN_PROFILE(				"Compte banni"),
		PROFILE_AUTO_BAN_IP(					"Adresse IP banni"),
		PROFILE_AUTO_BAN_PROFILE_AND_IP(		"Compte et adresse IP banni"),
		PROFILE_AUTO_MUTE(						"Compte muet"),
		PROFILE_AUTO_JAIL(						"Compte emprisonné"),
		PROFILE_AUTO_MUTE_AND_JAIL(				"Compte muet et emprisonné"),
		PROFILE_MANUAL_BAN_PROFILE(				"Compte banni"),
		PROFILE_MANUAL_BAN_IP(					"Adresse IP banni"),
		PROFILE_MANUAL_MUTE(					"Compte muet"),
		PROFILE_MANUAL_JAIL(					"Compte emprisonné"),
		PROFILE_TITLE_OTHERS(					"&aSanctions de &c{player}"),
		PROFILE_TITLE_EQUALS(					"&aVos sanctions"),
		PROFILE_TITLE_OTHERS_TYPE(				"&aSanctions de &c{player} &a: &c{type}"),
		PROFILE_TITLE_EQUALS_TYPE(				"&aVos sanctions : &c{type}"),
		PROFILE_EMPTY(							"&7Aucun sanction"),
		PROFILE_ERROR_TYPE(						"&cErreur : Il n'y a pas de type '&6{type}&c'"),
		
		// Ban		
		BAN_DESCRIPTION(						"Banni le joueur du serveur"),
		BAN_UNLIMITED_STAFF(					"&7Vous avez banni définitivement &6{player} &7du serveur pour la raison : &6{reason}"),
		BAN_UNLIMITED_PLAYER(					"&c&lBanni du serveur par {staff}[RT][RT]&cRaison : &7{reason}[RT]"),
		BAN_TEMP_STAFF(							"&7Vous avez banni &6{player} &7pendant une durée de &7{duration} &7pour la raison : &6{reason}"),
		BAN_TEMP_PLAYER(						"&c&lBanni du serveur par {staff}[RT][RT]&cRaison : &7{reason}[RT][RT]&cPendant : &7{duration}"),
		BAN_ERROR_REASON(						"&cErreur : La raison est obligatoire."),
		BAN_ERROR_CANCEL(						"&cErreur : Impossible de bannir &6{player} &cpour le moment."),
		BAN_ERROR_EQUALS(						"&cErreur : Impossible de vous bannir vous-même."),
		BAN_ERROR_NOEMPTY(						"&cErreur : &6{player} &cest déjà banni."),
		
		// UnBan
		UNBAN_DESCRIPTION(						"Débanni le joueur du serveur"),
		UNBAN_STAFF(							"&7Vous avez débanni &6{player}&7."),
		UNBAN_ERROR_REASON(						"&cErreur : La raison est obligatoire."),
		UNBAN_ERROR_EQUALS(						"&cErreur : Impossible de vous débannir vous-même."),
		UNBAN_ERROR_EMPTY(						"&cErreur : &6{player} &cn'est pas banni."),
		UNBAN_CANCEL(							"&cErreur : Impossible de débannir &6{player} &cpour le moment."),
		
		// Banip
		BANIP_DESCRIPTION(						"Banni l'adresse IP du joueur"),
		
		BANIP_IP_UNLIMITED_STAFF(				"&7Vous avez banni définitivement l'adresse IP &6{address} &7du serveur pour la raison : &6{reason}"),
		BANIP_IP_UNLIMITED_PLAYER(				"&c&lAdresse IP banni du serveur par {staff}[RT][RT]&cRaison : &7{reason}[RT]"),
		BANIP_IP_TEMP_STAFF(					"&7Vous avez banni l'adresse IP&6{address} &7pendant une durée de &7{duration} &7pour la raison : &6{reason}"),
		BANIP_IP_TEMP_PLAYER(					"&c&lAdresse IP banni du serveur par {staff}[RT][RT]&cRaison : &7{reason}[RT][RT]&cPendant : &7{duration}"),
		BANIP_IP_ERROR_REASON(					"&cErreur : La raison est obligatoire."),
		BANIP_IP_ERROR_CANCEL(					"&cErreur : Impossible de bannir l'adresse IP {address} pour le moment."),
		BANIP_IP_ERROR_EQUALS(					"&cErreur : Impossible de bannir votre propre adresse IP."),
		BANIP_IP_ERROR_NOEMPTY(					"&cErreur : L'addresse IP &6{player} &cest déjà banni."),
		
		BANIP_PLAYER_UNLIMITED_STAFF(			"&7Vous avez banni définitivement l'adresse IP de &6{player} &7du serveur pour la raison : &6{reason}"),
		BANIP_PLAYER_UNLIMITED_PLAYER(			"&c&lAdresse IP banni du serveur par {staff}[RT][RT]&cRaison : &7{reason}[RT]"),
		BANIP_PLAYER_TEMP_STAFF(				"&7Vous avez banni l'adresse IP de &6{player} &7pendant une durée de &7{duration} &7pour la raison : &6{reason}"),
		BANIP_PLAYER_TEMP_PLAYER(				"&c&lAdresse IP banni du serveur par {staff}[RT][RT]&cRaison : &7{reason}[RT][RT]&cPendant : &7{duration}"),
		BANIP_PLAYER_ERROR_REASON(				"&cErreur : La raison est obligatoire."),
		BANIP_PLAYER_ERROR_CANCEL(				"&cErreur : Impossible de bannir l'adresse de &6{player}&c pour le moment."),
		BANIP_PLAYER_ERROR_EQUALS(				"&cErreur : Impossible de bannir votre propre adresse IP."),
		BANIP_PLAYER_ERROR_NOEMPTY(				"&cErreur : L'adresse IP de &6{player} &cest déjà banni."),
		BANIP_PLAYER_ERROR_IP(					"&cErreur : Aucune adresse IP connu de &6{player}&c."),
		
		// UnBanIp
		UNBANIP_DESCRIPTION(					"Débanni l'adresse IP du serveur"),
		
		UNBANIP_IP_STAFF(						"&7Vous avez débanni l'adresse IP &6{address}&7."),
		UNBANIP_IP_ERROR_REASON(				"&cErreur : La raison est obligatoire."),
		UNBANIP_IP_ERROR_EQUALS(				"&cErreur : Impossible de débannir votre propre adresse IP."),
		UNBANIP_IP_ERROR_EMPTY(					"&cErreur : L'adresse IP  &6{address} &cn'est pas banni."),
		UNBANIP_IP_CANCEL(						"&cErreur : Impossible de débannir l'adresse IP &6{address} &cpour le moment."),
		
		UNBANIP_PLAYER_STAFF(					"&7Vous avez débanni l'adresse IP de &6{player}&7."),
		UNBANIP_PLAYER_ERROR_REASON(			"&cErreur : La raison est obligatoire."),
		UNBANIP_PLAYER_ERROR_EQUALS(			"&cErreur : Impossible de débannir votre propre adresse IP."),
		UNBANIP_PLAYER_ERROR_EMPTY(				"&cErreur : L'adresse IP de &6{player} &cn'est pas banni."),
		UNBANIP_PLAYER_CANCEL(					"&cErreur : Impossible de débannir l'adresse IP de &6{player} &cpour le moment."),
		
		// Jail	
		JAIL_DESCRIPTION(						"Emprisonne le joueur"),
		JAIL_UNLIMITED_STAFF(					"&7Vous avez emprissonné définitivement &6{player} &7dans la prison &6{jail} &7pour la raison : &6{reason}"),
		JAIL_UNLIMITED_PLAYER(					"&c&lVous avez été emprissonné par &6{staff} &c&lpour la raison : &6{reason}"),
		JAIL_TEMP_STAFF(						"&7Vous avez emprissonné &6{player} &7pendant une durée de &7{duration} &7dans la prison &6{jail} &7pour la raison : &6{reason}"),
		JAIL_TEMP_PLAYER(						"&c&lVous avez été emprissonné par &6{staff} &c&lpendant une durée &6{duration} &c&lpour la raison : &6{reason}"),
		JAIL_ERROR_REASON(						"&cErreur : La raison est obligatoire."),
		JAIL_ERROR_CANCEL_UNLIMITED(			"&cErreur : Impossible d'emprissonner &6{player} &cpour le moment."),
		JAIL_ERROR_CANCEL_TEMP(					"&cErreur : Impossible d'emprissonner &6{player} &cpour le moment."),
		JAIL_ERROR_EQUALS(						"&cErreur : Impossible de vous emprissonner vous-même."),
		JAIL_ERROR_NOEMPTY(						"&cErreur : &6{player} &cest déjà emprissonné."),
		
		JAIL_DISABLE_COMMAND_TEMP(				"&cVous ne pouvez pas utiliser cette commande quand vous êtes en prison."),
		JAIL_DISABLE_COMMAND_UNLIMITED(			"&cVous ne pouvez pas utiliser cette commande quand vous êtes en prison."),
		JAIL_DISABLE_MOVE(						"&4Ne vous éloignez pas de votre prison."),
		JAIL_DISABLE_INTERACT(					EMessageFormat.builder()
														.actionbarMessageString("&4Vous ne pouvez pas intéragir car vous êtes en prison.")
														.actionbarStay(3 * 1000)
														.actionbarPriority(SanctionService.MESSAGE_JAIL)),
		JAIL_CONNECTION_TEMP(					"&c&lVous êtes emprissonné par &6{staff} &c&lpendant une durée &6{duration} &c&lpour la raison : &6{reason}"),
		JAIL_CONNECTION_UNLIMITED(				"&c&lVous êtes emprissonné définitivement par &6{staff} &c&lpour la raison : &6{reason}"),
		
		// UnJail
		UNJAIL_DESCRIPTION(						"Libère le joueur de prison"),
		UNJAIL_STAFF(							"&7Vous avez libéré &6{player}&7 de prison."),
		UNJAIL_PLAYER(							"&6{staff} &7vous as libéré de prison."),
		UNJAIL_ERROR_REASON(					"&cErreur : La raison est obligatoire."),
		UNJAIL_ERROR_EQUALS(					"&cErreur : Impossible de vous libérer vous-même."),
		UNJAIL_ERROR_EMPTY(						"&cErreur : &6{player} &cn'est pas emprisonné."),
		UNJAIL_CANCEL(							"&cErreur : Impossible de libérer &6{player} &cpour le moment."),

		JAIL_NAME(								"&6&l{name}"),
		JAIL_NAME_HOVER(						"&cRayon : &6{radius} block(s)[RT]&cMonde : &6{world}[RT]&cX : &6{x}[RT]&cY : &6{y}[RT]&cZ : &6{z}"),
		JAIL_UNKNOWN(							"&cErreur : Impossible de trouver une prison avec le nom &6{jail}&c."),
		JAIL_EMPTY(								"&cErreur : Il y a aucune prison définie sur le serveur."),
		
		// Mute		
		MUTE_DESCRIPTION(						"Réduit au silence le joueur"),
		MUTE_UNLIMITED_STAFF(					"&7Vous avez définitivement rendu muet &6{player} &7pour la raison : &6{reason}"),
		MUTE_UNLIMITED_PLAYER(					"&6{staff} &cvous a définitivement rendu muet pour la raison : &6{reason}[RT]"),
		MUTE_TEMP_STAFF(						"&7Vous avez rendu muet &6{player} &7pendant une durée de &7{duration} &7pour la raison : &6{reason}"),
		MUTE_TEMP_PLAYER(						"&6{staff} &cvous a rendu muet pendant &6{duration} &cpour la raison : &6{reason}"),
		MUTE_ERROR_REASON(						"&cErreur : La raison est obligatoire."),
		MUTE_ERROR_CANCEL(						"&cErreur : Impossible de rendre muet &6{player} &cpour le moment."),
		MUTE_ERROR_EQUALS(						"&cErreur : Impossible de vous rendre muet."),
		MUTE_ERROR_NOEMPTY(						"&cErreur : &6{player} &cest déjà mute."),
		
		MUTE_DISABLE_CHAT_TEMP(					"&cVous êtes encore muet pendant &6{duration} &cpour &6{reason}&c."),
		MUTE_DISABLE_CHAT_UNLIMITED(			"&cVous muet indéfiniment &cpour &6{reason}&c."),
		MUTE_DISABLE_COMMAND_TEMP(				"&cVous ne pouvez pas utiliser cette commande quand vous êtes muet."),
		MUTE_DISABLE_COMMAND_UNLIMITED(			"&cVous ne pouvez pas utiliser cette commande quand vous êtes muet."),
		MUTE_CONNECTION_TEMP(					"&c&lVous êtes muet pendant encore &6{duration} &c&lpour avoir : &6{reason}"),
		MUTE_CONNECTION_UNLIMITED(				"&c&lVous êtes définitivement muet pour avoir : &6{reason}[RT]"),
		
		// UnMute
		UNMUTE_DESCRIPTION(						"Débanni le joueur du serveur"),
		UNMUTE_STAFF(							"&7Vous avez rendu la parole à &6{player}&7."),
		UNMUTE_PLAYER(							"&7Vous pouvez désormais reparler grâce à &6{staff}&7."),
		UNMUTE_ERROR_REASON(					"&cErreur : La raison est obligatoire."),
		UNMUTE_ERROR_EQUALS(					"&cErreur : Impossible de vous débannir vous-même."),
		UNMUTE_ERROR_EMPTY(						"&cErreur : &6{player} &cn'est pas muet."),
		UNMUTE_CANCEL(							"&cErreur : Impossible de débannir &6{player} &cpour le moment."),
		
		// Jails
		JAILS_DESCRIPTION(						"Gestion des prisons"),
		
		JAILS_LIST_DESCRIPTION(					"Affiche la liste des prisons"),
		JAILS_LIST_EMPTY(						"&7Aucune prison"),
		JAILS_LIST_TITLE(						"&aListe des prisons"),
		JAILS_LIST_LINE_DELETE(					"    &6&l➤  &6{jail} &7: {teleport} {delete}"),
		JAILS_LIST_LINE_DELETE_ERROR_WORLD(		"    &6&l➤  &6{jail} &7: {delete}"),
		JAILS_LIST_LINE(						"    &6&l➤  &6{jail} &7: {teleport}"),
		JAILS_LIST_TELEPORT(					"&a&nTéléporter"),
		JAILS_LIST_TELEPORT_HOVER(				"&cCliquez ici pour vous téléporter à la prison &6{jail}&c."),
		JAILS_LIST_DELETE(						"&c&nSupprimer"),
		JAILS_LIST_DELETE_HOVER(				"&cCliquez ici pour supprimer la prison &6{jail}&c."),
		
		JAILS_TELEPORT_DESCRIPTION(				"Téléporte à une prison"),
		JAILS_TELEPORT_PLAYER(					"&7Vous avez été téléporté à la prison &6{jail}&7."),
		JAILS_TELEPORT_PLAYER_ERROR(			"&cErreur : Impossible de trouver un position pour vous téléportez à la prison &6{jail}&c."),
		
		JAILS_DELETE_DESCRIPTION(				"Supprime une prison"),
		JAILS_DELETE_CONFIRMATION(				"&7Souhaitez-vous vraiment supprimer la prison &6{jail} &7: {confirmation}"),
		JAILS_DELETE_CONFIRMATION_VALID(		"&2&nConfirmer"),
		JAILS_DELETE_CONFIRMATION_VALID_HOVER(	"&cCliquez ici pour supprimer la prison &6{jail}&c."),
		JAILS_DELETE_DELETE(					"&7Vous avez supprimé la prison &6{jail}&7."),
		JAILS_DELETE_CANCEL(					"&cErreur : Impossible de supprimé la &6{jail} &cpour le moment."),
		
		JAILS_ADD_DESCRIPTION(					"Crée une prison"),
		JAILS_ADD_REPLACE(						"&7Vous avez redéfini la prison &6{jail}&7."),
		JAILS_ADD_NEW(							"&7Vous avez défini la prison &6{jail}&7."),
		JAILS_ADD_CANCEL_REPLACE(				"&cErreur : Impossible de redéfinir la prison &6{jail} &4pour le moment."),
		JAILS_ADD_CANCEL_NEW(					"&cErreur : Impossible de définir la prison &6{jail} &4pour le moment."),
		
		JAILS_SETRADIUS_DESCRIPTION(			"Modifie le rayon d'une prison"),
		JAILS_SETRADIUS_DEFAULT(				"&7Vous avez défini le rayon de la prison {jail} &7avec la valeur par défault (&6{radius} block(s)&7)"),
		JAILS_SETRADIUS_VALUE(					"&7Vous avez défini le rayon de la prison {jail} &7à &6{radius} block(s)&7."),
		JAILS_SETRADIUS_CANCEL_DEFAULT(			"&cErreur : Impossible de définir le rayon de la prison {jail} &cpour le moment."),
		JAILS_SETRADIUS_CANCEL_VALUE(			"&cErreur : Impossible de définir le rayon de la prison {jail} &cpour le moment."),
		
		// Auto
		SANCTION_DESCRIPTION(					"Sanctionne un joueur"),
		SANCTION_STAFF(							"&7Vous avez sanctionné &6{player} &7pour la raison : &6{reason}"),
		SANCTION_PLAYER(						"&6{staff} &c&lvous a sanctionné &6&l{type} &c&lpour avoir : &6{reason}"),
		SANCTION_ERROR_REASON(					"&cErreur : La raison est obligatoire."),
		SANCTION_ERROR_CANCEL(					"&cErreur : Impossible de rendre muet &6{player} &cpour le moment."),
		SANCTION_ERROR_EQUALS(					"&cErreur : Impossible de vous rendre muet."),
		SANCTION_ERROR_NOEMPTY(					"&cErreur : &6{player} &cest déjà muet."),
		SANCTION_ERROR_UNKNOWN(					"&cErreur : Il y n'y a pas de sanction &6{name}&c."),
		
		UNSANCTION_DESCRIPTION(					"Annule une sanction à un joueur"),
		
		SANCTIONS_DESCRIPTION(					"Affiche la liste des sanctions disponibles"),
		SANCTIONS_LIST_EMPTY(					"&7Aucune sanction"),
		SANCTIONS_LIST_TITLE(					"&aListe des sanctions"),
		SANCTIONS_LIST_LINE(					"    &6&l➤  &6{name}"),
		SANCTIONS_LIST_LINE_HOVER(				"&cCliquez ici pour avoir plus d'informations sur la sanction &6{name}"),
		SANCTIONS_REASON_UNKNOWN(				"&cErreur : Il y n'y a pas de sanction &6{name}&c."),
		SANCTIONS_REASON_TITLE(					"&aSanction &6{name}"),
		SANCTIONS_REASON_LINE_TEMP(				"    &6&l➤  &6Niveau &c{num} &6:[RT]"
																				  + "        &7Type : &c{type}[RT]"
																				  + "        &7Temps : &c{duration}[RT]"
																				  + "        &7Raison : &c{reason}"),
		SANCTIONS_REASON_LINE_TEMP_JAIL(		"    &6&l➤  &6Niveau &c{num} &6:[RT]"
																				  + "        &7Type : &c{type}[RT]"
																				  + "        &7Prison : &c{jail}[RT]"
																				  + "        &7Temps : &c{duration}[RT]"
																				  + "        &7Raison : &c{reason}"),
		SANCTIONS_REASON_LINE_UNLIMITED(		"    &6&l➤  &6Niveau &c{num} &6:[RT]"
																				  + "        &7Type : &c{type}[RT]"
																				  + "        &7Temps : &cUNLIMITED[RT]"
																				  + "        &7Raison : &c{reason}[RT]"),
		SANCTIONS_REASON_LINE_UNLIMITED_JAIL(	"    &6&l➤  &6Niveau &c{num} &6:"
																				  + "        &7Type : &c{type}[RT]"
																				  + "        &7Prison : &c{jail}[RT]"
																				  + "        &7Temps : &cUNLIMITED[RT]"
																				  + "        &7Raison : &c{reason}"),
		SANCTIONS_TYPE_BAN_PROFILE(				"Bannit le joueur"),
		SANCTIONS_TYPE_BAN_IP(					"Bannit l'adresse IP"),
		SANCTIONS_TYPE_BAN_PROFILE_AND_IP(		"Bannit le joueur et son adresse IP"),
		SANCTIONS_TYPE_MUTE(					"Rend muet le joueur"),
		SANCTIONS_TYPE_JAIL(					"Met le joueur en prison"),
		SANCTIONS_TYPE_MUTE_AND_JAIL(			"Rend muet emprisonne le joueur"),
		
		PERMISSIONS_COMMANDS_EXECUTE(""),
		PERMISSIONS_COMMANDS_HELP(""),
		PERMISSIONS_COMMANDS_RELOAD(""),
		PERMISSIONS_COMMANDS_PROFILE_EXECUTE(""),
		PERMISSIONS_COMMANDS_PROFILE_OTHERS(""),
		PERMISSIONS_COMMANDS_SANCTIONS_EXECUTE(""),
		PERMISSIONS_COMMANDS_SANCTION_EXECUTE(""),
		PERMISSIONS_COMMANDS_SANCTION_OFFLINE(""),
		PERMISSIONS_COMMANDS_UNSANCTION_EXECUTE(""),
		PERMISSIONS_COMMANDS_BAN_EXECUTE(""),
		PERMISSIONS_COMMANDS_BAN_UNLIMITED(""),
		PERMISSIONS_COMMANDS_BAN_OFFLINE(""),
		PERMISSIONS_COMMANDS_BANIP_EXECUTE(""),
		PERMISSIONS_COMMANDS_BANIP_UNLIMITED(""),
		PERMISSIONS_COMMANDS_BANIP_OFFLINE(""),
		PERMISSIONS_COMMANDS_MUTE_EXECUTE(""),
		PERMISSIONS_COMMANDS_MUTE_UNLIMITED(""),
		PERMISSIONS_COMMANDS_MUTE_OFFLINE(""),
		PERMISSIONS_COMMANDS_JAIL_EXECUTE(""),
		PERMISSIONS_COMMANDS_JAIL_UNLIMITED(""),
		PERMISSIONS_COMMANDS_JAIL_OFFLINE(""),
		PERMISSIONS_COMMANDS_JAILS_EXECUTE(""),
		PERMISSIONS_COMMANDS_JAILS_ADD(""),
		PERMISSIONS_COMMANDS_JAILS_DELETE(""),
		PERMISSIONS_COMMANDS_JAILS_LIST(""),
		PERMISSIONS_COMMANDS_JAILS_TELEPORT(""),
		PERMISSIONS_COMMANDS_JAILS_SETRADIUS(""),
		PERMISSIONS_COMMANDS_UNBAN_EXECUTE(""),
		PERMISSIONS_COMMANDS_UNBANIP_EXECUTE(""),
		PERMISSIONS_COMMANDS_UNMUTE_EXECUTE(""),
		PERMISSIONS_COMMANDS_UNJAIL_EXECUTE(""),;
		
		private final String path;
	    private final EMessageBuilder french;
	    private final EMessageBuilder english;
	    private EMessageFormat message;
	    private EMessageBuilder builder;
	    
	    private ESMessages(final String french) {   	
	    	this(EMessageFormat.builder().chat(new EFormatString(french), true));
	    }
	    
	    private ESMessages(final String french, final String english) {   	
	    	this(EMessageFormat.builder().chat(new EFormatString(french), true), 
	    		EMessageFormat.builder().chat(new EFormatString(english), true));
	    }
	    
	    private ESMessages(final EMessageBuilder french) {   	
	    	this(french, french);
	    }
	    
	    private ESMessages(final EMessageBuilder french, final EMessageBuilder english) {
	    	Preconditions.checkNotNull(french, "Le message '" + this.name() + "' n'est pas définit");
	    	
	    	this.path = this.resolvePath();	    	
	    	this.french = french;
	    	this.english = english;
	    	this.message = french.build();
	    }

	    public String getName() {
			return this.name();
		}
	    
		public String getPath() {
			return this.path;
		}

		public EMessageBuilder getFrench() {
			return this.french;
		}

		public EMessageBuilder getEnglish() {
			return this.english;
		}
		
		public EMessageFormat getMessage() {
			return this.message;
		}
		
		public EMessageBuilder getBuilder() {
			return this.builder;
		}
		
		public void set(EMessageBuilder message) {
			this.message = message.build();
			this.builder = message;
		}
	}
}
