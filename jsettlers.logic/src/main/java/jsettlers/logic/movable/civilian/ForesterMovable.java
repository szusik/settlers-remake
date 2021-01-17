package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
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

public class ForesterMovable extends BuildingWorkerMovable {

	public ForesterMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable replace) {
		super(grid, movableType, position, player, replace, tree);
	}
	/*
	<!-- going home job -->
	<job name="gohome" 		type="GO_TO" 				successjob="hide" 			failjob="waithome" 	dx="3" dy="2"/>
	<job name="waithome" 	type="WAIT" 				successjob="gohome" 		failjob="waithome" 	time="1.0"/>
	<job name="hide" 		type="HIDE" 				successjob="start" 			failjob="start"/>

	<job name="start" 		type="WAIT" 				successjob="set_tree" 		failjob="show" 		time="4.0"/>
	<job name="set_tree" 	type="SET_MATERIAL" 		successjob="presearch" 		failjob="presearch" material="TREE"/>
	<job name="presearch" 	type="PRE_SEARCH_IN_AREA" 	successjob="show" 			failjob="start" 	search="PLANTABLE_TREE" dx="3" dy="2"/>

	<job name="show" 		type="SHOW" 				successjob="search"			failjob="hide" 		dx="3" dy="2"/>

	<job name="search" 		type="FOLLOW_SEARCHED"		successjob="look" 			failjob="hide" 		/>
	<job name="look" 		type="LOOK_AT" 				successjob="plant" 			failjob="gohome" 	direction="NORTH_WEST"/>
	<job name="plant" 		type="PLAY_ACTION1" 		successjob="unset_tree" 	failjob="gohome"	time="3.0"/>
	<job name="unset_tree" 	type="SET_MATERIAL" 		successjob="execute" 		failjob="gohome" 	material="NO_MATERIAL"/>
	<job name="execute" 	type="EXECUTE" 				successjob="gohome" 		failjob="gohome" 	search="PLANTABLE_TREE"/>*/

	private static final Root<ForesterMovable> tree = new Root<>(createForesterBehaviour());

	private static Node<ForesterMovable> createForesterBehaviour() {
		return guardSelector(
				fleeIfNecessary(),
				handleBuildingDestroyedGuard(),
				guard(mov -> mov.building!=null,
					sequence(
						enterHome(),
						repeat(mov -> true,
							sequence(
								sleep(4000),
								setMaterialNode(EMaterialType.TREE),
								preSearchPath(false, ESearchType.PLANTABLE_TREE),
								leaveHome(),
								ignoreFailure(
									sequence(
										followPresearchedPath(BuildingWorkerMovable::tmpPathStep), // TODO
										setDirectionNode(mov -> EDirection.NORTH_WEST),
										playAction(EMovableAction.ACTION1, mov -> (short)3000),
										setMaterialNode(EMaterialType.NO_MATERIAL),
										executeSearch(ESearchType.PLANTABLE_TREE)
									)
								),
								enterHome()
							)
						)
					)
				),
				registerMovableGuard(),
				doingNothingGuard()
		);
	}
}
