package jsettlers.logic.map.grid.partition.manager.settings;

import jsettlers.common.map.partition.ISingleProfessionLimit;

public class AbsoluteProfessionLimit extends SingleProfessionLimit {

	private int targetCount;

	public AbsoluteProfessionLimit(ProfessionSettings parent, int initialTargetCount) {
		super(parent);

		this.targetCount = initialTargetCount;
	}

	public AbsoluteProfessionLimit(ProfessionSettings parent, ISingleProfessionLimit predecessor) {
		super(parent, predecessor.getCurrentCount());

		this.targetCount = predecessor.getTargetCount();
	}

	@Override
	public int getTargetCount() {
		return targetCount;
	}

	@Override
	public float getTargetRatio() {
		return targetCount / (float) parent.getWorkerCount();
	}

	@Override
	public void setLimit(int value, boolean relative) {
		if(relative) {
			targetCount += value;
		} else {
			targetCount = value;
		}

		if(targetCount < 0) {
			targetCount = 0;
		}
	}

	@Override
	public String toString() {
		return "AbsoluteProfessionLimit{" +
				"targetCount=" + targetCount +
				", parent=" + parent +
				", currentAmount=" + currentAmount +
				'}';
	}

	@Override
	public boolean isRelative() {
		return false;
	}
}
