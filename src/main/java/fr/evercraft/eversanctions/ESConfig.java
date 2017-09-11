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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import fr.evercraft.everapi.java.UtilsInteger;
import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.plugin.file.EConfig;
import fr.evercraft.everapi.services.jail.Jail;
import fr.evercraft.everapi.services.sanction.SanctionService;
import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;
import fr.evercraft.everapi.sponge.UtilsDate;
import fr.evercraft.eversanctions.service.auto.EAutoLevel;
import fr.evercraft.eversanctions.service.auto.EAutoReason;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;

public class ESConfig extends EConfig<EverSanctions> {

	public ESConfig(final EverSanctions plugin) {
		super(plugin);
	}
	
	public void reload() {
		super.reload();
		this.plugin.getELogger().setDebug(this.isDebug());
	}
	
	@Override
	public List<String> getHeader() {
		return 	Arrays.asList(	"####################################################### #",
								"                EverSanctions (By rexbut)                #",
								"    For more information : https://docs.evercraft.fr     #",
								"####################################################### #");
	}
	
	@Override
	public void loadDefault() {
		this.configDefault();
		this.sqlDefault();
		
		// Manual
		addDefault("manual.ban.max-time", "5y");
		
		addDefault("manual.ban-ip.max-time", "5y");
		
		addDefault("manual.jail.radius", 20);
		addDefault("manual.jail.max-time", "5y");
		addDefault("manual.jail.commands-enable", Arrays.asList("profile"));
		
		addDefault("manual.mute.max-time", "5y");
		addDefault("manual.mute.commands-disable", Arrays.asList("msg", "reply", "mail"));
		
		
		// Auto
		Map<String, Object> types = new HashMap<String, Object>();
		Map<String, Object> types_values = new HashMap<String, Object>();
		Map<String, Object> levels = new HashMap<String, Object>();
		Map<String, String> levels_values = new HashMap<String, String>();
		
		// Cheat
		levels_values.put("type", "BAN_PROFILE");
		levels_values.put("temp", "15d");
		levels.put("1", levels_values);
		
		levels_values = new HashMap<String, String>();
		levels_values.put("type", "BAN_PROFILE");
		levels_values.put("temp", "30d");
		levels.put("2", levels_values);
		
		levels_values = new HashMap<String, String>();
		levels_values.put("type", "BAN_PROFILE");
		levels_values.put("temp", "UNLIMITED");
		levels.put("3", levels_values);
		
		types_values.put("reason", "Pour avoir cheat sur le serveur");
		types_values.put("levels", levels);
		
		types.put("CHEAT", types_values);
		
		// DDOS
		types_values = new HashMap<String, Object>();
		types_values.put("reason", "Pour avoir ménacé de DDOS le serveur ou pour l'avoir fait.");
		types_values.put("type", "BAN_PROFILE_AND_IP");
		types_values.put("temp", "UNLIMITED");
		
		types.put("DDOS", types_values);
		
		// Jail
		types_values = new HashMap<String, Object>();
		levels = new HashMap<String, Object>();
		levels_values = new HashMap<String, String>();
		
		addDefault("sanctions", types);
	}
	
	/*
	 * Ban
	 */
	
	public Optional<Long> getBanMaxTime() {
		return UtilsDate.parseDuration(this.get("manual.ban.max-time").getString("5y"), true);
	}
	
	/*
	 * Ban-IP
	 */
	
	public Optional<Long> getBanIpMaxTime() {
		return UtilsDate.parseDuration(this.get("manual.ban-ip.max-time").getString("5y"), true);
	}
	
	/*
	 * Jail
	 */
	
	public Optional<Long> getJailMaxTime() {
		return UtilsDate.parseDuration(this.get("manual.jail.max-time").getString("5y"), true);
	}
	
	public int getJailRadius() {
		return this.get("manual.jail.radius").getInt(20);
	}
	
	public List<String> getJailCommandsEnable() {
		return this.getListString("manual.jail.commands-enable");
	}
	
	/*
	 * Mute
	 */
	
	public Optional<Long> getMuteMaxTime() {
		return UtilsDate.parseDuration(this.get("manual.mute.max-time").getString("5y"), true);
	}
	
