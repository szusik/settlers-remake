package jsettlers.logic.movable.civilian;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.RelativePoint;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class PigFarmerMovable extends BuildingWorkerMovable {

	public PigFarmerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.PIG_FARMER, position, player, replace, tree);
	}

	private static final Root<PigFarmerMovable> tree = new Root<>(createPigFarmerBehaviour());

	private ShortPoint2D targetPig;

	private static Node<PigFarmerMovable> createPigFarmerBehaviour() {
		return defaultWorkCycle(
				sequence(
						sleep(3000),
						waitFor(
							selector(
								sequence(
									outputStackNotFull(EMaterialType.PIG),
									condition(PigFarmerMovable::findKillablePig)
								),
								sequence(
									inputStackNotEmpty(EMaterialType.CROP),
									inputStackNotEmpty(EMaterialType.WATER),
									condition(PigFarmerMovable::findFreePigPlace)
								)
							)
						),
						show(),
						setMaterialNode(EMaterialType.NO_MATERIAL),
						selector(
							sequence(
								outputStackNotFull(EMaterialType.PIG),
								condition(mov -> mov.targetPig != null),
								ignoreFailure(
									sequence(
										goToPos(mov -> mov.targetPig, BuildingWorkerMovable::tmpPathStep),
										setDirectionNode(EDirection.NORTH_EAST),
										take(mov -> EMaterialType.PIG, mov -> false, mov -> {}),
										setPigAtTarget(false),
										goToOutputStack(EMaterialType.PIG, BuildingWorkerMovable::tmpPathStep),
										setDirectionNode(EDirection.NORTH_WEST),
										dropProduced(mov -> EMaterialType.PIG)
									)
								)
							),
							sequence(
								inputStackNotEmpty(EMaterialType.CROP),
								inputStackNotEmpty(EMaterialType.WATER),
								condition(mov -> mov.targetPig != null),
								ignoreFailure(
									sequence(
										// take crops
										setDirectionNode(EDirection.SOUTH_EAST),
										goToInputStack(EMaterialType.CROP, BuildingWorkerMovable::tmpPathStep),
										setDirectionNode(EDirection.NORTH_WEST),
										take(mov -> EMaterialType.CROP, mov -> true, mov -> {}),
										// and go home
										enterHome(),
										sleep(1000),

										// take water
										setMaterialNode(EMaterialType.NO_MATERIAL),
										setDirectionNode(EDirection.SOUTH_EAST),
										show(),
										goToInputStack(EMaterialType.WATER, BuildingWorkerMovable::tmpPathStep),
										setDirectionNode(EDirection.NORTH_EAST),
										take(mov -> EMaterialType.WATER, mov -> true, mov -> {}),
										// and go home
										enterHome(),
										sleep(1000),

										// place new pig
										condition(PigFarmerMovable::findFreePigPlace),
										setPigAtTarget(true),
										goToPos(mov -> mov.targetPig, BuildingWorkerMovable::tmpPathStep),
										setMaterialNode(EMaterialType.BASKET),
										setDirectionNode(EDirection.SOUTH_EAST),
										show(),
										goToPos(PigFarmerMovable::getPigPlacePosition, BuildingWorkerMovable::tmpPathStep),
										setDirectionNode(EDirection.NORTH_EAST),
										playAction(EMovableAction.ACTION1, (short)2000)
									)
								)
							),
							alwaysSucceed()
						),
						enterHome()
				)
		);
	}

	private static <T extends PigFarmerMovable> Node<T> setPigAtTarget(boolean place) {
		return action(mov -> {
			PigFarmerMovable realMov = mov;
			mov.grid.placePigAt(realMov.targetPig, place);
			mov.building.addMapObjectCleanupPosition(realMov.targetPig, EMapObjectType.PIG);
		});
	}

	private List<RelativePoint> getPigPositions() {
		List<RelativePoint> pigPositions = new ArrayList<>();

		pigPositions.add(new RelativePoint(2, 2));
		pigPositions.add(new RelativePoint(2, 1));
		pigPositions.add(new RelativePoint(3, 2));
		pigPositions.add(new RelativePoint(3, 3));

		return pigPositions;
	}

	private ShortPoint2D getPigPlacePosition() {
		return new RelativePoint(2, 3).calculatePoint(building.getPosition());
	}

	private ShortPoint2D searchTargetPig(Predicate<ShortPoint2D> validFunction) {
		for(RelativePoint pigPosition : getPigPositions()) {
			ShortPoint2D realPosition = pigPosition.calculatePoint(building.getPosition());

			if(!validFunction.test(realPosition)) continue;

			return realPosition;
		}

		return null;
	}

	private boolean findKillablePig() {
		targetPig = searchTargetPig(grid::isPigAdult);

		return targetPig != null;
	}


	private boolean findFreePigPlace() {
		targetPig = searchTargetPig(pos -> !grid.hasPigAt(pos));

		return targetPig != null;
	}
}
