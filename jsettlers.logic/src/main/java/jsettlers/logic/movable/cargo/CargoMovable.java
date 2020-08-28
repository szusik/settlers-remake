package jsettlers.logic.movable.cargo;

import java.util.Iterator;
import java.util.List;

import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper;
import jsettlers.algorithms.simplebehaviortree.IBooleanConditionFunction;
import jsettlers.algorithms.simplebehaviortree.IShortPoint2DSupplier;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.ITradeBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.other.AttackableMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public abstract class CargoMovable extends AttackableMovable {

	protected ITradeBuilding tradeBuilding = null;
	protected Iterator<ShortPoint2D> waypoints;

	public CargoMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable) {
		super(grid, movableType, position, player, movable, tree);
	}

	private static Root<CargoMovable> tree = new Root<>(createCargoBehaviour());

	private static Node<CargoMovable> createCargoBehaviour() {
		return sequence(
				repeat(CargoMovable::hasTrader,
					sequence(
						goToPos(mov -> mov.tradeBuilding.getPickUpPosition(), mov -> true),
						condition(CargoMovable::loadUp),
						BehaviorTreeHelper.action(mov -> {
							mov.waypoints = mov.tradeBuilding.getWaypointsIterator();
							mov.lostCargo = false;
							mov.pathStep = (mov2) -> !((CargoMovable)mov2).lostCargo;
						}),
						ignoreFailure(repeat(mov -> mov.waypoints.hasNext(),
							sequence(
								condition(mov -> {
									ShortPoint2D nextPosition = mov.waypoints.next();
									if (mov.preSearchPath(true, nextPosition.x, nextPosition.y, mov.getWaypointSearchRadius(), ESearchType.VALID_FREE_POSITION)) {
										mov.followPresearchedPath();
										return true;
									}
									return false;
								}),
								waitFor(condition(mov -> mov.path == null)),
								condition(mov -> !mov.aborted)
							)
						)),
						BehaviorTreeHelper.action(mov -> {
							mov.pathStep = null;
							mov.waypoints = null;
						}),
						BehaviorTreeHelper.action(CargoMovable::dropMaterialIfPossible)
					)
				),
				// choose a new trader if the old one is no longer requesting
				selector(
					condition(CargoMovable::findNewTrader),
					BehaviorTreeHelper.sleep(1000)
				)
		);
	}

	private boolean hasTrader() {
		return tradeBuilding != null && tradeBuilding.needsTrader();
	}

	private boolean findNewTrader() {
		List<? extends ITradeBuilding> newTradeBuilding = getAllTradeBuildings().filter(ITradeBuilding::needsTrader).collect(Collectors.toList());

		if (!newTradeBuilding.isEmpty()) { // randomly distribute the donkeys onto the markets needing them
			tradeBuilding = newTradeBuilding.get(MatchConstants.random().nextInt(newTradeBuilding.size()));
			return true;
		} else {
			tradeBuilding = null;
			return false;
		}
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		dropMaterialIfPossible();
	}

	protected boolean lostCargo = false;

	/**
	 *
	 * @return
	 * true if the tradeBuilding had material, the unit is loaded and should proceed to the target destination
	 */
	protected abstract boolean loadUp();

	protected abstract void dropMaterialIfPossible();

	protected abstract Stream<? extends ITradeBuilding> getAllTradeBuildings();

	protected abstract short getWaypointSearchRadius();
}
