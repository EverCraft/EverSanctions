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
import java.util.Optional;

import fr.evercraft.everapi.plugin.file.EConfig;
import fr.evercraft.everapi.plugin.file.EMessage;
import fr.evercraft.everapi.sponge.UtilsDate;

public class ESConfig extends EConfig {

	public ESConfig(final EverSanctions plugin) {
		super(plugin);
	}
	
	public void reload() {
		super.reload();
		this.plugin.getLogger().setDebug(this.isDebug());
	}
	
	@Override
	public void loadDefault() {
		addDefault("DEBUG", false, "Displays plugin performance in the logs");
		addDefault("LANGUAGE", EMessage.FRENCH, "Select language messages", "Examples : ", "  French : FR_fr", "  English : EN_en");
		
		// SQL
		addComment("SQL", 				"Save the user in a database : ",
										" H2 : \"jdbc:h2:" + this.plugin.getPath().toAbsolutePath() + "/data\"",
										" SQL : \"jdbc:mysql://[login[:password]@]<host>:<port>/<database>\"",
										" Default users are saving in the 'data.mv.db'");
		addDefault("SQL.enable", false);
		addDefault("SQL.url", "jdbc:mysql://root:password@localhost:3306/minecraft");
		addDefault("SQL.prefix", "eversanctions_");
		
		
		
		addDefault("auto.sanctions", "5y");
		
		addDefault("manual.ban.max-time", "5y");
		
		addDefault("manual.ban-ip.max-time", "5y");
		
		addDefault("manual.jail.radius", 20);
		addDefault("manual.jail.max-time", "5y");
		addDefault("manual.jail.commands-enable", Arrays.asList("profile"));
		
		addDefault("manual.mute.max-time", "5y");
		addDefault("manual.mute.commands-disable", Arrays.asList("msg", "reply", "mail"));
	}
	
	/*
	 * Ban
	 */
	
	public Optional<Long> getBanMaxTime() {
		return UtilsDate.parseDateDiff(this.get("manual.ban.max-time").getString("5y"), true);
	}
	
	/*
	 * Ban-IP
	 */
	
	public Optional<Long> getBanIpMaxTime() {
		return UtilsDate.parseDateDiff(this.get("manual.ban-ip.max-time").getString("5y"), true);
	}
	
	/*
	 * Jail
	 */
	
	public Optional<Long> getJailMaxTime() {
		return UtilsDate.parseDateDiff(this.get("manual.jail.max-time").getString("5y"), true);
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
		return UtilsDate.parseDateDiff(this.get("manual.mute.max-time").getString("5y"), true);
	}
	
	public List<String> getMuteCommandsDisable() {
		return this.getListString("manual.mute.commands-disable");
	}
}
