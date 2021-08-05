package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class SawMillerMovable extends BuildingWorkerMovable {

	public SawMillerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.SAWMILLER, position, player, replace, tree);
	}

	private static final Root<SawMillerMovable> tree = new Root<>(createSawMillerBehaviour());

	private static Node<SawMillerMovable> createSawMillerBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(1000),
					waitFor(
						sequence(
							isAllowedToWork(),
							inputStackNotEmpty(EMaterialType.TRUNK),
							outputStackNotFull(EMaterialType.PLANK)
						)
					),
					setMaterialNode(EMaterialType.NO_MATERIAL),
					show(),
					ignoreFailure(
						sequence(
							goToInputStack(EMaterialType.TRUNK, BuildingWorkerMovable::tmpPathStep),
							setDirectionNode(EDirection.NORTH_EAST),
							take(mov -> EMaterialType.TRUNK, mov -> true, mov -> {}),
							goToPos(SawMillerMovable::getWorkPosition, BuildingWorkerMovable::tmpPathStep),
							setDirectionNode(SawMillerMovable::getWorkDirection),
							repeatLoop(5, playAction(EMovableAction.ACTION1, (short)900)),
							setMaterialNode(EMaterialType.PLANK),
							goToOutputStack(EMaterialType.PLANK, BuildingWorkerMovable::tmpPathStep),
							setDirectionNode(EDirection.NORTH_EAST),
							dropProduced(mov -> EMaterialType.PLANK)
						)
					),
					enterHome()
				)
		);
	}

	private ShortPoint2D getWorkPosition() {
		return building.getBuildingVariant().getSawmillerWorkPosition().calculatePoint(building.getPosition());
	}

	private EDirection getWorkDirection() {
		return building.getBuildingVariant().getSawmillerWorkPosition().getDirection();
	}
}
