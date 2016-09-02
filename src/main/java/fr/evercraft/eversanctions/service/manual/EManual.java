package fr.evercraft.eversanctions.service.manual;

import java.util.Optional;

import org.spongepowered.api.text.Text;

import fr.evercraft.everapi.services.sanction.manual.SanctionManual;

public class EManual implements SanctionManual {

	private final Long date_start;
	private Optional<Long> date_end;
	private final Text reason;
	private final String source;
	
	public EManual(final long date_start, final long date_end, final Text reason, final String source) {
		this.date_start = date_start;
		this.reason = reason;
		this.source = source;
		
		if (date_end <= 0) {
			this.date_end = Optional.empty();
		} else {
			this.date_end = Optional.of(date_end);
		}
	}

	@Override
	public Long getCreationDate() {
		return this.date_start;
	}
	
	@Override
	public Optional<Long> getExpirationDate() {
		return this.date_end;
	}
	
	@Override
	public Text getReason() {
		return this.reason;
	}
	
	@Override
	public String getSource() {
		return this.source;
	}
}
