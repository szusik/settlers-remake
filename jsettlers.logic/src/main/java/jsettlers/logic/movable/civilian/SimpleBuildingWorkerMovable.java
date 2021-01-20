package jsettlers.logic.movable.civilian;

import java.util.EnumMap;
import java.util.Map;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.landscape.EResourceType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
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
		trees.put(EMovableType.WATERWORKER, new Root<>(createWaterworkerBehaviour()));
		trees.put(EMovableType.FISHERMAN, new Root<>(createFishermanBehaviour()));
		trees.put(EMovableType.STONECUTTER, new Root<>(createStonecutterBehaviour()));
		trees.put(EMovableType.WINEGROWER, new Root<>(createWinegrowerBehaviour()));
		trees.put(EMovableType.FARMER, new Root<>(createFarmerBehaviour()));
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

	private static final short LUMBERJACK_ACTION1_DURATION = (short)1000;

	private static Node<SimpleBuildingWorkerMovable> playAction3Times(EMovableAction action, short duration) {
		return sequence(
				playAction(action, duration),
				playAction(action, duration),
				playAction(action, duration)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createLumberjackBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(3000),
					waitFor(
						sequence(
							isAllowedToWork(),
							outputStackNotFull(EMaterialType.TRUNK),
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
									playAction3Times(EMovableAction.ACTION1, LUMBERJACK_ACTION1_DURATION),
									playAction3Times(EMovableAction.ACTION1, LUMBERJACK_ACTION1_DURATION),
									goInDirectionWaitFree(EDirection.WEST, BuildingWorkerMovable::tmpPathStep)
								),
								sequence(
									setDirectionNode(mov -> EDirection.NORTH_WEST),
									playAction3Times(EMovableAction.ACTION1, LUMBERJACK_ACTION1_DURATION),
									playAction3Times(EMovableAction.ACTION1, LUMBERJACK_ACTION1_DURATION)
								)
							),
							executeSearch(ESearchType.CUTTABLE_TREE),
							ignoreFailure(
								sequence(
									goInDirectionWaitFree(EDirection.SOUTH_EAST, BuildingWorkerMovable::tmpPathStep),
									goInDirectionWaitFree(EDirection.NORTH_EAST, BuildingWorkerMovable::tmpPathStep),
									setDirectionNode(mov -> EDirection.NORTH_WEST),
									playAction3Times(EMovableAction.ACTION1, LUMBERJACK_ACTION1_DURATION),
									goInDirectionWaitFree(EDirection.NORTH_EAST, BuildingWorkerMovable::tmpPathStep),
									setDirectionNode(mov -> EDirection.NORTH_WEST),
									playAction3Times(EMovableAction.ACTION1, LUMBERJACK_ACTION1_DURATION),
									goInDirectionWaitFree(EDirection.SOUTH_WEST, BuildingWorkerMovable::tmpPathStep),
									setDirectionNode(mov -> EDirection.NORTH_WEST),
									take(mov -> EMaterialType.TRUNK, mov -> false, mov -> {})
								)
							),
							setMaterialNode(EMaterialType.TRUNK),
							goToOutputStack(EMaterialType.TRUNK, BuildingWorkerMovable::tmpPathStep),
							dropProduced(mov -> EMaterialType.TRUNK)
						)
					),
					enterHome()
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createWaterworkerBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(3000),
					waitFor(
						sequence(
							isAllowedToWork(),
							outputStackNotFull(EMaterialType.WATER),
							preSearchPath(false, ESearchType.RIVER)
						)
					),
					setMaterialNode(EMaterialType.EMPTY_BUCKET),
					show(),
					ignoreFailure(
						sequence(
							followPresearchedPath(BuildingWorkerMovable::tmpPathStep),
							lookAtSearched(ESearchType.RIVER),
							playAction(EMovableAction.ACTION1, (short)1000),
							setMaterialNode(EMaterialType.WATER),
							goToOutputStack(EMaterialType.WATER, BuildingWorkerMovable::tmpPathStep),
							dropProduced(mov -> EMaterialType.WATER)
						)
					),
					enterHome()
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createFishermanBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(3000),
					waitFor(
						sequence(
							isAllowedToWork(),
							outputStackNotFull(EMaterialType.FISH),
							preSearchPath(false, ESearchType.FISHABLE)
						)
					),
					setMaterialNode(EMaterialType.NO_MATERIAL),
					show(),
					ignoreFailure(
						sequence(
							followPresearchedPath(BuildingWorkerMovable::tmpPathStep),
							lookAtSearched(ESearchType.FISHABLE),
							playAction(EMovableAction.ACTION1, (short)1500),
							selector(
								//try taking fish
								condition(mov -> {
									SimpleBuildingWorkerMovable smov = (SimpleBuildingWorkerMovable) mov;
									return smov.grid.tryTakingResource(smov.getDirection().getNextHexPoint(smov.position), EResourceType.FISH);
								}),
								sequence(// fishing failed
									playAction(EMovableAction.ACTION3, (short)2000),
									alwaysFail()
								)
							),
							// fishing succeeded
							playAction(EMovableAction.ACTION2, (short)1000),
							setMaterialNode(EMaterialType.FISH),
							goToOutputStack(EMaterialType.FISH, BuildingWorkerMovable::tmpPathStep),
							setDirectionNode(mov -> EDirection.SOUTH_WEST),
							dropProduced(mov -> EMaterialType.FISH)
						)
					),
					enterHome()
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createStonecutterBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(3000),
					waitFor(
						sequence(
							isAllowedToWork(),
							outputStackNotFull(EMaterialType.STONE),
							preSearchPath(true, ESearchType.CUTTABLE_STONE)
						)
					),
					setMaterialNode(EMaterialType.NO_MATERIAL),
					show(),
					ignoreFailure(
						sequence(
							followPresearchedPathMarkTarget(BuildingWorkerMovable::tmpPathStep),
							setDirectionNode(mov -> EDirection.SOUTH_WEST),
							playAction3Times(EMovableAction.ACTION1, (short)750),
							playAction3Times(EMovableAction.ACTION1, (short)750),
							executeSearch(ESearchType.CUTTABLE_STONE),
							take(mov -> EMaterialType.STONE, mov -> false, mov -> {}),
							goToOutputStack(EMaterialType.STONE, BuildingWorkerMovable::tmpPathStep),
							dropProduced(mov -> EMaterialType.STONE)
						)
					),
					enterHome()
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createWinegrowerBehaviour() {
		return defaultWorkCycle(
				ignoreFailure(
					sequence(
						waitFor(isAllowedToWork()),
						selector(
							sequence(
								outputStackNotFull(EMaterialType.WINE),
								preSearchPath(true, ESearchType.HARVESTABLE_WINE),
								ignoreFailure(
									sequence(
										setMaterialNode(EMaterialType.BASKET),
										show(),
										followPresearchedPathMarkTarget(BuildingWorkerMovable::tmpPathStep),
										setDirectionNode(mov -> EDirection.NORTH_WEST),
										take(mov -> EMaterialType.BASKET, mov -> false, mov -> {}),
										executeSearch(ESearchType.HARVESTABLE_WINE),
										enterHome(),
										sleep(3000),
										setMaterialNode(EMaterialType.WINE),
										show(),
										goToOutputStack(EMaterialType.WINE, BuildingWorkerMovable::tmpPathStep),
										setDirectionNode(mov -> EDirection.NORTH_EAST),
										dropProduced(mov -> EMaterialType.WINE)
									)
								)
							),
							sequence(
								preSearchPathNoWarning(true, ESearchType.PLANTABLE_WINE),
								ignoreFailure(
									sequence(
										setMaterialNode(EMaterialType.PLANT),
										show(),
										followPresearchedPathMarkTarget(BuildingWorkerMovable::tmpPathStep),
										setDirectionNode(mov -> EDirection.SOUTH_WEST),
										playAction(EMovableAction.ACTION1, (short)4000),
										executeSearch(ESearchType.PLANTABLE_WINE),
										setMaterialNode(EMaterialType.NO_MATERIAL)
									)
								)
							),
							sequence(
								sleep(1000),
								alwaysFail()
							)
						),
						enterHome(),
						sleep(8000)
					)
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createFarmerBehaviour() {
		return defaultWorkCycle(
				ignoreFailure(
					sequence(
						waitFor(isAllowedToWork()),
						selector(
							sequence(
								outputStackNotFull(EMaterialType.CROP),
								preSearchPath(true, ESearchType.CUTTABLE_CORN),
								ignoreFailure(
									sequence(
										show(),
										setMaterialNode(EMaterialType.SCYTHE),
										followPresearchedPathMarkTarget(BuildingWorkerMovable::tmpPathStep),

										setDirectionNode(mov -> EDirection.SOUTH_WEST),
										playAction(EMovableAction.ACTION1, (short)1000),
										setDirectionNode(mov -> EDirection.NORTH_WEST),
										playAction(EMovableAction.ACTION1, (short)1000),
										setDirectionNode(mov -> EDirection.NORTH_EAST),
										playAction(EMovableAction.ACTION1, (short)1000),
										setDirectionNode(mov -> EDirection.SOUTH_EAST),
										playAction(EMovableAction.ACTION1, (short)1000),

										executeSearch(ESearchType.CUTTABLE_CORN),
										setDirectionNode(mov -> EDirection.NORTH_EAST),
										setMaterialNode(EMaterialType.NO_MATERIAL),
										take(mov -> EMaterialType.CROP, mov -> false, mov -> {}),
										goToOutputStack(EMaterialType.CROP, BuildingWorkerMovable::tmpPathStep),
										setDirectionNode(mov -> EDirection.NORTH_EAST),
										drop(mov -> EMaterialType.CROP, BuildingWorkerMovable::tmpPathStep)
									)
								)
							),
							sequence(
								preSearchPathNoWarning(true, ESearchType.PLANTABLE_CORN),
								ignoreFailure(
									sequence(
										show(),
										setMaterialNode(EMaterialType.PLANT),
										followPresearchedPathMarkTarget(BuildingWorkerMovable::tmpPathStep),

										setDirectionNode(mov -> EDirection.NORTH_EAST),
										playAction(EMovableAction.ACTION2, (short)1400),
										setDirectionNode(mov -> EDirection.SOUTH_EAST),
										playAction(EMovableAction.ACTION2, (short)1400),
										setDirectionNode(mov -> EDirection.SOUTH_WEST),
										playAction(EMovableAction.ACTION2, (short)1400),
										setDirectionNode(mov -> EDirection.NORTH_WEST),
										playAction(EMovableAction.ACTION2, (short)1400),

										executeSearch(ESearchType.PLANTABLE_CORN),
										setMaterialNode(EMaterialType.NO_MATERIAL)
									)
								)
							),
							sequence(
								sleep(1000),
								alwaysFail()
							)
						),
						enterHome(),
						sleep(8000)
					)
				)
		);
	}
}
