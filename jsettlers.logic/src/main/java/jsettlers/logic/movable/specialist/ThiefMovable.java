package jsettlers.logic.movable.specialist;

import java.util.BitSet;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.movable.interfaces.IThiefMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

public class ThiefMovable extends AttackableHumanMovable implements IThiefMovable {

	private BitSet uncoveredBy = new BitSet();

	private static final float ACTION1_DURATION = 1f;

	private EThiefState state = EThiefState.JOBLESS;

	private ShortPoint2D returnPos = null;
	private EMaterialType stolenMaterial = EMaterialType.NO_MATERIAL;

	public ThiefMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.THIEF, position, player, movable);
	}


	@Override
	public boolean isUncoveredBy(byte teamId) {
		return uncoveredBy.get(teamId);
	}

	@Override
	public void uncoveredBy(byte teamId) {
		uncoveredBy.set(teamId);
	}


	@Override
	protected void action() {
		if(isOnOwnGround() && stolenMaterial != EMaterialType.NO_MATERIAL) {
			grid.dropMaterial(position, stolenMaterial, true, false);
			setMaterial(EMaterialType.NO_MATERIAL);
			stolenMaterial = EMaterialType.NO_MATERIAL;
		}

		switch (state) {
			case JOBLESS:
				break;
			case GOING_TO_POS:
				if(stolenMaterial == EMaterialType.NO_MATERIAL) {
					if(canWorkOnPos(position)) {
						playAction(EMovableAction.ACTION1, ACTION1_DURATION);
						state = EThiefState.PLAYING_ACTION1;
					} else {
						if(!findWorkablePosition()) {
							state = EThiefState.JOBLESS;
						}
					}
				}
				break;
			case PLAYING_ACTION1:
				if(canWorkOnPos(position)) {
					executeAction(position);
				}

				goToPos(returnPos);
				state = EThiefState.JOBLESS;
				break;
		}
	}


	private boolean findWorkablePosition() {
		if (preSearchPath(true, position.x, position.y, (short) 10, ESearchType.FOREIGN_MATERIAL)) {
			followPresearchedPath();
			return true;
		} else {
			return false;
		}
	}

	private boolean canWorkOnPos(ShortPoint2D pos) {
		return grid.fitsSearchType(this, pos.x, pos.y, ESearchType.FOREIGN_MATERIAL);
	}

	private void executeAction(ShortPoint2D pos) {
		stolenMaterial = grid.takeMaterial(pos);
		setMaterial(stolenMaterial);
	}

	@Override
	protected void moveToPathSet(ShortPoint2D oldPosition, ShortPoint2D oldTargetPos, ShortPoint2D targetPos, EMoveToType moveToType) {
		state = moveToType.isWorkOnDestination()? EThiefState.GOING_TO_POS : EThiefState.JOBLESS;

		if(getMaterial() == EMaterialType.NO_MATERIAL) {
			returnPos = position;
		} else {
			returnPos = null;
		}
	}

	@Override
	public void stopOrStartWorking(boolean stop) {
		if(stop) {
			state = EThiefState.JOBLESS;
		} else {
			state = EThiefState.GOING_TO_POS;
		}
	}

	@Override
	protected void pathAborted(ShortPoint2D pathTarget) {
		state = EThiefState.JOBLESS;
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		if(stolenMaterial != EMaterialType.NO_MATERIAL) {
			drop(stolenMaterial);
		}
	}

	private enum EThiefState {
		JOBLESS,
		GOING_TO_POS,
		PLAYING_ACTION1,
		RAISING_UP,
	}
}
