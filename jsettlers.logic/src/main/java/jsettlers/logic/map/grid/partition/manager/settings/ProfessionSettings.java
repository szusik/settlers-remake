package jsettlers.logic.map.grid.partition.manager.settings;

import java.io.Serializable;

import jsettlers.common.map.partition.IProfessionSettings;
import jsettlers.common.movable.EMovableType;
import jsettlers.logic.movable.interfaces.ILogicMovable;

public class ProfessionSettings implements Serializable, IProfessionSettings {

	private static final long serialVersionUID = -3565901631440791614L;

	private float minBearerRatio;
	private float maxDiggerRatio;
	private float maxBricklayerRatio;

	private transient int workerCount;
	private transient int bearerCount;
	private transient int diggerCount;
	private transient int bricklayerCount;

	public ProfessionSettings() {
		this(0.25f, 0.25f, 0.25f);
	}

	public ProfessionSettings(float minBearerRatio, float maxDiggerRatio, float maxBricklayerRatio) {
		this.minBearerRatio = minBearerRatio;
		this.maxDiggerRatio = maxDiggerRatio;
		this.maxBricklayerRatio = maxBricklayerRatio;
		resetCount();
	}

	public void resetCount() {
		this.workerCount = 0;
		this.bearerCount = 0;
		this.diggerCount = 0;
		this.bricklayerCount = 0;
	}

	@Override
	public boolean isBaererRatioFulfilled() {
		return getCurrentBearerRatio() >= minBearerRatio;
	}

	@Override
	public boolean isDiggerRatioFulfilled() {
		return getCurrentDiggerRatio() < maxDiggerRatio;
	}

	@Override
	public boolean isBricklayerRatioFulfilled() {
		return getCurrentBricklayerRatio() < maxBricklayerRatio;
	}

	public void changeMinBearerRatio(float delta) {
		float newMinBearerRatio = minBearerRatio + delta;
		if(newMinBearerRatio < 0) return;

		if(newMinBearerRatio + maxBricklayerRatio + maxDiggerRatio > 1) return;

		minBearerRatio = newMinBearerRatio;
	}

	@Override
	public float getMinBearerRatio() {
		return minBearerRatio;
	}
	
	@Override
	public float getCurrentBearerRatio() {
		return bearerCount / (float) workerCount;
	}
	
	public void changeMaxDiggerRatio(float delta) {
		float newMaxDiggerRatio = maxDiggerRatio + delta;
		if(newMaxDiggerRatio < 0) return;

		if(minBearerRatio + maxBricklayerRatio + newMaxDiggerRatio > 1) return;

		maxDiggerRatio = newMaxDiggerRatio;
	}

	@Override
	public float getMaxDiggerRatio() {
		return maxDiggerRatio;
	}
	
	@Override
	public float getCurrentDiggerRatio() {
		return diggerCount / (float) workerCount;
	}
	
	public void changeMaxBricklayerRatio(float delta) {
		float newMaxBricklayerRatio = maxBricklayerRatio + delta;
		if(newMaxBricklayerRatio < 0) return;

		if(minBearerRatio + newMaxBricklayerRatio + maxDiggerRatio > 1) return;

		maxBricklayerRatio = newMaxBricklayerRatio;
	}

	@Override
	public float getMaxBricklayerRatio() {
		return maxBricklayerRatio;
	}
	
	@Override
	public float getCurrentBricklayerRatio() {
		return bricklayerCount / (float) workerCount;
	}

	@Override
	public int getWorkerCount() {
		return workerCount;
	}
	
	public void increment(EMovableType movableType) {
		if(!movableType.isPlayerControllable()) workerCount++;

		switch (movableType) {
			case BEARER:
				bearerCount++;
				break;
			case DIGGER:
				diggerCount++;
				break;
			case BRICKLAYER:
				bricklayerCount++;
				break;
			default:
				break;
		}
	}
	
	@Override
	public int getBearerCount() {
		return bearerCount;
	}

	public void decrementBearerCount() {
		this.bearerCount--;
	}

	@Override
	public int getDiggerCount() {
		return diggerCount;
	}

	@Override
	public int getBricklayerCount() {
		return bricklayerCount;
	}

	@Override
	public String toString() {
		return String.format("ProfessionSettings [minBearerRatio=%s, maxDiggerRatio=%s, maxBricklayerRatio=%s, workerCount=%s, bearerCount=%s, diggerCount=%s, bricklayerCount=%s]", minBearerRatio,
				maxDiggerRatio, maxBricklayerRatio, workerCount, bearerCount, diggerCount, bricklayerCount);
	}
}
