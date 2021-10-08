package jsettlers.logic.map.grid.partition.manager.settings;

import java.io.Serializable;

import jsettlers.common.map.partition.IProfessionSettings;
import jsettlers.common.movable.EMovableType;

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
		this(0.10f, 0.25f, 0.25f);
	}

	public ProfessionSettings(float minBearerRatio, float maxDiggerRatio, float maxBricklayerRatio) {
		this.minBearerRatio = minBearerRatio;
		this.maxDiggerRatio = maxDiggerRatio;
		this.maxBricklayerRatio = maxBricklayerRatio;
		resetCount();
	}

	@Override
	public boolean isBearerConversionAllowed(EMovableType newType) {
		if(getCurrentMovableCount(newType) > getTargetMovableCount(EMovableType.BEARER)) {
			return false;
		}

		if(newType == EMovableType.DIGGER || newType == EMovableType.BRICKLAYER) {
			return getCurrentMovableCount(newType) < getTargetMovableCount(newType);
		} else {
			return true;
		}
	}

	public void changeRatio(EMovableType movableType, float delta) {
		switch (movableType) {
			case BEARER:
				changeMinBearerRatio(delta);
				break;
			case DIGGER:
				changeMaxDiggerRatio(delta);
				break;
			case BRICKLAYER:
				changeMaxBricklayerRatio(delta);
				break;
			default:
				System.err.println("Unknown movable ratio!");
		}
	}

	private void changeMinBearerRatio(float delta) {
		float newMinBearerRatio = minBearerRatio + delta;
		if(newMinBearerRatio < 0) return;

		if(newMinBearerRatio + maxBricklayerRatio + maxDiggerRatio > 1) return;

		minBearerRatio = newMinBearerRatio;
	}

	private void changeMaxDiggerRatio(float delta) {
		float newMaxDiggerRatio = maxDiggerRatio + delta;
		if(newMaxDiggerRatio < 0) return;

		if(minBearerRatio + maxBricklayerRatio + newMaxDiggerRatio > 1) return;

		maxDiggerRatio = newMaxDiggerRatio;
	}

	private void changeMaxBricklayerRatio(float delta) {
		float newMaxBricklayerRatio = maxBricklayerRatio + delta;
		if(newMaxBricklayerRatio < 0) return;

		if(minBearerRatio + newMaxBricklayerRatio + maxDiggerRatio > 1) return;

		maxBricklayerRatio = newMaxBricklayerRatio;
	}

	public void resetCount() {
		this.workerCount = 0;
		this.bearerCount = 0;
		this.diggerCount = 0;
		this.bricklayerCount = 0;
	}

	public void decrementBearerCount() {
		this.bearerCount--;
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
	public float getTargetMovableRatio(EMovableType movableType) {
		switch (movableType) {
			case BEARER:
				return minBearerRatio;
			case DIGGER:
				return maxDiggerRatio;
			case BRICKLAYER:
				return maxBricklayerRatio;
			default:
				return Float.POSITIVE_INFINITY;
		}
	}

	@Override
	public int getTargetMovableCount(EMovableType movableType) {
		return (int) getTargetMovableRatio(movableType) * workerCount;
	}

	@Override
	public float getCurrentMovableRatio(EMovableType movableType) {
		return getCurrentMovableCount(movableType) / (float) workerCount;
	}

	@Override
	public int getCurrentMovableCount(EMovableType movableType) {
		switch (movableType) {
			case BEARER:
				return bearerCount;
			case DIGGER:
				return diggerCount;
			case BRICKLAYER:
				return bricklayerCount;
			default:
				return -1;
		}
	}

	@Override
	public String toString() {
		return String.format("ProfessionSettings [minBearerRatio=%s, maxDiggerRatio=%s, maxBricklayerRatio=%s, workerCount=%s, bearerCount=%s, diggerCount=%s, bricklayerCount=%s]", minBearerRatio,
				maxDiggerRatio, maxBricklayerRatio, workerCount, bearerCount, diggerCount, bricklayerCount);
	}
}
