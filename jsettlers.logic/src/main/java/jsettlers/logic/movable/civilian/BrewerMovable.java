package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
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
				sleep(1000)
		);
	}
}
