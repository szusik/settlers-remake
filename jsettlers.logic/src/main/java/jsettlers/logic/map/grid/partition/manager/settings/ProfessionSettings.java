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
		if(getSettings(EMovableType.BEARER).getCurrentCount() <= getSettings(EMovableType.BEARER).getTargetCount()) {
			return false;
		}

		if(newType == EMovableType.DIGGER || newType == EMovableType.BRICKLAYER) {
			return getSettings(newType).getCurrentCount() < getSettings(newType).getTargetCount();
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

	public int getWorkerCount() {
		return workerCount;
	}
}
