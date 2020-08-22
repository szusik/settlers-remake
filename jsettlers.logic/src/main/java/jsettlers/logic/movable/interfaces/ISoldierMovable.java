package jsettlers.logic.movable.interfaces;

import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.military.occupying.IOccupyableBuilding;

public interface ISoldierMovable extends IAttackableHumanMovable {

	boolean moveToTower(IOccupyableBuilding building);

	void leaveTower(ShortPoint2D newPosition);

	/**
	 * This method is called when this movable has to defend it's building at the given position.
	 *
	 * @param pos
	 *            The position the defending movable is standing.
	 */
	void defendTowerAt(ShortPoint2D pos);
}
