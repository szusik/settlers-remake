/*******************************************************************************
 * Copyright (c) 2016 - 2018
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.ai.army;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import jsettlers.ai.highlevel.AiStatistics;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.ESoldierType;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.action.EMoveToType;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.movable.MovableGrid;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.player.Player;
import jsettlers.network.client.interfaces.ITaskScheduler;

import static java8.util.stream.StreamSupport.stream;

/**
 * This general is named winner because his attacks and defence should be very hard for human enemies. This should be realized by creating locally superiority. (You can kill 200 BOWMEN with just 100
 * BOWMEN if you fight 100 vs 20 in loops. This general should lay the focus on some SWORDSMEN to occupy own towers, 20 spearmen to defeat rushes and the rest only BOWMEN because in mass this is the
 * strongest military unit. It upgrades BOWMEN first because this is the main unit and the 20 defeating spearmen defeats with lv1 as well. This general should store bows until level3 is reached to get
 * as many level3 BOWMEN as posibble. TODO: store bows until level3 is reached TODO: group SOLDIERS in direction of enemy groups to defeat them TODO: group SOLDIERS in direction of enemy groups to
 * attack them
 *
 * @author codingberlin
 */
public class ConfigurableGeneral implements ArmyGeneral {

	private static float[] ATTACKER_COUNT_FACTOR_BY_PLAYER_TYPE = { 1.1F, 1F, 0.9F, 0.8F, 0F };

	private static final byte MIN_ATTACKER_COUNT = 20;
	private static final byte MIN_SWORDSMEN_COUNT = 10;
	private static final byte MIN_PIKEMEN_COUNT = 20;
	private static final int BOWMEN_COUNT_OF_KILLING_INFANTRY = 300;
	private static final EBuildingType[] MIN_BUILDING_REQUIREMENTS_FOR_ATTACK = { EBuildingType.COALMINE, EBuildingType.IRONMINE, EBuildingType.IRONMELT, EBuildingType.WEAPONSMITH,
			EBuildingType.BARRACK };
	private static final ESoldierType[] SOLDIER_UPGRADE_ORDER = new ESoldierType[] { ESoldierType.BOWMAN, ESoldierType.PIKEMAN, ESoldierType.SWORDSMAN };

	private static final float SOLDIERS_MIN_HEALTH = 0.6f;
	private static final float HEALING_DISTANCE_WEIGHT = -1;
	private static final float HEALING_USAGE_WEIGHT = -10;
	private static final float MAX_WOUNDED_RATIO_FOR_ATTACK = 0.5f;

	private final ArmyFramework framework;
	private final float attackerCountFactor;
	private final float healPropFactor;
	private final Set<Integer> woundedSoldiers = new HashSet<>();
	private final Map<ILogicMovable, ShortPoint2D> assignedPatients = new HashMap<>();
	private final Map<ShortPoint2D, Integer> usedHospitalCapacity = new HashMap<>();

	private final int hospitalWorkRadius;

	/**
	 *
	 * @param  healPropFactor
	 * 		The fraction of wounded soldiers that get send to the hospital
	 */
	public ConfigurableGeneral(AiStatistics aiStatistics, Player player, MovableGrid movableGrid, ITaskScheduler taskScheduler, EPlayerType playerType, float healPropFactor) {
		framework = new ArmyFramework(aiStatistics, player, movableGrid, taskScheduler);
		this.attackerCountFactor = ATTACKER_COUNT_FACTOR_BY_PLAYER_TYPE[playerType.ordinal()];
		this.healPropFactor = healPropFactor;

		// setup woundedSoldiers
		healTroops(null, false);

		hospitalWorkRadius = EBuildingType.HOSPITAL.getVariant(player.getCivilisation()).getWorkRadius();
	}

	@Override
	public void applyHeavyRules(Set<Integer> soldiersWithOrders) {
		levyUnits();

		soldiersWithOrders.addAll(woundedSoldiers);
		framework.ensureAllTowersFullyMounted();

		SoldierPositions soldierPositions = calculateSituation(framework.getPlayerId());
		if (framework.getEnemiesInTown().size() > 0) {
			defend(soldierPositions, soldiersWithOrders);
		} else if (framework.existsAliveEnemy()) {
			IPlayer weakestEnemy = framework.getWeakestEnemy();
			SoldierPositions enemySoldierPositions = calculateSituation(weakestEnemy.getPlayerId());
			boolean infantryWouldDie = wouldInfantryDie(enemySoldierPositions);
			if (woundedSoldiers.size()/(float)soldierPositions.getSoldiersCount() <= MAX_WOUNDED_RATIO_FOR_ATTACK &&
					attackIsPossible(soldierPositions, enemySoldierPositions, infantryWouldDie)) {
				attack(soldierPositions, infantryWouldDie, soldiersWithOrders);
			}
		}
	}

