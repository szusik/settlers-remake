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

	private int searchesCounter = 0;
	private boolean turnNextTime;

	private int lastCheckedPathStep;
	private byte pathStepCheckedCounter;

	private boolean peactimeActive = true;

	protected CivilianStrategy(T movable) {
		super(movable);

		strategyStarted();
	}

	@Override
	protected final void action() {
		movable.checkPlayerOfPosition();

		if(movable.isFleeing()) {
			if(peactimeActive) {
				peactimeActive = false;
				strategyStopped();
			}

			findEvacuationPath();
		} else {
			if(!peactimeActive) {
				peactimeActive = true;
				strategyStarted();
			}

			peacetimeAction();
		}
	}

	@Override
	protected final void strategyKilledEvent(ShortPoint2D pathTarget) {
		if(!peactimeActive) return;

		if(pathTarget != null) peacetimePathAborted(pathTarget);
		strategyStopped();
	}

	@Override
	protected final boolean checkPathStepPreconditions(ShortPoint2D pathTarget, int step, EMoveToType moveToType) {
		movable.checkPlayerOfPosition();

		if(peactimeActive) {
			if(movable.isFleeing()) {
				return false;
			}

			return peacetimeCheckPathStepPreconditions(pathTarget, step, moveToType);
		} else {
			return checkEvacuationPath(step);
		}
	}

	@Override
	protected final void pathAborted(ShortPoint2D pathTarget) {
		if(peactimeActive) peacetimePathAborted(pathTarget);
	}


	protected void peacetimeAction() {}

	protected void strategyStarted() {}

	protected void strategyStopped() {}

	protected void peacetimePathAborted(ShortPoint2D pathTarget) {}

	protected boolean peacetimeCheckPathStepPreconditions(ShortPoint2D pathTarget, int step, EMoveToType moveToType) {
		return true;
	}

	private void findEvacuationPath() {
		if(searchesCounter > 120) {
			movable.kill();
			return;
		}

		ShortPoint2D position = movable.getPosition();

		if (super.preSearchPath(true, position.x, position.y, Constants.MOVABLE_FLEEING_DIJKSTRA_RADIUS, ESearchType.VALID_FREE_POSITION)
				|| super.preSearchPath(false, position.x, position.y, Constants.MOVABLE_FLEEING_MAX_RADIUS, ESearchType.VALID_FREE_POSITION)) {
			lastCheckedPathStep = Integer.MIN_VALUE;
			super.followPresearchedPath();
		} else {
			EDirection currentDirection = movable.getDirection();
			EDirection newDirection;
			if (turnNextTime || MatchConstants.random().nextFloat() < 0.10) {
				turnNextTime = false;
				newDirection = currentDirection.getNeighbor(MatchConstants.random().nextInt(-1, 1));
			} else {
				newDirection = currentDirection;
			}

			if (super.goInDirection(newDirection, EGoInDirectionMode.GO_IF_FREE)) {
				turnNextTime = MatchConstants.random().nextInt(7) == 0;
			} else {
				super.lookInDirection(newDirection);
				turnNextTime = true;
			}
		}

		searchesCounter++;
	}

	private boolean checkEvacuationPath(int step) {
		if (lastCheckedPathStep == step) {
			pathStepCheckedCounter++;
			searchesCounter++;
		} else {
			pathStepCheckedCounter = 0;
			lastCheckedPathStep = (short) step;
		}

		return !super.isValidPosition(movable.getPosition()) && pathStepCheckedCounter < 5;
	}

}
