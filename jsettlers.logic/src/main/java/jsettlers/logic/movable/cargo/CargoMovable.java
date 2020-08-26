package jsettlers.logic.movable.cargo;

import java.util.Iterator;
import java.util.List;

import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.ITradeBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.other.AttackableMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

public abstract class CargoMovable extends AttackableMovable {

	private ETraderState state = ETraderState.JOBLESS;

	private ITradeBuilding         tradeBuilding;
	private Iterator<ShortPoint2D> waypoints;
	private final short waypointSearchRadius;
	private Function<Player, Stream<? extends ITradeBuilding>> allTradeBuildings;

	public CargoMovable(AbstractMovableGrid grid,
						EMovableType movableType, ShortPoint2D position,
						Player player, Movable movable, short waypointSearchRadius,
						Function<Player, Stream<? extends ITradeBuilding>> allTradeBuildings) {
		super(grid, movableType, position, player, movable);
		this.waypointSearchRadius = waypointSearchRadius;
		this.allTradeBuildings = allTradeBuildings;
	}

	@Override
	protected void action() {
		switch (state) {
			case JOBLESS:
				if (tradeBuilding == null || !tradeBuilding.needsTrader()) {
					this.tradeBuilding = findTradeBuildingWithWork();
				}

				if (this.tradeBuilding == null) { // no tradeBuilding found
					break;
				}

			case INIT_GOING_TO_TRADING_BUILDING:
				if (tradeBuilding.needsTrader() && super.goToPos(tradeBuilding.getPickUpPosition())) {
					state = ETraderState.GOING_TO_TRADING_BUILDING;
				} else {
					reset();
				}
				break;

			case GOING_TO_TRADING_BUILDING:
				if (loadUp(tradeBuilding)) {
					this.waypoints = tradeBuilding.getWaypointsIterator();
					state = ETraderState.GOING_TO_TARGET;
				} else {
					state = ETraderState.JOBLESS;
					break;
				}

			case GOING_TO_TARGET:
				if (!goToNextWaypoint()) { // no waypoint left
					dropMaterialIfPossible();
					waypoints = null;
					state = ETraderState.INIT_GOING_TO_TRADING_BUILDING;
				}
				break;

			default:
				break;
		}
	}

	/**
	 *
	 * @return
	 * true if the tradeBuilding had material, the unit is loaded and should proceed to the target destination
	 */
	protected abstract boolean loadUp(ITradeBuilding tradeBuilding);

	private ITradeBuilding findTradeBuildingWithWork() {
		List<? extends ITradeBuilding> tradeBuilding = allTradeBuildings.apply(player).filter(ITradeBuilding::needsTrader).collect(Collectors.toList());

		if (!tradeBuilding.isEmpty()) { // randomly distribute the donkeys onto the markets needing them
			return tradeBuilding.get(MatchConstants.random().nextInt(tradeBuilding.size()));
		} else {
			return null;
		}
	}

	private boolean goToNextWaypoint() {
		while (waypoints.hasNext()) {
			ShortPoint2D nextPosition = waypoints.next();
			if (super.preSearchPath(true, nextPosition.x, nextPosition.y, waypointSearchRadius, ESearchType.VALID_FREE_POSITION)) {
				super.followPresearchedPath();
				return true;
			}
		}

		return false;
	}

	protected void reset() {
		dropMaterialIfPossible();
		tradeBuilding = null;
		waypoints = null;
		state = ETraderState.JOBLESS;
	}

	protected abstract void dropMaterialIfPossible();

	public ETraderState getState() {
		return state;
	}

	protected enum ETraderState {
		JOBLESS,
		INIT_GOING_TO_TRADING_BUILDING,
		GOING_TO_TRADING_BUILDING,
		GOING_TO_TARGET,
		DEAD
	}
}
