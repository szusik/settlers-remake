package jsettlers.logic.map.grid.partition.manager.settings;

import jsettlers.common.map.partition.ISingleProfessionLimit;

public class RelativeProfessionLimit extends SingleProfessionLimit {

	private float targetRatio;

	protected RelativeProfessionLimit(ProfessionSettings parent, float initialTargetRatio) {
		super(parent);

		targetRatio = initialTargetRatio;
	}

	protected RelativeProfessionLimit(ProfessionSettings parent, ISingleProfessionLimit predecessor) {
		super(parent);

		targetRatio = predecessor.getTargetRatio();
	}

	@Override
	public int getTargetCount() {
		return (int) (targetRatio * parent.getWorkerCount());
	}

	@Override
	public float getTargetRatio() {
		return targetRatio;
	}

	@Override
	public void setLimit(int value, boolean relative) {
		if(relative) {
			targetRatio += value/100f;
		} else {
			targetRatio = value/100f;
		}
	}

	@Override
	public String toString() {
		return "RelativeProfessionLimit{" +
				"targetRatio=" + targetRatio +
				", parent=" + parent +
				", currentAmount=" + currentAmount +
				'}';
	}
}
