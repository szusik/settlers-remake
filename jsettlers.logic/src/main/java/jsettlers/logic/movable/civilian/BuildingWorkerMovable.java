package jsettlers.logic.movable.civilian;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IBuildingWorkerMovable;
import jsettlers.logic.player.Player;

public class BuildingWorkerMovable extends Movable implements IBuildingWorkerMovable {

	public BuildingWorkerMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable replace) {
		super(grid, movableType, position, player, replace);
	}

	@Override
	public EBuildingType getGarrisonedBuildingType() {
		return strategy.getBuildingType();
	}
}