	public List<String> getMuteCommandsDisable() {
		return this.getListString("manual.mute.commands-disable");
	}
	
	public Map<String, EAutoReason> getSanctions() {
		Map<String, EAutoReason> sanctions = new HashMap<String, EAutoReason>();
		for (Entry<Object, ? extends CommentedConfigurationNode> node : this.get("sanctions").getChildrenMap().entrySet()) {
			if (node.getKey() instanceof String) {
				String name = (String) node.getKey();
				CommentedConfigurationNode config = node.getValue();
				if (config == null) {
					this.plugin.getELogger().warn("Config : Il n'y a aucune sanction automatique de définie !");
				}
				String type_default = config.getNode("type").getString("");
				String reason_default = config.getNode("reason").getString("Reason ...");
				String temp_default = config.getNode("temp").getString(SanctionService.UNLIMITED);
				String jail_default = config.getNode("jail").getString("");
				final Map<Integer, EAutoLevel> levels = new HashMap<Integer, EAutoLevel>();
				CommentedConfigurationNode config_levels = config.getNode("levels");
				
				
				if(config_levels.isVirtual()) {
					try {
						SanctionAuto.Type type = SanctionAuto.Type.valueOf(type_default);
						
						Optional<String> duration = Optional.empty();
						if (!temp_default.equalsIgnoreCase(SanctionService.UNLIMITED)) {
							duration = Optional.of(temp_default);
						}
						
						if (type.equals(SanctionAuto.Type.JAIL) || type.equals(SanctionAuto.Type.MUTE_AND_JAIL)) {
							Optional<Jail> jail = this.plugin.getJailService().get(jail_default);
							if (jail.isPresent()) {
								levels.put(1, new EAutoLevel(type, duration, EChat.of(reason_default), jail.get()));
							} else {
								this.plugin.getELogger().warn("Config : Il n'y a de prison '{" + jail_default + "}' (sanctionauto='" + name + "')");
							}
						} else {
							levels.put(1, new EAutoLevel(type, duration, EChat.of(reason_default)));
						}
					} catch (IllegalArgumentException e) {
						this.plugin.getELogger().warn("Config : Type de sanction inconnu '" + type_default + "' : (sanctionauto='" + name + "')");
					}
				} else {		
					config_levels.getChildrenMap().forEach((key_levels, config_level) -> {
						if (key_levels instanceof String) {
							Optional<Integer> level = UtilsInteger.parseInt((String) key_levels);
							if (level.isPresent()) {
								try {
									SanctionAuto.Type type = SanctionAuto.Type.valueOf(config_level.getNode("type").getString(type_default));
									String reason = config_level.getNode("reason").getString(reason_default);
									String temp = config_level.getNode("temp").getString(temp_default);
									String jail_name = config_level.getNode("jail").getString(jail_default);
																		
									Optional<String> duration = Optional.empty();
									if (!temp.equalsIgnoreCase(SanctionService.UNLIMITED)) {
										duration = Optional.of(temp);
									}
									
									if (type.equals(SanctionAuto.Type.JAIL) || type.equals(SanctionAuto.Type.MUTE_AND_JAIL)) {
										Optional<Jail> jail = this.plugin.getJailService().get(jail_name);
										if (jail.isPresent()) {
											levels.put(level.get(), new EAutoLevel(type, duration, EChat.of(reason), jail.get()));
										} else {
											this.plugin.getELogger().warn("Config : Il n'y a de prison '{" + jail_default + "}' (sanctionauto='" + name + "';level='" + level.get() + "')");
										}
									} else {
										levels.put(level.get(), new EAutoLevel(type, duration, EChat.of(reason)));
									}
								} catch (IllegalArgumentException e) {
									this.plugin.getELogger().warn("Config : Type de sanction inconnu '" + type_default + "' : (sanctionauto='" + name + "';level='" + level.get() + "')");
								}
							}
						}
					});
				}
				
				if (!levels.isEmpty()) {
					sanctions.put(name.toLowerCase(), new EAutoReason(name, levels));
				} else {
					
				}
				
			}
		}
		
		
		return sanctions;
	}
}