	private boolean attackIsPossible(SoldierPositions soldierPositions, SoldierPositions enemySoldierPositions, boolean infantryWouldDie) {
		for (EBuildingType requiredType : MIN_BUILDING_REQUIREMENTS_FOR_ATTACK) {
			if (framework.aiStatistics.getNumberOfBuildingTypeForPlayer(requiredType, framework.getPlayerId()) < 1) {
				return false;
			}
		}

		float combatStrength = framework.getPlayer().getCombatStrengthInformation().getCombatStrength(false);
		float effectiveAttackerCount;
		if (infantryWouldDie) {
			effectiveAttackerCount = soldierPositions.bowmenPositions.size() * combatStrength;
		} else {
			effectiveAttackerCount = soldierPositions.getSoldiersCount() * combatStrength;
		}
		return effectiveAttackerCount >= MIN_ATTACKER_COUNT && effectiveAttackerCount * attackerCountFactor > enemySoldierPositions.getSoldiersCount();

	}

	private boolean wouldInfantryDie(SoldierPositions enemySoldierPositions) {
		return enemySoldierPositions.bowmenPositions.size() > BOWMEN_COUNT_OF_KILLING_INFANTRY;
	}

	public void levyUnits() {
		upgradeSoldiers();

		int missingSwordsmenCount = Math.max(0, MIN_SWORDSMEN_COUNT - framework.aiStatistics.getCountOfMovablesOfPlayer(framework.getPlayer(), EMovableType.SWORDSMEN));
		int missingPikemenCount = Math.max(0, MIN_PIKEMEN_COUNT - framework.aiStatistics.getCountOfMovablesOfPlayer(framework.getPlayer(), EMovableType.PIKEMEN));
		int bowmenCount = framework.aiStatistics.getCountOfMovablesOfPlayer(framework.getPlayer(), EMovableType.BOWMEN);

		if (missingSwordsmenCount > 0) {
			framework.setNumberOfFutureProducedMaterial(framework.getPlayerId(), EMaterialType.SWORD, missingSwordsmenCount);
			framework.setNumberOfFutureProducedMaterial(framework.getPlayerId(), EMaterialType.SPEAR, 0);
			framework.setNumberOfFutureProducedMaterial(framework.getPlayerId(), EMaterialType.BOW, 0);
			framework.setRatioOfMaterial(framework.getPlayerId(), EMaterialType.SWORD, 0F);
			framework.setRatioOfMaterial(framework.getPlayerId(), EMaterialType.SPEAR, 1F);
			framework.setRatioOfMaterial(framework.getPlayerId(), EMaterialType.BOW, 0F);
		} else if (missingPikemenCount > 0) {
			framework.setNumberOfFutureProducedMaterial(framework.getPlayerId(), EMaterialType.SWORD, 0);
			framework.setNumberOfFutureProducedMaterial(framework.getPlayerId(), EMaterialType.SPEAR, missingPikemenCount);
			framework.setNumberOfFutureProducedMaterial(framework.getPlayerId(), EMaterialType.BOW, 0);
			framework.setRatioOfMaterial(framework.getPlayerId(), EMaterialType.SWORD, 0F);
			framework.setRatioOfMaterial(framework.getPlayerId(), EMaterialType.SPEAR, 0.3F);
			framework.setRatioOfMaterial(framework.getPlayerId(), EMaterialType.BOW, 1F);
		} else if (bowmenCount * framework.getPlayer().getCombatStrengthInformation().getCombatStrength(false) < BOWMEN_COUNT_OF_KILLING_INFANTRY) {
			framework.setNumberOfFutureProducedMaterial(framework.getPlayerId(), EMaterialType.SWORD, 0);
			framework.setNumberOfFutureProducedMaterial(framework.getPlayerId(), EMaterialType.SPEAR, 0);
			framework.setNumberOfFutureProducedMaterial(framework.getPlayerId(), EMaterialType.BOW, 0);
			framework.setRatioOfMaterial(framework.getPlayerId(), EMaterialType.SWORD, 0F);
			framework.setRatioOfMaterial(framework.getPlayerId(), EMaterialType.SPEAR, 0.3F);
			framework.setRatioOfMaterial(framework.getPlayerId(), EMaterialType.BOW, 1F);
		} else {
			framework.setNumberOfFutureProducedMaterial(framework.getPlayerId(), EMaterialType.SWORD, 0);
			framework.setNumberOfFutureProducedMaterial(framework.getPlayerId(), EMaterialType.SPEAR, 0);
			framework.setNumberOfFutureProducedMaterial(framework.getPlayerId(), EMaterialType.BOW, 0);
			framework.setRatioOfMaterial(framework.getPlayerId(), EMaterialType.SWORD, 0F);
			framework.setRatioOfMaterial(framework.getPlayerId(), EMaterialType.SPEAR, 0F);
			framework.setRatioOfMaterial(framework.getPlayerId(), EMaterialType.BOW, 1F);
		}
	}

