package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.buildings.EBuildingType;
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

public class SmithMovable extends BuildingWorkerMovable {

	private EMaterialType outputMaterial;

	public SmithMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.SMITH, position, player, replace, tree);
	}

	private static final Root<SmithMovable> tree = new Root<>(createSmithBehaviour());

	private static Node<SmithMovable> createSmithBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(1000),
					waitFor(
						sequence(
							isAllowedToWork(),
							inputStackNotEmpty(EMaterialType.COAL),
							inputStackNotEmpty(EMaterialType.IRON),
							condition(mov -> mov.grid.canPushMaterial(mov.getDropPosition())),
							// must be last
							condition(SmithMovable::popMaterial)
						)
					),
					show(),
					ignoreFailure(
						sequence(
							dropIntoOven(EMaterialType.COAL, EDirection.NORTH_WEST),
							dropIntoOven(EMaterialType.IRON, EDirection.NORTH_EAST),

							setSmoke((short) 3000),
							//playAction(EMovableAction.ACTION3, (short)3000),
							sleep(3000),
							setSmoke((short) 0),
							take(mov -> EMaterialType.IRON, mov -> false, mov -> {}),

							goToPos(SmithMovable::getWorkPosition, BuildingWorkerMovable::tmpPathStep),
							setDirectionNode(SmithMovable::getWorkDirection),
							setMaterialNode(EMaterialType.NO_MATERIAL),
							repeatLoop(5, playAction(EMovableAction.ACTION1, (short)700)),

							goToPos(SmithMovable::getDropPosition, BuildingWorkerMovable::tmpPathStep),
							setDirectionNode(EDirection.NORTH_EAST),
							dropProduced(mov -> mov.outputMaterial)
						)
					),
					enterHome()
				)
		);
	}

	private static Node<SmithMovable> dropIntoOven(EMaterialType material, EDirection takeDirection) {
		return sequence(
				goToInputStack(material, BuildingWorkerMovable::tmpPathStep),
				setDirectionNode(takeDirection),
				take(mov -> material, mov -> true, mov -> {}),

				goToPos(SmithMovable::getOvenPosition, BuildingWorkerMovable::tmpPathStep),
				setDirectionNode(SmithMovable::getOvenDirection),
				drop(mov -> EMaterialType.NO_MATERIAL, mov -> false)
		);
	}

	private boolean popMaterial() {
		if(building.getBuildingVariant().getType() == EBuildingType.TOOLSMITH) {
			outputMaterial = building.getMaterialProduction().drawRandomAbsolutelyRequestedTool(); // first priority: Absolutely set tool production requests of user
			if (outputMaterial == null) {
				outputMaterial = grid.popToolProductionRequest(building.getDoor()); // second priority: Tools needed by settlers (automated production)
			}
			if (outputMaterial == null) {
				outputMaterial = building.getMaterialProduction().drawRandomRelativelyRequestedTool(); // third priority: Relatively set tool production requests of user
			}
		} else {
			outputMaterial = building.getMaterialProduction().getWeaponToProduce();
		}

		return outputMaterial != null;
	}

	private ShortPoint2D getWorkPosition() {
		return building.getBuildingVariant().getAnvilPosition().calculatePoint(building.getPosition());
	}

	private EDirection getWorkDirection() {
		return building.getBuildingVariant().getAnvilPosition().getDirection();
	}

	private ShortPoint2D getOvenPosition() {
		return building.getBuildingVariant().getOvenPosition().calculatePoint(building.getPosition());
	}

	private EDirection getOvenDirection() {
		return building.getBuildingVariant().getOvenPosition().getDirection();
	}

	private ShortPoint2D getDropPosition() {
		return building.getBuildingVariant().getSmithDropPosition().calculatePoint(building.getPosition());
	}
}
