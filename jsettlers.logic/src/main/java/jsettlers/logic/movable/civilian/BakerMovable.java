package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
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

public class BakerMovable extends BuildingWorkerMovable {

	public BakerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.BAKER, position, player, replace, tree);
	}

	private static final Root<BakerMovable> tree = new Root<>(createBakerBehaviour());

	private static Node<BakerMovable> createBakerBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(1000),
					waitFor(
						sequence(
							isAllowedToWork(),
							inputStackNotEmpty(EMaterialType.FLOUR),
							inputStackNotEmpty(EMaterialType.WATER),
							outputStackNotFull(EMaterialType.BREAD)
						)
					),
					show(),
					ignoreFailure(
						sequence(
							setMaterialNode(EMaterialType.NO_MATERIAL),
							goToInputStack(EMaterialType.FLOUR, BuildingWorkerMovable::tmpPathStep),
							setDirectionNode(EDirection.NORTH_WEST),
							take(mov -> EMaterialType.FLOUR, mov -> true, mov -> {}),
							enterHome(),

							sleep(1000),

							setMaterialNode(EMaterialType.NO_MATERIAL),
							show(),
							goToInputStack(EMaterialType.WATER, BuildingWorkerMovable::tmpPathStep),
							setDirectionNode(EDirection.NORTH_EAST),
							take(mov -> EMaterialType.WATER, mov -> true, mov -> {}),
							enterHome(),

							sleep(3000),

							setMaterialNode(EMaterialType.WHITE_BREAD),
							show(),
							goToPos(BakerMovable::getOvenPosition, BuildingWorkerMovable::tmpPathStep),
							setDirectionNode(BakerMovable::getOvenDirection),
							playAction(EMovableAction.ACTION1, (short)1000),
							setMaterialNode(EMaterialType.BLADE),
							setSmoke(true),
							sleep(4000),
							setMaterialNode(EMaterialType.BREAD),
							setSmoke(false),
							playAction(EMovableAction.ACTION1, (short)1000),

							goToOutputStack(EMaterialType.BREAD, BuildingWorkerMovable::tmpPathStep),
							setDirectionNode(EDirection.NORTH_WEST),
							dropProduced(mov -> EMaterialType.BREAD),
							setMaterialNode(EMaterialType.BLADE)
						)
					),
					enterHome()
				)
		);
	}

	private ShortPoint2D getOvenPosition() {
		return building.getBuildingVariant().getOvenPosition().calculatePoint(building.getPosition());
	}

	private EDirection getOvenDirection() {
		return building.getBuildingVariant().getOvenPosition().getDirection();
	}
}