	private void upgradeSoldiers() {
		for (ESoldierType type : SOLDIER_UPGRADE_ORDER) {
			if (framework.canUpgradeSoldiers(type)) {
				framework.upgradeSoldiers(type);
			}
		}
	}

	@Override
	public void applyLightRules(Set<Integer> soldiersWithOrders) {
		healTroops(soldiersWithOrders, true);
	}

	private void healTroops(Set<Integer> soldiersWithOrders, boolean commit) {
		woundedSoldiers.clear();
		if(healPropFactor == 0) return;

		// this list is at most a couple of seconds old
		Set<ShortPoint2D> hospitals = framework.aiStatistics.getActiveHospitalsForPlayer(framework.getPlayerId());
		if(hospitals.isEmpty()) return;

		Iterator<Map.Entry<ILogicMovable, ShortPoint2D>> woundedIter = assignedPatients.entrySet().iterator();

		// remove dead and healed movables from the patients list
		// regenerate the usedHospitalCapacity
		// unassign patients that are going to destroyed hospitals

		usedHospitalCapacity.clear();
		hospitals.forEach(pt -> usedHospitalCapacity.put(pt, 0));

		while(woundedIter.hasNext()) {
			Map.Entry<ILogicMovable, ShortPoint2D> next = woundedIter.next();
			ShortPoint2D hospital = next.getValue();
			if(!hospitals.contains(hospital)) {
				woundedIter.remove();
				continue;
			}

			ILogicMovable movable = next.getKey();

			if(!isWounded(movable) || !movable.isAlive()) {
				woundedIter.remove();
				continue;
			}

			increaseHospitalUse(hospital);
		}

		// assign newly wounded soldiers to hospitals

		Map<ShortPoint2D, List<Integer>> newOrders = new HashMap<>();
		hospitals.forEach(hospital -> newOrders.put(hospital, new ArrayList<>()));

		MovableManager.getAllMovables().stream()
				.filter(this::isWounded)
				.filter(mov -> mov.getPlayer().equals(framework.getPlayer()))
				.filter(mov -> EMovableType.PLAYER_CONTROLLED_HUMAN_MOVABLE_TYPES
						.contains(mov.getMovableType()))
				// only wounded movables that we actually can heal should be considered
				.forEach(mov -> {
					ShortPoint2D assignedHospital = assignedPatients.get(mov);

					// not all wounded soldiers should be send
					if(assignedHospital != null || randomHealChance()) {
						if (assignedHospital == null) {
							assignedHospital = getBestHospital(mov, hospitals);
							increaseHospitalUse(assignedHospital);
							assignedPatients.put(mov, assignedHospital);
						}

						woundedSoldiers.add(mov.getID());

						if(mov.getPosition().getOnGridDistTo(assignedHospital) >= hospitalWorkRadius) {
							newOrders.get(assignedHospital).add(mov.getID());
						}
					}
				});

		if(commit) {
			newOrders.entrySet().forEach(newOrder -> {
				framework.sendTroopsToById(newOrder.getValue(), newOrder.getKey(), soldiersWithOrders, EMoveToType.FORCED);
			});
		}
	}

	private boolean randomHealChance() {
		if(healPropFactor == 1) return true;

		return MatchConstants.aiRandom().nextFloat() >= 1-healPropFactor;
	}

	private boolean isWounded(ILogicMovable mov) {
		return mov.getHealth() <= SOLDIERS_MIN_HEALTH * mov.getMovableType().getHealth();
	}

	private void increaseHospitalUse(ShortPoint2D hospital) {
		usedHospitalCapacity.compute(hospital, (pos, oldValue) -> oldValue+1);
	}

	private ShortPoint2D getBestHospital(ILogicMovable movable, Set<ShortPoint2D> hospitals) {
		float maxScore = Float.NEGATIVE_INFINITY;
		ShortPoint2D bestHospital = null;

		for (ShortPoint2D hospital : hospitals) {
			float localScore = getHospitalScore(hospital, movable.getPosition());

			if (localScore > maxScore) {
				maxScore = localScore;
				bestHospital = hospital;
			}
		}

		return bestHospital;
	}

