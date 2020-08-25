package jsettlers.logic.movable.specialist;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.EGoInDirectionMode;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IPioneerMovable;
import jsettlers.logic.player.Player;

public class PioneerMovable extends AttackableHumanMovable implements IPioneerMovable {

	private static final float ACTION1_DURATION = 1.2f;

	private EPioneerState state = EPioneerState.JOBLESS;
	private ShortPoint2D centerPos;

	public PioneerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.PIONEER, position, player, movable);
	}

	@Override
	public boolean convertToBearer() {
		if(!player.equals(grid.getPlayerAt(position))) return false;

		createMovable(EMovableType.BEARER, player, position, grid, this);
		killMovable();

		return true;
	}

	@Override
	protected void action() {
		switch (state) {
			case JOBLESS:
				return;

			case GOING_TO_POS:
				if (centerPos == null) {
					this.centerPos = position;
				}

				if (canWorkOnPos(position)) {
					super.playAction(EMovableAction.ACTION1, ACTION1_DURATION);
					state = EPioneerState.WORKING_ON_POS;
				} else {
					findWorkablePosition();
				}
				break;

			case WORKING_ON_POS:
				if (canWorkOnPos(position)) {
					executeAction(position);
				}

				findWorkablePosition();
				break;
		}
	}

	private void findWorkablePosition() {
		EDirection closeForeignTileDir = getCloseForeignTile();

		if (closeForeignTileDir != null && goInDirection(closeForeignTileDir, EGoInDirectionMode.GO_IF_ALLOWED_AND_FREE)) {
			this.state = EPioneerState.GOING_TO_POS;
			return;
		}
		centerPos = null;

		if (super.preSearchPath(true, position.x, position.y, (short) 30, ESearchType.UNENFORCED_FOREIGN_GROUND)) {
			super.followPresearchedPath();
			this.state = EPioneerState.GOING_TO_POS;
		} else {
			this.state = EPioneerState.JOBLESS;
		}
	}

	private EDirection getCloseForeignTile() {
		EDirection[] bestNeighbourDir = new EDirection[1];
		double[] bestNeighbourDistance = new double[] { Double.MAX_VALUE }; // distance from start point

		HexGridArea.stream(position.x, position.y, 1, 6)
				.filter((x, y) -> grid.isValidPosition(this, x, y) && canWorkOnPos(x, y))
				.forEach((x, y) -> {
					double distance = ShortPoint2D.getOnGridDist(x - centerPos.x, y - centerPos.y);
					if (distance < bestNeighbourDistance[0]) {
						bestNeighbourDistance[0] = distance;
						bestNeighbourDir[0] = EDirection.getApproxDirection(position.x, position.y, x, y);
					}
				});
		return bestNeighbourDir[0];
	}

	private void executeAction(ShortPoint2D pos) {
		grid.changePlayerAt(pos, player);
	}

	private boolean canWorkOnPos(int x, int y) {
		return grid.fitsSearchType(this, x, y, ESearchType.UNENFORCED_FOREIGN_GROUND);
	}

	private boolean canWorkOnPos(ShortPoint2D pos) {
		return grid.fitsSearchType(this, pos.x, pos.y, ESearchType.UNENFORCED_FOREIGN_GROUND);
	}

	@Override
	protected void moveToPathSet(ShortPoint2D oldPosition, ShortPoint2D oldTargetPos, ShortPoint2D targetPos, EMoveToType moveToType) {
		this.state = moveToType.isWorkOnDestination()? EPioneerState.GOING_TO_POS : EPioneerState.JOBLESS;
		centerPos = null;
	}

	@Override
	public void stopOrStartWorking(boolean stop) {
		if (stop) {
			state = EPioneerState.JOBLESS;
		} else {
			state = EPioneerState.GOING_TO_POS;
		}
	}

	@Override
	protected void pathAborted(ShortPoint2D pathTarget) {
		state = EPioneerState.JOBLESS;
	}

	private enum EPioneerState {
		JOBLESS,
		GOING_TO_POS,
		WORKING_ON_POS
	}
}
