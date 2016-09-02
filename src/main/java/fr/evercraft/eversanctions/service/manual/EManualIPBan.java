package fr.evercraft.eversanctions.service.manual;

import java.net.InetAddress;

import org.spongepowered.api.text.Text;

import fr.evercraft.everapi.services.sanction.manual.SanctionManualIP;

public class EManualIPBan extends EManual implements SanctionManualIP.Ban {
	
	private final InetAddress address;
	
	public EManualIPBan(final InetAddress address,final long date_start, final long date_end, final Text reason, final String source) {
		super(date_start, date_end, reason, source);
		
		this.address = address;
	}

	@Override
	public InetAddress getAddress() {
		return this.address;
	}
}
