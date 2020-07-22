package jsettlers.logic.movable.military;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.military.IBuildingOccupyableMovable;
import jsettlers.logic.buildings.military.occupying.IOccupyableBuilding;
import jsettlers.logic.movable.AttackableHumanMovable;
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
	public final IBuildingOccupyableMovable setOccupyableBuilding(IOccupyableBuilding building) {
		return ((SoldierStrategy<?>) strategy).setOccupyableBuilding(building);
	}
}
