package jsettlers.ai.army;

import jsettlers.common.CommonConstants;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.ILogicMovable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GroupStrategyModule extends ArmyModule {

	private static final float SOLDIER_THREAT_DISTANCE = CommonConstants.TOWER_RADIUS * 4;
	private static final float SOLDIER_OWN_GROUND_THREAT_MOD = 3;
	private static final float MAX_THREAT_OVER_COMMIT_FACTOR = 3;
	private static final float SOLDIER_FORCE_MOVE_DISTANCE = CommonConstants.TOWER_RADIUS * 0.5f;
	private static final float SOLDIER_MIN_MOVE_DISTANCE = 10;
	private static final float MIN_DEFENSE_QUOTA = 0.1f;
	private static final int MIN_DEFENSE_FORCE = 30;
	private static final float MIN_THREAT_LEVEL = 1f;

	private final GroupMap<ShortPoint2D, Integer> groups = new GroupMap<>();
	private final Comparator<Map.Entry<ShortPoint2D, Float>> POI_COMPARATOR;
	private ShortPoint2D attackTarget;

	public GroupStrategyModule(ArmyFramework parent) {
		super(parent);

		POI_COMPARATOR = Map.Entry.<ShortPoint2D, Float>comparingByValue().reversed();
	}

	@Override
	public void applyHeavyRules(Set<Integer> soldiersWithOrders) {
		updateGroups(soldiersWithOrders);

		// send soldiers
		sendSoldiers(soldiersWithOrders);
	}

	private int i = 0;

	@Override
	public void applyLightRules(Set<Integer> soldiersWithOrders) {
		updateAttackTarget();

		if (i < 3) {
			i++;
			return;
		}
		sendSoldiers(soldiersWithOrders);
		i = 0;
	}

	private void updateGroups(Set<Integer> soldiersWithOrders) {
		updateAttackTarget();

		Map<ShortPoint2D, Float> pois = calculateThreatLevels(calculatePointsOfInterest());

		Set<ShortPoint2D> validGroups = new HashSet<>(pois.keySet());
		validGroups.add(attackTarget);
		removeUnnecessaryGroups(validGroups);

		// ignore otherwise assigned soldiers
		soldiersWithOrders.forEach(s -> groups.setMember(s, null));

		List<ShortPoint2D> unassignedSoldiers = getAvailableSoldiers(soldiersWithOrders);
		updateAttackGroup(unassignedSoldiers);
		updateDefenseGroups(pois, unassignedSoldiers);

	}

	private void updateDefenseGroups(Map<ShortPoint2D, Float> pois, List<ShortPoint2D> unassignedSoldiers) {
		List<Map.Entry<ShortPoint2D, Float>> sortedPOIs = pois.entrySet().stream().sorted(POI_COMPARATOR).collect(Collectors.toList());

		for (Map.Entry<ShortPoint2D, Float> poiData : sortedPOIs) {
			ShortPoint2D poi = poiData.getKey();
			float targetNumber = poiData.getValue() * MAX_THREAT_OVER_COMMIT_FACTOR;
			unassignedSoldiers.sort(Comparator.comparing(poi::getOnGridDistTo));

			Set<Integer> group = groups.getMembers(poi);
			if(group.size() > targetNumber) {
				float removeNumber = group.size() - targetNumber;
				Iterator<Integer> removeIter = group.iterator();
				for(int i = 0; i < removeNumber && removeIter.hasNext(); i++) {
					int soldier = removeIter.next();
					groups.setMember(soldier, null);
					unassignedSoldiers.add(MovableManager.getMovableByID(soldier).getPosition());
				}
			} else {
				for (int i = group.size(); i < targetNumber && !unassignedSoldiers.isEmpty(); i++) {
					ShortPoint2D newSoldier = unassignedSoldiers.remove(unassignedSoldiers.size() - 1);
					groups.setMember(getID(newSoldier), poi);
				}
			}
		}
	}

	private void updateAttackGroup(List<ShortPoint2D> unassignedSoldiers) {

	}

	private void updateAttackTarget() {

	}

	private List<ShortPoint2D> getAvailableSoldiers(Set<Integer> soldiersWithOrders) {
		List<ShortPoint2D> allSoldiers = parent.aiStatistics.getPositionsOfMovablesWithTypesForPlayer(parent.getPlayerId(), EMovableType.SOLDIERS);
		Stream.of(soldiersWithOrders.stream(), groups.listMembers().stream())
				.flatMap(id -> id)
				.map(MovableManager::getMovableByID)
				.map(ILogicMovable::getPosition)
				.forEach(allSoldiers::remove);

		return allSoldiers;
	}

	private void sendSoldiers(Set<Integer> soldiersWithOrders) {
		for(Map.Entry<ShortPoint2D, Set<Integer>> poiData : groups.listGroups().entrySet()) {
			ShortPoint2D poi = poiData.getKey();
			Set<Integer> soldiers = poiData.getValue();
			if(poi.equals(attackTarget)) continue;

			List<Integer> forceMove = new ArrayList<>();
			List<Integer> defaultMove = new ArrayList<>();
			soldiers.stream().map(MovableManager::getMovableByID).forEach(mov -> {
				float distance = mov.getPosition().getOnGridDistTo(poi);
				if(distance <= SOLDIER_MIN_MOVE_DISTANCE) return;
				if(distance >= SOLDIER_FORCE_MOVE_DISTANCE) {
					forceMove.add(mov.getID());
				} else {
					defaultMove.add(mov.getID());
				}
			});
			parent.sendTroopsToById(forceMove, poi, soldiersWithOrders, EMoveToType.FORCED);
			parent.sendTroopsToById(defaultMove, poi, soldiersWithOrders, EMoveToType.DEFAULT);
		}
	}

	private int getID(ShortPoint2D soldier) {
		return parent.movableGrid.getMovableAt(soldier.x, soldier.y).getID();
	}

	private void removeUnnecessaryGroups(Set<ShortPoint2D> newGroups) {
		Set<ShortPoint2D> removeGroups = new HashSet<>(groups.listGroups().keySet());
		removeGroups.removeAll(newGroups);
		removeGroups.forEach(groups::removeGroup);
	}

	private List<ShortPoint2D> calculatePointsOfInterest() {
		List<ShortPoint2D> pois = new ArrayList<>(parent.aiStatistics.getBuildingPositionsOfTypesForPlayer(EBuildingType.MILITARY_BUILDINGS, parent.getPlayerId()));
		for(ShortPoint2D building : parent.aiStatistics.getBuildingPositionsOfTypesForPlayer(EnumSet.allOf(EBuildingType.class), parent.getPlayerId())) {
			if(pois.stream().mapToInt(poi -> poi.getOnGridDistTo(building)).min().orElse(0) >= CommonConstants.TOWER_RADIUS) {
				pois.add(building);
			}
		}
		return pois;
	}

	private Map<ShortPoint2D, Float> calculateThreatLevels(List<ShortPoint2D> pois) {
		Map<ShortPoint2D, Float> poiThreatLevels = new HashMap<>();
		for(ShortPoint2D poi : pois) {
			poiThreatLevels.put(poi, MIN_THREAT_LEVEL);
		}
		for(IPlayer enemy : parent.aiStatistics.getAliveEnemiesOf(parent.getPlayer())) {
			for (EMovableType soldierType : EMovableType.SOLDIERS) {
				for(ShortPoint2D soldier : parent.aiStatistics.getPositionsOfMovablesWithTypeForPlayer(enemy.getPlayerId(), soldierType)) {
					for(ShortPoint2D poi : pois) {
						float dst = poi.getOnGridDistTo(soldier)/ SOLDIER_THREAT_DISTANCE;
						if(dst < 1) {
							float threat = 1 - dst;
							if(parent.getEnemiesInTown().contains(soldier))  {
								threat *= SOLDIER_OWN_GROUND_THREAT_MOD;
							}
							float threatConst = threat;
							poiThreatLevels.compute(poi, (key, val) -> val + threatConst);
						}
					}
				}
			}
		}

		return poiThreatLevels;
	}
}
