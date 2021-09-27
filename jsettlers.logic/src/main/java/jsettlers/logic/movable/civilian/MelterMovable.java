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
import jsettlers.logic.constants.Constants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class MelterMovable extends BuildingWorkerMovable {

	public MelterMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.MELTER, position, player, replace, tree);
	}

	private static final Root<MelterMovable> tree = new Root<>(createMelterBehaviour());

	private static Node<MelterMovable> createMelterBehaviour() {
		return defaultWorkCycle(
			sequence(
				waitFor(melterWorkPreconditions()),
				show(),
				repeat(mov -> true,
					sequence(
						melterWorkPreconditions(),
						goToInputStack(MelterMovable::getInputMaterial, BuildingWorkerMovable::tmpPathStep),
						setDirectionNode(EDirection.NORTH_WEST),
						take(MelterMovable::getInputMaterial, mov -> true, mov -> {}),

						dropIntoMeltingPot(),

						goToInputStack(EMaterialType.COAL, BuildingWorkerMovable::tmpPathStep),
						setDirectionNode(EDirection.NORTH_WEST),
						take(mov -> EMaterialType.COAL, mov -> true, mov -> {}),

						dropIntoMeltingPot(),

						goToPos(MelterMovable::getOutputPosition, BuildingWorkerMovable::tmpPathStep),
						setDirectionNode(MelterMovable::getOutputDirection),
						playAction(EMovableAction.ACTION1, MelterMovable::getMeltingTime),
						take(MelterMovable::getProducedMaterial, mov -> false, mov -> {}),

						goToOutputStack(MelterMovable::getProducedMaterial, BuildingWorkerMovable::tmpPathStep),
						setDirectionNode(EDirection.NORTH_EAST),
						dropProduced(MelterMovable::getProducedMaterial)
					)
				),
				enterHome()
			)
		);
	}

	private static Node<MelterMovable> dropIntoMeltingPot() {
		return sequence(
				goToPos(MelterMovable::getInputPoint, BuildingWorkerMovable::tmpPathStep),
				setDirectionNode(MelterMovable::getInputDirection),

				playAction(EMovableAction.BEND_DOWN, Constants.MOVABLE_BEND_DURATION),
				setMaterialNode(EMaterialType.NO_MATERIAL),
				playAction(EMovableAction.RAISE_UP, Constants.MOVABLE_BEND_DURATION)
		);
	}

	private static Node<MelterMovable> melterWorkPreconditions() {
		return sequence(
			isAllowedToWork(),
			inputStackNotEmpty(EMaterialType.COAL),
			inputStackNotEmpty(MelterMovable::getInputMaterial),
			outputStackNotFull(MelterMovable::getProducedMaterial)
		);
	}

	private EMaterialType getProducedMaterial() {
		return building.getBuildingVariant().getMeltOutputMaterial();
	}

	private EMaterialType getInputMaterial() {
		return building.getBuildingVariant().getMeltInputMaterial();
	}

	private ShortPoint2D getInputPoint() {
		return building.getBuildingVariant().getMeltInput().calculatePoint(building.getPosition());
	}

	private ShortPoint2D getOutputPosition() {
		return building.getBuildingVariant().getMeltOutput().calculatePoint(building.getPosition());
	}

	private EDirection getOutputDirection() {
		return building.getBuildingVariant().getMeltOutput().getDirection();
	}

	private EDirection getInputDirection() {
		return building.getBuildingVariant().getMeltInput().getDirection();
	}

	private short getMeltingTime() {
		return building.getBuildingVariant().getType() == EBuildingType.IRONMELT ? (short)5000 : (short)3500;
	}
}
