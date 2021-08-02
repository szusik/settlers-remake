package jsettlers.logic.movable.civilian;

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

public class DonkeyFarmer extends BuildingWorkerMovable {

	private int feedIndex;

	public DonkeyFarmer(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.DONKEY_FARMER, position, player, replace, tree);
	}

	private static final Root<DonkeyFarmer> tree = new Root<>(createDonkeyBehaviour());

	private static Node<DonkeyFarmer> createDonkeyBehaviour() {
		return defaultWorkCycle(
			sequence(
				sleep(3000),
				waitFor(
					sequence(
						isAllowedToWork(),
						inputStackNotEmpty(EMaterialType.CROP),
						inputStackNotEmpty(EMaterialType.WATER)
					)
				),
				show(),
				ignoreFailure(
					sequence(
						// take crops
						setMaterialNode(EMaterialType.NO_MATERIAL),
						goToInputStack(EMaterialType.CROP, BuildingWorkerMovable::tmpPathStep),
						setDirectionNode(EDirection.NORTH_WEST),
						take(mov -> EMaterialType.CROP, mov -> true, mov -> {}),
						// and go home
						enterHome(),
						setMaterialNode(EMaterialType.NO_MATERIAL),
						sleep(5000),

						// take water
						show(),
						goToInputStack(EMaterialType.WATER, BuildingWorkerMovable::tmpPathStep),
						setDirectionNode(EDirection.NORTH_WEST),
						take(mov -> EMaterialType.WATER, mov -> true, mov -> {}),
						// and go home
						enterHome(),
						setMaterialNode(EMaterialType.BASKET),
						sleep(5000),
						// and feed the donkeys
						waitFor(
							sequence(
								isAllowedToWork(),
								condition(DonkeyFarmer::growDonkeys)
							)
						),
						action(mov -> mov.feedIndex = 0),
						show(),
						repeatLoop(DonkeyFarmer::getFeedPositionCount,
							sequence(
								goToPos(DonkeyFarmer::getFeedPosition, BuildingWorkerMovable::tmpPathStep),
								setDirectionNode(DonkeyFarmer::getFeedDirection),
								playAction(EMovableAction.ACTION1, mov -> (short)900),
								action(mov -> mov.feedIndex++)
							)
						)
					)
				),
				enterHome()
			)
		);
	}

	private int getFeedPositionCount() {
		return 2;
	}

	private ShortPoint2D getFeedPosition() {
		return building.getBuildingVariant().getDonkeyFeedPosition()[feedIndex].calculatePoint(building.getPosition());
	}

	private EDirection getFeedDirection() {
		return building.getBuildingVariant().getDonkeyFeedPosition()[feedIndex].getDirection();
	}

	private RelativePoint[] getDonkeyPositions() {
		return building.getBuildingVariant().getAnimalPositions();
	}

	private boolean growDonkeys() {
		for(RelativePoint donkeyPosition : getDonkeyPositions()) {
			ShortPoint2D realPosition = donkeyPosition.calculatePoint(building.getPosition());

			if(growDonkey(realPosition)) {
				return true;
			}
		}

		return false;
	}

	private boolean growDonkey(ShortPoint2D pos) {
		if(grid.feedDonkeyAt(pos)) {
			building.addMapObjectCleanupPosition(pos, EMapObjectType.DONKEY);
			return true;
		}

		return false;
	}
}
