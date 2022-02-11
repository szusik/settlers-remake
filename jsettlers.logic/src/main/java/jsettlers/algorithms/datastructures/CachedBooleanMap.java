package jsettlers.algorithms.datastructures;

import jsettlers.logic.constants.MatchConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class CachedBooleanMap<T> {

	private final Predicate<T> valueProvider;
	private final int recheckInterval;

	private final Map<T, Integer> knownValues = new HashMap<>();

	public CachedBooleanMap(Predicate<T> valueProvider, int recheckInterval) {
		this.valueProvider = valueProvider;
		this.recheckInterval = recheckInterval;
	}

	public boolean getValue(T key) {
		int recheckTime = MatchConstants.clock().getTime() + recheckInterval;

		boolean value = valueProvider.test(key);

		knownValues.put(key, (value?1:-1) * recheckTime);
		return value;
	}

	public boolean getValueCached(T key) {
		int time = MatchConstants.clock().getTime();
		int value = knownValues.getOrDefault(key, time);

		if(time >= Math.abs(value)) {
			return getValue(key);
		}

		return value > 0;
	}
}
