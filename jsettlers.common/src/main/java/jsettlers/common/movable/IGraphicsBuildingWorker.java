package jsettlers.common.movable;

import jsettlers.common.buildings.EBuildingType;

public interface IGraphicsBuildingWorker extends IGraphicsMovable {

	/**
	 * This method returns the building type of a work if the worker is stationed in a building
	 *
	 * @return EBuildingType of the building the worker is garrisoned in or null if the worker is not garrisoned.
	 */
	EBuildingType getGarrisonedBuildingType();
}
