package jsettlers.logic.movable.civilian;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.RelativePoint;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.partition.manager.manageables.IManageableDigger;
import jsettlers.logic.map.grid.partition.manager.manageables.interfaces.IDiggerRequester;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

public class DiggerMovable extends CivilianMovable implements IManageableDigger {

	private IDiggerRequester requester;
	private EDiggerState state = EDiggerState.JOBLESS;

	public DiggerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.DIGGER, position, player, movable);
	}

	@Override
	public void strategyStarted() {
		reportJobless();
	}

	@Override
	public boolean setDiggerJob(IDiggerRequester requester) {
		if (state == EDiggerState.JOBLESS) {
			this.requester = requester;
			this.state = EDiggerState.INIT_JOB;
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void peacetimeAction() {
		switch (state) {
			case JOBLESS:
				break;

			case INIT_JOB:
				goToDiggablePosition();
				break;

			case PLAYING_ACTION:
				executeDigg();
				if (!requester.isDiggerRequestActive()) {
					grid.setMarked(position, false);
					reportJobless();
					break;
				}
			case GOING_TO_POS:
				if (needsToBeWorkedOn(position)) {
					super.playAction(EMovableAction.ACTION1, 1f);
					this.state = EDiggerState.PLAYING_ACTION;
				} else {
					goToDiggablePosition();
				}
				break;

			case DEAD_OBJECT:
				break;
		}
	}

	private void executeDigg() {
		grid.changeHeightTowards(position.x, position.y, requester.getAverageHeight());
	}

	private void goToDiggablePosition() {
		grid.setMarked(position, false);
		ShortPoint2D diggablePos = getDiggablePosition();
		if (diggablePos != null) {
			if (super.goToPos(diggablePos)) {
				state = EDiggerState.GOING_TO_POS;
				grid.setMarked(diggablePos, true);
			} else {
				reportJobless();
			}

		} else if (allPositionsFlattened()) { // all positions are flattened => building is finished
			reportJobless();

		} // else { not all positions are finished, so wait if one becomes unmarked or all are finished => do nothing
	}

	private boolean allPositionsFlattened() {
		for (RelativePoint relativePosition : requester.getBuildingVariant().getProtectedTiles()) {
			if (needsToBeWorkedOn(relativePosition.calculatePoint(requester.getPosition()))) {
				return false;
			}
		}
		return true;
	}

	private ShortPoint2D getDiggablePosition() {
		RelativePoint[] blockedTiles = requester.getBuildingVariant().getProtectedTiles();
		ShortPoint2D buildingPos = requester.getPosition();
		int offset = MatchConstants.random().nextInt(blockedTiles.length);

		for (int i = 0; i < blockedTiles.length; i++) {
			ShortPoint2D pos = blockedTiles[(i + offset) % blockedTiles.length].calculatePoint(buildingPos);
			if (!grid.isMarked(pos) && needsToBeWorkedOn(pos)) {
				return pos;
			}
		}
		return null;
	}

	private boolean needsToBeWorkedOn(ShortPoint2D pos) {
		return needsToChangeHeight(pos) || isNotFlattened(pos);
	}

	private boolean isNotFlattened(ShortPoint2D pos) {
		// some places can't be flattened
		if(!grid.canChangeLandscapeTo(pos.x, pos.y, ELandscapeType.FLATTENED)) return false;

		return grid.getLandscapeTypeAt(pos.x, pos.y) != ELandscapeType.FLATTENED;
	}

	private boolean needsToChangeHeight(ShortPoint2D pos) {
		return grid.getHeightAt(pos) != requester.getAverageHeight();
	}

	private void reportJobless() {
		this.state = EDiggerState.JOBLESS;
		this.requester = null;
		grid.addJobless(this);
	}

	@Override
	protected boolean peacetimeCheckPathStepPreconditions(ShortPoint2D pathTarget, int step, EMoveToType moveToType) {
		if (requester == null || requester.isDiggerRequestActive()) {
			return true;
		} else {
			if (state != EDiggerState.JOBLESS) {
				reportJobless();
			}

			if (pathTarget != null) {
				grid.setMarked(pathTarget, false);
			}
			return false;
		}
	}

	@Override
	protected void strategyStopped() {
		switch (state) {
			case JOBLESS:
				grid.removeJobless(this);
				break;
			case PLAYING_ACTION:
				grid.setMarked(position, false);
				break;
			default:
				break;
		}

		if (requester != null) {
			abortJob();
		}

		state = EDiggerState.DEAD_OBJECT;
	}

	@Override
	protected void peacetimePathAborted(ShortPoint2D pathTarget) {
		if (requester != null) {
			grid.setMarked(pathTarget, false);
			abortJob();
			reportJobless();
		}
	}

	private void abortJob() {
		requester.diggerRequestFailed();
	}


	private enum EDiggerState {
		JOBLESS,
		INIT_JOB,
		GOING_TO_POS,
		PLAYING_ACTION,

		DEAD_OBJECT
	}
}
