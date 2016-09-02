package fr.evercraft.eversanctions.service.auto;

import java.util.Optional;

import fr.evercraft.everapi.services.sanction.Jail;
import fr.evercraft.everapi.services.sanction.auto.SanctionAutoLevel;
import fr.evercraft.everapi.services.sanction.auto.SanctionAutoType;

public class EAutoLevel implements SanctionAutoLevel {
	
	private final SanctionAutoType type;
	private final Optional<Long> duration;
	private final String reason;
	private final Optional<Jail> jail;
	
	public EAutoLevel(final SanctionAutoType type, final Optional<Long> duration, String reason) {
		this(type, duration, reason, null);
	}
	
	public EAutoLevel(final SanctionAutoType type, final Optional<Long> duration, String reason, Jail jail) {
		this.type = type;
		this.duration = duration;
		this.reason = reason;
		this.jail = Optional.ofNullable(jail);
	}

	@Override
	public SanctionAutoType getType() {
		return this.type;
	}

	@Override
	public Optional<Long> getDuration() {
		return this.duration;
	}

	@Override
	public String getReason() {
		return this.reason;
	}

	@Override
	public Optional<Jail> getJail() {
		return this.jail;
	}
}
