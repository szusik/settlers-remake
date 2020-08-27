package jsettlers.logic.movable.strategies;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.EGoInDirectionMode;
import jsettlers.logic.movable.MovableStrategy;
import jsettlers.logic.movable.civilian.CivilianMovable;

public class CivilianStrategy<T extends CivilianMovable> extends MovableStrategy<T> {

	protected CivilianStrategy(T movable) {
		super(movable);
	}

	public void peacetimeAction() {}

	public void strategyStarted() {}

	public void strategyStopped() {}

	public void peacetimePathAborted(ShortPoint2D pathTarget) {}

	public boolean peacetimeCheckPathStepPreconditions(ShortPoint2D pathTarget, int step, EMoveToType moveToType) {
		return true;
	}

}
