package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class AlchemistMovable extends BuildingWorkerMovable {

	public AlchemistMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.ALCHEMIST, position, player, replace);
	}

	static {
		MovableManager.registerBehaviour(EMovableType.ALCHEMIST, new Root<>(createAlchemistBehaviour()));
	}

	private static Node<AlchemistMovable> createAlchemistBehaviour() {
		return defaultWorkCycle(
				sequence(
						sleep(3000),
						waitFor(
								sequence(
										isAllowedToWork(),
										inputStackNotEmpty(EMaterialType.GEMS),
										inputStackNotEmpty(EMaterialType.SULFUR),
										outputStackNotFull(EMaterialType.IRON),
										outputStackNotFull(EMaterialType.GOLD)
								)
						),
						show(),
						ignoreFailure(
								sequence(
										setMaterialNode(EMaterialType.NO_MATERIAL),
										dropIntoOven(EMaterialType.SULFUR, EDirection.SOUTH_WEST),
										dropIntoOven(EMaterialType.GEMS, EDirection.SOUTH_WEST),
										enterHome(),
										sleep(1000),
										show(),
										setMaterialNode(EMaterialType.CHEMICALS),
										goToPos(AlchemistMovable::getOvenPosition),
										setDirectionNode(AlchemistMovable::getOvenDirection),
										playAction(EMovableAction.ACTION1, (short)1000),

										setMaterialNode(EMaterialType.NO_MATERIAL),
										crouchDown(setMaterialNode(EMaterialType.METALS)),
										goToOutputStack(Movable::getMaterial),
										dropProduced(Movable::getMaterial)
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
