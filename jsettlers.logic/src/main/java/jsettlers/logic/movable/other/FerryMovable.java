package jsettlers.logic.movable.other;

import java.util.ArrayList;
import java.util.List;

import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.IGraphicsFerry;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IAttackableHumanMovable;
import jsettlers.logic.movable.interfaces.IFerryMovable;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.player.Player;

import static java8.util.stream.StreamSupport.stream;

public class FerryMovable extends AttackableMovable implements IGraphicsFerry, IFerryMovable {

	private static final int MAX_NUMBER_OF_PASSENGERS = 7;

	private final List<IAttackableHumanMovable> passengers = new ArrayList<>(MAX_NUMBER_OF_PASSENGERS);

	public FerryMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.FERRY, position, player, movable);
	}

	@Override
	public List<? extends IAttackableHumanMovable> getPassengers() {
		return passengers;
	}

	@Override
	public boolean addPassenger(IAttackableHumanMovable movable) {
		if (passengers.size() == MAX_NUMBER_OF_PASSENGERS) return false;

		passengers.add(movable);
		return true;
	}

	@Override
	public void unloadFerry() {
		if (passengers.isEmpty()) {
			return;
		}

		ShortPoint2D position = getPosition();

		HexGridArea.stream(position.x, position.y, 2, Constants.MAX_FERRY_UNLOADING_RADIUS)
				.filterBounds(grid.getWidth(), grid.getHeight())
				.filter((x, y) -> !grid.isWater(x, y))
				.iterate((x, y) -> {
					IAttackableHumanMovable passenger = passengers.get(passengers.size() - 1);

					if (grid.isValidPosition(passenger, x, y) && grid.isFreePosition(x,y)) {
						passenger.leaveFerryAt(new ShortPoint2D(x, y));
						passengers.remove(passengers.size() - 1);
					}

					return !passengers.isEmpty();
				});
	}

	@Override
	protected void killMovable() {
		super.killMovable();

		stream(passengers).forEach(ILogicMovable::kill);
	}
}