	private float getHospitalScore(ShortPoint2D hospital, ShortPoint2D from) {
		int distance = hospital.getOnGridDistTo(from);
		int usage = usedHospitalCapacity.get(hospital);

		float score = 0;

		score += distance * HEALING_DISTANCE_WEIGHT;
		score += usage * HEALING_USAGE_WEIGHT;

		return score;
	}

	private void defend(SoldierPositions soldierPositions, Set<Integer> soldiersWithOrders) {
		List<ShortPoint2D> allMyTroops = new Vector<>();
		allMyTroops.addAll(soldierPositions.bowmenPositions);
		allMyTroops.addAll(soldierPositions.pikemenPositions);
		allMyTroops.addAll(soldierPositions.swordsmenPositions);
		framework.sendTroopsTo(allMyTroops, framework.getEnemiesInTown().iterator().next(), soldiersWithOrders, EMoveToType.DEFAULT);
	}

	private void attack(SoldierPositions soldierPositions, boolean infantryWouldDie, Set<Integer> soldiersWithOrders) {
		IPlayer weakestEnemy = framework.getWeakestEnemy();
		ShortPoint2D targetDoor = getTargetEnemyDoorToAttack(weakestEnemy);
		if(targetDoor == null) return;

		if (infantryWouldDie) {
			framework.sendTroopsTo(soldierPositions.bowmenPositions, targetDoor, soldiersWithOrders, EMoveToType.DEFAULT);
		} else {
			List<ShortPoint2D> soldiers = new ArrayList<>(soldierPositions.bowmenPositions.size() + soldierPositions.pikemenPositions.size() + soldierPositions.swordsmenPositions.size());
			soldiers.addAll(soldierPositions.bowmenPositions);
			soldiers.addAll(soldierPositions.pikemenPositions);
			soldiers.addAll(soldierPositions.swordsmenPositions);
			framework.sendTroopsTo(soldiers, targetDoor, soldiersWithOrders, EMoveToType.DEFAULT);
		}
	}

	private ShortPoint2D getTargetEnemyDoorToAttack(IPlayer enemyToAttack) {
		List<ShortPoint2D> myMilitaryBuildings = framework.aiStatistics.getBuildingPositionsOfTypesForPlayer(EBuildingType.MILITARY_BUILDINGS, framework.getPlayerId());
		ShortPoint2D myBaseAveragePoint = AiStatistics.calculateAveragePointFromList(myMilitaryBuildings);
		List<ShortPoint2D> enemyMilitaryBuildings = framework.aiStatistics.getBuildingPositionsOfTypesForPlayer(EBuildingType.MILITARY_BUILDINGS, enemyToAttack.getPlayerId());
		// ignore unfinished buildings
		enemyMilitaryBuildings.removeIf(shortPoint2D -> !framework.aiStatistics.getBuildingAt(shortPoint2D).isConstructionFinished());

		ShortPoint2D nearestEnemyBuildingPosition = AiStatistics.detectNearestPointFromList(myBaseAveragePoint, enemyMilitaryBuildings);
		if(nearestEnemyBuildingPosition == null) return null;

		return framework.aiStatistics.getBuildingAt(nearestEnemyBuildingPosition).getDoor();
	}

	private void calculateSituation(List<ShortPoint2D> soldierPositions, Set<EMovableType> soldierTypes) {
		for(EMovableType soldierType : soldierTypes) {
			soldierPositions.addAll(framework.aiStatistics.getPositionsOfMovablesWithTypeForPlayer(framework.getPlayerId(), soldierType));
		}
	}

	private SoldierPositions calculateSituation(byte playerId) {
		SoldierPositions soldierPositions = new SoldierPositions();
		calculateSituation(soldierPositions.swordsmenPositions, EMovableType.SWORDSMEN);
		calculateSituation(soldierPositions.bowmenPositions, EMovableType.BOWMEN);
		calculateSituation(soldierPositions.pikemenPositions, EMovableType.PIKEMEN);
		return soldierPositions;
	}

	private static class SoldierPositions {
		private final List<ShortPoint2D> swordsmenPositions = new Vector<>();
		private final List<ShortPoint2D> bowmenPositions = new Vector<>();
		private final List<ShortPoint2D> pikemenPositions = new Vector<>();

		int getSoldiersCount() {
			return swordsmenPositions.size() + bowmenPositions.size() + pikemenPositions.size();
		}
	}

	@Override
	public String toString() {
		return this.getClass().getName();
	}
}
