package jsettlers.logic.map.grid.partition.manager.settings;

import jsettlers.common.map.partition.ISingleProfessionLimit;

import java.io.Serializable;

public abstract class SingleProfessionLimit implements ISingleProfessionLimit, Serializable {

	protected final ProfessionSettings parent;

	protected int currentAmount;

	protected SingleProfessionLimit(ProfessionSettings parent) {
		this.parent = parent;

		this.currentAmount = 0;
	}

	public abstract void setLimit(int value, boolean relative);

	public final void incrementAmount() {
		currentAmount++;
	}

	public final void decrementCount() {
		currentAmount--;
	}

	public final void resetCount() {
		currentAmount = 0;
	}

	@Override
	public final int getCurrentCount() {
		return currentAmount;
	}

	@Override
	public final float getCurrentRatio() {
		return currentAmount / (float) parent.getWorkerCount();
	}
}
