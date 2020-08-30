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

	public void setMinBearerRatio(float minBearerRatio) {
		if (this.minBearerRatio < 0 || this.minBearerRatio > 1)
			throw new IllegalArgumentException("minBearerRatio(" + minBearerRatio + ") out of bounds (0-1)");
		this.minBearerRatio = minBearerRatio;
	}

	@Override
	public float getMinBearerRatio() {
		return minBearerRatio;
	}
	
	@Override
	public float getCurrentBearerRatio() {
		return bearerCount / (float) workerCount;
	}
	
	public void setMaxDiggerRatio(float maxDiggerRatio) {
		if (this.maxDiggerRatio < 0 || this.maxDiggerRatio > 1)
			throw new IllegalArgumentException("maxDiggerRatio(" + maxBricklayerRatio + ") out of bounds (0-1)");
		this.maxDiggerRatio = maxDiggerRatio;
	}

	@Override
	public float getMaxDiggerRatio() {
		return maxDiggerRatio;
	}
	
	@Override
	public float getCurrentDiggerRatio() {
		return diggerCount / (float) workerCount;
	}
	
	public void setMaxBricklayerRatio(float maxBricklayerRatio) {
		if (this.maxBricklayerRatio < 0 || this.maxBricklayerRatio > 1)
			throw new IllegalArgumentException("maxBricklayerRatio(" + maxBricklayerRatio + ") out of bounds (0-1)");
		this.maxBricklayerRatio = maxBricklayerRatio;
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
