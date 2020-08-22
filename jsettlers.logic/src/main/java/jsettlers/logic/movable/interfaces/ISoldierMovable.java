package jsettlers.logic.movable.interfaces;

import jsettlers.logic.buildings.military.IBuildingOccupyableMovable;
import jsettlers.logic.buildings.military.occupying.IOccupyableBuilding;

public interface ISoldierMovable extends IAttackableHumanMovable {
	IBuildingOccupyableMovable setOccupyableBuilding(IOccupyableBuilding building);
}
