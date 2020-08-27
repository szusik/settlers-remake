package jsettlers.logic.movable.civilian;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.map.grid.partition.manager.manageables.IManageableBricklayer;
import jsettlers.logic.map.grid.partition.manager.manageables.interfaces.IConstructableBuilding;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

public class BricklayerMovable extends CivilianMovable implements IManageableBricklayer {
	private static final float BRICKLAYER_ACTION_DURATION = 1f;

	private BricklayerMovable.EBricklayerState state = BricklayerMovable.EBricklayerState.JOBLESS;
	private IConstructableBuilding constructionSite;
	private ShortPoint2D bricklayerTargetPos;
	private EDirection lookDirection;

	public BricklayerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.BRICKLAYER, position, player, movable);
	}

	@Override
	public void strategyStarted() {
		jobFinished();
	}

	@Override
	public boolean setBricklayerJob(IConstructableBuilding constructionSite, ShortPoint2D bricklayerTargetPos, EDirection direction) {
		if (state == BricklayerMovable.EBricklayerState.JOBLESS) {
			this.constructionSite = constructionSite;
			this.bricklayerTargetPos = bricklayerTargetPos;
			this.lookDirection = direction;
			this.state = BricklayerMovable.EBricklayerState.INIT_JOB;
			return true;
		} else {
			return false;
		}
	}

	private void jobFinished() {
		this.state = BricklayerMovable.EBricklayerState.JOBLESS;
		this.bricklayerTargetPos = null;
		this.constructionSite = null;
		this.lookDirection = null;
		reportJobless();
	}

	private void reportJobless() {
		grid.addJobless(this);
	}

	@Override
	public void peacetimeAction() {
		switch (state) {
			case JOBLESS:
				break;

			case INIT_JOB:
				if (constructionSite.isBricklayerRequestActive() && super.goToPos(bricklayerTargetPos)) {
					this.state = BricklayerMovable.EBricklayerState.GOING_TO_POS;
				} else {
					jobFinished();
				}
				break;

			case GOING_TO_POS:
				super.lookInDirection(lookDirection);
				state = BricklayerMovable.EBricklayerState.BUILDING;
			case BUILDING:
				tryToBuild();
				break;

			case DEAD_OBJECT:
				break;
		}
	}

	private void tryToBuild() {
		if (constructionSite.isBricklayerRequestActive() && constructionSite.tryToTakeMaterial()) {
			super.playAction(EMovableAction.ACTION1, BRICKLAYER_ACTION_DURATION);
		} else {
			jobFinished();
		}
	}

	@Override
	public boolean peacetimeCheckPathStepPreconditions(ShortPoint2D pathTarget, int step, EMoveToType moveToType) {
		if (constructionSite == null || constructionSite.isBricklayerRequestActive()) {
			return true;
		} else {
			jobFinished();
			return false;
		}
	}

	@Override
	public void strategyStopped() {
		if (state == BricklayerMovable.EBricklayerState.JOBLESS) {
			grid.removeJobless(this);
		} else {
			abortJob();
		}

		state = BricklayerMovable.EBricklayerState.DEAD_OBJECT;
	}

	@Override
	public void peacetimePathAborted(ShortPoint2D pathTarget) {
		if (constructionSite != null) {
			abortJob();
			jobFinished(); // this job is done for us
		}
	}

	private void abortJob() {
		constructionSite.bricklayerRequestFailed(bricklayerTargetPos, lookDirection);
	}

	public enum EBricklayerState {
		JOBLESS,
		INIT_JOB,
		GOING_TO_POS,
		BUILDING,

		DEAD_OBJECT
	}
}
