package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class BrewerMovable extends BuildingWorkerMovable {

	public BrewerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.BREWER, position, player, replace);
	}

	static {
		MovableManager.registerBehaviour(EMovableType.BREWER, new Root<>(createBrewerBehaviour()));
	}

	private static Node<BrewerMovable> createBrewerBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(1000),
					waitFor(
						sequence(
							isAllowedToWork(),
							inputStackNotEmpty(EMaterialType.CROP),
							inputStackNotEmpty(EMaterialType.WATER),
							outputStackNotFull(EMaterialType.KEG)
						)
					),
					show(),
					ignoreFailure(
						sequence(
							dropIntoOven(EMaterialType.WATER, EDirection.NORTH_EAST),
							dropIntoOven(EMaterialType.CROP, EDirection.NORTH_EAST),
							enterHome(),
							sleep(5000),
							setMaterialNode(EMaterialType.KEG),
							show(),
							goToOutputStack(EMaterialType.KEG),
							setDirectionNode(EDirection.NORTH_WEST),
							dropProduced(mov -> EMaterialType.KEG)
						)
					),
					enterHome()
				)
		);
	}
}
