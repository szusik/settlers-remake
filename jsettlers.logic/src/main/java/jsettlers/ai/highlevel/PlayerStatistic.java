package jsettlers.ai.highlevel;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.IMaterialProductionSettings;
import jsettlers.common.landscape.EResourceType;
import jsettlers.common.map.partition.IPartitionData;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

class PlayerStatistic {
	ShortPoint2D referencePosition;
	final int[] totalBuildingsNumbers = new int[EBuildingType.NUMBER_OF_BUILDINGS];
	final int[] buildingsNumbers = new int[EBuildingType.NUMBER_OF_BUILDINGS];
	final Map<EBuildingType, List<ShortPoint2D>> buildingPositions = new HashMap<>();
	final List<ShortPoint2D> farmWorkAreas = new Vector<>();
	final List<ShortPoint2D> wineGrowerWorkAreas = new Vector<>();
	final Set<ShortPoint2D> activeHospitals = new HashSet<>();
	short partitionIdToBuildOn;
	short blockedPartitionId;
	IPartitionData materials;
	final AiPositions landToBuildOn = new AiPositions();
	final AiPositions borderIngestibleByPioneers = new AiPositions();
	final AiPositions otherPartitionBorder = new AiPositions();
	final Map<EMovableType, List<ShortPoint2D>> movablePositions = new HashMap<>();
	final List<ShortPoint2D> joblessBearerPositions = new ArrayList<>();
	final AiPositions stones = new AiPositions();
	final AiPositions stonesNearBy = new AiPositions();
	final AiPositions trees = new AiPositions();
	final AiPositions rivers = new AiPositions();
	final AiPositions enemyTroopsInTown = new AiPositions();
	List<ShortPoint2D> threatenedBorder;
	final long[] resourceCount = new long[EResourceType.VALUES.length];
	int numberOfNotFinishedBuildings;
	int numberOfTotalBuildings;
	int numberOfNotOccupiedMilitaryBuildings;
	int wineCount;
	IMaterialProductionSettings materialProduction;

	PlayerStatistic() {
		clearIntegers();
	}

	public void clearAll() {
		materials = null;
		buildingPositions.clear();
		enemyTroopsInTown.clear();
		stones.clear();
		stonesNearBy.clear();
		trees.clear();
		rivers.clear();
		landToBuildOn.clear();
		borderIngestibleByPioneers.clear();
		otherPartitionBorder.clear();
		movablePositions.clear();
		joblessBearerPositions.clear();
		farmWorkAreas.clear();
		wineGrowerWorkAreas.clear();
		activeHospitals.clear();
		threatenedBorder = null;
		clearIntegers();
	}

	private void clearIntegers() {
		Arrays.fill(totalBuildingsNumbers, 0);
		Arrays.fill(buildingsNumbers, 0);
		Arrays.fill(resourceCount, 0);
		numberOfNotFinishedBuildings = 0;
		numberOfTotalBuildings = 0;
		numberOfNotOccupiedMilitaryBuildings = 0;
		wineCount = 0;
		partitionIdToBuildOn = Short.MIN_VALUE;
		blockedPartitionId = Short.MIN_VALUE;
	}
}
