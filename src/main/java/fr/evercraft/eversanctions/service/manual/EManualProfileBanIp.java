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
package fr.evercraft.eversanctions.service.manual;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.BanTypes;
import org.spongepowered.api.util.ban.Ban.Builder;
import org.spongepowered.api.util.ban.Ban.Ip;

import fr.evercraft.everapi.plugin.EChat;
import fr.evercraft.everapi.services.sanction.manual.SanctionManualProfile;

public class EManualProfileBanIp extends EManualProfile implements SanctionManualProfile.BanIp {
	
	private InetAddress address;
	
	public EManualProfileBanIp(final UUID uuid, final InetAddress address, final long date_start, final Optional<Long> expiration, final Text reason, final String source) {
		this(uuid, address, date_start, expiration, reason, source, Optional.empty(), Optional.empty(), Optional.empty());
	}
	
	public EManualProfileBanIp(final UUID uuid, final InetAddress address, final long date_start, final Optional<Long> expiration, final Text reason, final String source, 
			final Optional<Long> pardon_date, final Optional<Text> pardon_reason, final Optional<String> pardon_source) {
		super(uuid, date_start, expiration, reason, source, pardon_date, pardon_reason, pardon_source);
		
		this.address = address;
	}

	@Override
	public InetAddress getAddress() {
		return this.address;
	}
	
	@Override
	public Ip getBan() {
		Builder builder =  org.spongepowered.api.util.ban.Ban.builder()
				.type(BanTypes.IP)
				.reason(this.getReason())
				.startDate(Instant.ofEpochMilli(this.getCreationDate()))
				.address(this.address)
				.source(EChat.of(this.getSource()));
		
		if(this.getExpirationDate().isPresent()) {
			builder = builder.expirationDate(Instant.ofEpochMilli(this.getExpirationDate().get()));
		}
		return (Ip) builder.build();
	}
}
