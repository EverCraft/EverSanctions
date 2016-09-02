package fr.evercraft.eversanctions.service.auto;

import java.util.Optional;

import fr.evercraft.everapi.services.sanction.auto.SanctionAuto;
import fr.evercraft.everapi.services.sanction.auto.SanctionAutoReason;
import fr.evercraft.everapi.services.sanction.auto.SanctionAutoType;

public class EAuto implements SanctionAuto {

	private final Long date_start;
	private Optional<Long> date_end;
	private final SanctionAutoReason reason;
	private final SanctionAutoType type;
	private final int level;
	private final String source;
	
	public EAuto(final long date_start, final long date_end, final SanctionAutoReason reason, final SanctionAutoType type, final int level, final String source) {
		this.date_start = date_start;
		this.reason = reason;
		this.type = type;
		this.level = level;
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
	public String getSource() {
		return this.source;
	}

	@Override
	public int getLevel() {
		return this.level;
	}

	@Override
	public SanctionAutoType getType() {
		return this.type;
	}
	
	@Override
	public SanctionAutoReason getReason() {
		return this.reason;
	}
}
