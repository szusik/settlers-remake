package jsettlers.logic.map.grid.partition.manager.settings;

import java.io.Serializable;

import jsettlers.common.map.partition.IProfessionSettings;
import jsettlers.common.movable.EMovableType;

public class ProfessionSettings implements Serializable, IProfessionSettings {

	private static final long serialVersionUID = -3565901631440791613L;

	private SingleProfessionLimit bearerSettings;
	private SingleProfessionLimit diggerSettings;
	private SingleProfessionLimit bricklayerSettings;

	private int workerCount;

	public ProfessionSettings() {
		this(0.10f, 0.25f, 0.25f);
	}

	public ProfessionSettings(float minBearerRatio, float maxDiggerRatio, float maxBricklayerRatio) {

		bearerSettings = new RelativeProfessionLimit(this, minBearerRatio);
		diggerSettings = new RelativeProfessionLimit(this, maxDiggerRatio);
		bricklayerSettings = new RelativeProfessionLimit(this, maxBricklayerRatio);

		resetCount();
	}

	public boolean isBearerConversionAllowed(EMovableType newType) {
		if(getCurrentMovableCount(EMovableType.BEARER) <= getTargetMovableCount(EMovableType.BEARER)) {
			return false;
		}

		if(newType == EMovableType.DIGGER || newType == EMovableType.BRICKLAYER) {
			return getCurrentMovableCount(newType) < getTargetMovableCount(newType);
		} else {
			return true;
		}
	}

	@Override
	public SingleProfessionLimit getSettings(EMovableType movableType) {
		switch (movableType) {
			case BEARER:
				return bearerSettings;
			case DIGGER:
				return diggerSettings;
			case BRICKLAYER:
				return bricklayerSettings;
			default:
				return null;
		}
	}

	public void changeRatio(EMovableType movableType, int value, boolean relative) {
		getSettings(movableType).setLimit(value, relative);
	}

	public void resetCount() {
		this.workerCount = 0;
		bearerSettings.resetCount();
		diggerSettings.resetCount();
		bricklayerSettings.resetCount();
	}
	
	public void increment(EMovableType movableType) {
		if(!movableType.isPlayerControllable()) workerCount++;

		SingleProfessionLimit settings = getSettings(movableType);
		if(settings != null) {
			settings.incrementAmount();
		}
	}
	
	@Override
	public float getTargetMovableRatio(EMovableType movableType) {
		return getSettings(movableType).getTargetRatio();
	}

	@Override
	public int getTargetMovableCount(EMovableType movableType) {
		return getSettings(movableType).getTargetCount();
	}

	@Override
	public float getCurrentMovableRatio(EMovableType movableType) {
		return getSettings(movableType).getCurrentRatio();
	}

	@Override
	public int getCurrentMovableCount(EMovableType movableType) {
		return getSettings(movableType).getCurrentCount();
	}

	public int getWorkerCount() {
		return workerCount;
	}
}
