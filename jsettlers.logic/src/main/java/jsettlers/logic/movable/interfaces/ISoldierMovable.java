package jsettlers.logic.movable.interfaces;

import jsettlers.logic.buildings.military.IBuildingOccupyableMovable;
import jsettlers.logic.buildings.military.occupying.IOccupyableBuilding;

public interface ISoldierMovable extends ILogicMovable {
	IBuildingOccupyableMovable setOccupyableBuilding(IOccupyableBuilding building);
}
