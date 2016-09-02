package fr.evercraft.eversanctions.service.auto;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

import fr.evercraft.everapi.services.sanction.auto.SanctionAutoLevel;
import fr.evercraft.everapi.services.sanction.auto.SanctionAutoReason;

public class EAutoReason implements SanctionAutoReason {
	
	private final String name;
	private final ConcurrentSkipListMap<Integer, EAutoLevel> levels;
	
	public EAutoReason(final String name, Map<Integer, EAutoLevel> levels) {
		this.name = name;
		this.levels = new ConcurrentSkipListMap<Integer, EAutoLevel>((Integer o1, Integer o2) -> o1.compareTo(o2));
	}

	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public Optional<SanctionAutoLevel> getLevel(int level) {
		if(level < 0) {
			new IllegalArgumentException("Level is negative");
		}
		
		return Optional.ofNullable(this.levels.floorEntry(level).getValue());
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Collection<SanctionAutoLevel> getLevels() {
		return (Collection) this.levels.values();
	}
	
}