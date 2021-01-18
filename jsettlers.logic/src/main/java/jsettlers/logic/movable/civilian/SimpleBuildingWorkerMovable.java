package jsettlers.logic.movable.civilian;

import java.util.EnumMap;
import java.util.Map;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.RelativePoint;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class SimpleBuildingWorkerMovable extends BuildingWorkerMovable {

	public SimpleBuildingWorkerMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable replace) {
		super(grid, movableType, position, player, replace, trees.get(movableType));
	}

	private static final Map<EMovableType, Root<SimpleBuildingWorkerMovable>> trees = new EnumMap<>(EMovableType.class);

	static {
		trees.put(EMovableType.FORESTER, new Root<>(createForesterBehaviour()));
		trees.put(EMovableType.LUMBERJACK, new Root<>(createLumberjackBehaviour()));
	}

	private static Node<SimpleBuildingWorkerMovable> createForesterBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(4000),
					setMaterialNode(EMaterialType.TREE),
					waitFor(
						sequence(
							isAllowedToWork(),
							preSearchPath(false, ESearchType.PLANTABLE_TREE)
						)
					),
					show(),
					ignoreFailure(
						sequence(
							followPresearchedPath(BuildingWorkerMovable::tmpPathStep), // TODO
							setDirectionNode(mov -> EDirection.NORTH_WEST),
							playAction(EMovableAction.ACTION1, (short)3000),
							setMaterialNode(EMaterialType.NO_MATERIAL),
							executeSearch(ESearchType.PLANTABLE_TREE)
						)
					),
					enterHome()
				)
		);
	}

	private static final RelativePoint LUMBERJACK_DROPOFF_POINT = new RelativePoint(0, 3);

	private static final short LUMBERJACK_ACTION1_DURATION = (short)1000;

	private static Node<SimpleBuildingWorkerMovable> lumberjackAction() {
		return sequence(
				playAction(EMovableAction.ACTION1, LUMBERJACK_ACTION1_DURATION),
				playAction(EMovableAction.ACTION1, LUMBERJACK_ACTION1_DURATION),
				playAction(EMovableAction.ACTION1, LUMBERJACK_ACTION1_DURATION)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createLumberjackBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(3000),
					waitFor(
						sequence(
							isAllowedToWork(),
							condition(mov -> {
								// TODO generify
								ShortPoint2D dropOff = LUMBERJACK_DROPOFF_POINT.calculatePoint(mov.building.getPosition());

								return ((SimpleBuildingWorkerMovable)mov).grid.canPushMaterial(dropOff);
							}),
							preSearchPath(true, ESearchType.CUTTABLE_TREE)
						)
					),
					show(),
					ignoreFailure(
						sequence(
							followPresearchedPathMarkTarget(BuildingWorkerMovable::tmpPathStep),
							selector(
								sequence(
									goInDirectionWaitFree(EDirection.EAST, BuildingWorkerMovable::tmpPathStep),
									setDirectionNode(mov -> EDirection.NORTH_WEST),
									lumberjackAction(),
									lumberjackAction(),
									goInDirectionWaitFree(EDirection.WEST, BuildingWorkerMovable::tmpPathStep)
								),
								sequence(
									setDirectionNode(mov -> EDirection.NORTH_WEST),
									lumberjackAction(),
									lumberjackAction()
								)
							),
							executeSearch(ESearchType.CUTTABLE_TREE),
							ignoreFailure(
								sequence(
									goInDirectionWaitFree(EDirection.SOUTH_EAST, BuildingWorkerMovable::tmpPathStep),
									goInDirectionWaitFree(EDirection.NORTH_EAST, BuildingWorkerMovable::tmpPathStep),
									setDirectionNode(mov -> EDirection.NORTH_WEST),
									lumberjackAction(),
									goInDirectionWaitFree(EDirection.NORTH_EAST, BuildingWorkerMovable::tmpPathStep),
									setDirectionNode(mov -> EDirection.NORTH_WEST),
									lumberjackAction(),
									goInDirectionWaitFree(EDirection.SOUTH_WEST, BuildingWorkerMovable::tmpPathStep),
									setDirectionNode(mov -> EDirection.NORTH_WEST),
									take(mov -> EMaterialType.TRUNK, mov -> false, mov -> {})
								)
							),
							setMaterialNode(EMaterialType.TRUNK),
							goToPos(mov -> LUMBERJACK_DROPOFF_POINT.calculatePoint(mov.building.getPosition()),  BuildingWorkerMovable::tmpPathStep),
							dropProduced(mov -> EMaterialType.TRUNK)
						)
					),
					enterHome()
				)
		);
	}
}
