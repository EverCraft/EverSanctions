package fr.evercraft.eversanctions.service.manual;

import java.util.Optional;

import org.spongepowered.api.text.Text;

import fr.evercraft.everapi.services.sanction.manual.SanctionManualProfile;

public class EManualProfileJail extends EManual implements SanctionManualProfile.Jail {
	
	public EManualProfileJail(final long date_start, final long date_end, final Text reason, final String source) {
		super(date_start, date_end, reason, source);
	}

	@Override
	public String getJailName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<Jail> getJail() {
		// TODO Auto-generated method stub
		return null;
	}
}
