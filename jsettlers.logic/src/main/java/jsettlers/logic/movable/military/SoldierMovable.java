package jsettlers.logic.movable.military;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.military.occupying.IOccupyableBuilding;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.ISoldierMovable;
import jsettlers.logic.movable.strategies.military.SoldierStrategy;
import jsettlers.logic.player.Player;

public class SoldierMovable extends AttackableHumanMovable implements ISoldierMovable {

	public SoldierMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable) {
		super(grid, movableType, position, player, movable);
	}


	@Override
	public final boolean moveToTower(IOccupyableBuilding building) {
		return ((SoldierStrategy<?>) strategy).setOccupyableBuilding(building);
	}

	@Override
	public void leaveTower(ShortPoint2D newPosition) {
		((SoldierStrategy<?>) strategy).leaveTower(newPosition);
	}

	@Override
	public void defendTowerAt(ShortPoint2D pos) {
		((SoldierStrategy<?>) strategy).defendTowerAt(pos);
	}
}
