package jsettlers.logic.movable.specialist;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.MutablePoint2D;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.utils.mutables.MutableDouble;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

public class GeologistMovable extends AttackableHumanMovable {

	private static final float ACTION1_DURATION = 1.4f;
	private static final float ACTION2_DURATION = 1.5f;

	private EGeologistState state = EGeologistState.JOBLESS;
	private ShortPoint2D centerPos;

	public GeologistMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.GEOLOGIST, position, player, movable);
	}

	@Override
	protected void action() {
		switch (state) {
			case JOBLESS:
				return;

			case GOING_TO_POS: {

				if (centerPos == null) {
					this.centerPos = position;
				}

				grid.setMarked(position, false); // unmark the pos for the following check
				if (canWorkOnPos(position)) {
					grid.setMarked(position, true);
					playAction(EMovableAction.ACTION1, ACTION1_DURATION);
					state = EGeologistState.PLAYING_ACTION_1;
				} else {
					findWorkablePosition();
				}
			}
			break;

			case PLAYING_ACTION_1:
				playAction(EMovableAction.ACTION2, ACTION2_DURATION);
				state = EGeologistState.PLAYING_ACTION_2;
				break;

			case PLAYING_ACTION_2: {
				grid.setMarked(position, false);
				if (canWorkOnPos(position)) {
					executeAction(position);
				}

				findWorkablePosition();
			}
			break;
		}
	}

	private void findWorkablePosition() {
		ShortPoint2D closeWorkablePos = getCloseWorkablePos();

		if (closeWorkablePos != null && goToPos(closeWorkablePos)) {
			grid.setMarked(closeWorkablePos, true);
			this.state = EGeologistState.GOING_TO_POS;
			return;
		}
		centerPos = null;

		if (preSearchPath(true, position.x, position.y, (short) 30, ESearchType.RESOURCE_SIGNABLE)) {
			followPresearchedPath();
			this.state = EGeologistState.GOING_TO_POS;
			return;
		}

		this.state = EGeologistState.JOBLESS;
	}

	private ShortPoint2D getCloseWorkablePos() {
		MutablePoint2D bestNeighbourPos = new MutablePoint2D(-1, -1);
		MutableDouble bestNeighbourDistance = new MutableDouble(Double.MAX_VALUE); // distance from start point

		HexGridArea.streamBorder(position, 2).filter((x, y) -> grid.isValidPosition(this, x, y) && canWorkOnPos(x, y)).forEach((x, y) -> {
			double distance = ShortPoint2D.getOnGridDist(x - centerPos.x, y - centerPos.y);
			if (distance < bestNeighbourDistance.value) {
				bestNeighbourDistance.value = distance;
				bestNeighbourPos.x = x;
				bestNeighbourPos.y = y;
			}
		});

		if (bestNeighbourDistance.value != Double.MAX_VALUE) {
			return bestNeighbourPos.createShortPoint2D();
		} else {
			return null;
		}
	}

	private void executeAction(ShortPoint2D pos) {
		grid.executeSearchType(this, pos, ESearchType.RESOURCE_SIGNABLE);
	}

	private boolean canWorkOnPos(ShortPoint2D pos) {
		return grid.fitsSearchType(this, pos.x, pos.y, ESearchType.RESOURCE_SIGNABLE);
	}

	private boolean canWorkOnPos(int x, int y) {
		return grid.fitsSearchType(this, x, y, ESearchType.RESOURCE_SIGNABLE);
	}

	@Override
	protected void moveToPathSet(ShortPoint2D oldPosition, ShortPoint2D oldTargetPos, ShortPoint2D targetPos, EMoveToType moveToType) {
		this.state = moveToType.isWorkOnDestination()? EGeologistState.GOING_TO_POS : EGeologistState.JOBLESS;
		centerPos = null;

		grid.setMarked(oldPosition, false);

		if (oldTargetPos != null) {
			grid.setMarked(oldTargetPos, false);
		}
	}

	@Override
	public void stopOrStartWorking(boolean stop) {
		if (stop) {
			state = EGeologistState.JOBLESS;
		} else {
			state = EGeologistState.GOING_TO_POS;
		}
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		if(path != null) {
			grid.setMarked(path.getTargetPosition(), false);
		} else {
			grid.setMarked(position, false);
		}
	}

	@Override
	protected void pathAborted(ShortPoint2D pathTarget) {
		state = EGeologistState.JOBLESS;
	}

	private enum EGeologistState {
		JOBLESS,
		GOING_TO_POS,
		PLAYING_ACTION_1,
		PLAYING_ACTION_2
	}
}
