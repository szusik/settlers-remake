package jsettlers.logic.movable.civilian;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.EGoInDirectionMode;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.ICivilianMovable;
import jsettlers.logic.player.Player;

public class CivilianMovable extends Movable implements ICivilianMovable {

	private boolean fleeing;

	private int searchesCounter = 0;
	private boolean turnNextTime;

	private int lastCheckedPathStep;
	private byte pathStepCheckedCounter;

	private boolean peacetimeActive;

	protected CivilianMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable replace) {
		super(grid, movableType, position, player, replace);

		peacetimeActive = true;
		fleeing = false;

		strategyStarted();
	}

	@Override
	protected final void action() {
		checkPlayerOfPosition();

		if(fleeing) {
			if(peacetimeActive) {
				peacetimeActive = false;
				strategyStopped();
			}

			findEvacuationPath();
		} else {
			if(!peacetimeActive) {
				peacetimeActive = true;
				strategyStarted();
			}

			peacetimeAction();
		}
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		if(peacetimeActive) {
			if(path != null) peacetimePathAborted(path.getTargetPosition());
			strategyStopped();
		}
	}

	@Override
	protected final boolean checkPathStepPreconditions(ShortPoint2D pathTarget, int step, EMoveToType moveToType) {
		checkPlayerOfPosition();

		if(peacetimeActive) {
			if(fleeing) {
				return false;
			}

			return peacetimeCheckPathStepPreconditions(pathTarget, step, moveToType);
		} else {
			return checkEvacuationPath(step);
		}
	}

	@Override
	protected final void pathAborted(ShortPoint2D pathTarget) {
		if(peacetimeActive) peacetimePathAborted(pathTarget);
	}


	protected void peacetimeAction() {
	}

	protected void strategyStarted() {
	}

	protected void strategyStopped() {
	}

	protected void peacetimePathAborted(ShortPoint2D pathTarget) {
	}

	protected boolean peacetimeCheckPathStepPreconditions(ShortPoint2D pathTarget, int step, EMoveToType moveToType) {
		return true;
	}

	private void findEvacuationPath() {
		if(searchesCounter > 120) {
			kill();
			return;
		}

		if (super.preSearchPath(true, position.x, position.y, Constants.MOVABLE_FLEEING_DIJKSTRA_RADIUS, ESearchType.VALID_FREE_POSITION)
				|| super.preSearchPath(false, position.x, position.y, Constants.MOVABLE_FLEEING_MAX_RADIUS, ESearchType.VALID_FREE_POSITION)) {
			lastCheckedPathStep = Integer.MIN_VALUE;
			super.followPresearchedPath();
		} else {
			EDirection currentDirection = getDirection();
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

		return !grid.isValidPosition(this, position.x, position.y) && pathStepCheckedCounter < 5;
	}

	@Override
	public void checkPlayerOfPosition() {
		// civilians are only allowed on their players ground => abort current task and flee to nearest own ground
		fleeing = grid.getPlayerAt(position) != player;
	}
}
