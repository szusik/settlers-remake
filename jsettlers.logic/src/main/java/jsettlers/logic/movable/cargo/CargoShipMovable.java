package jsettlers.logic.movable.cargo;

import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.IGraphicsCargoShip;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

public class CargoShipMovable extends CargoMovable implements IGraphicsCargoShip {

	public CargoShipMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.CARGO_SHIP, position, player, movable);
	}

	@Override
	public EMaterialType getCargoType(int stack) {
		return strategy.getCargoType(stack);
	}

	@Override
	public int getCargoCount(int stack) {
		return strategy.getCargoCount(stack);
	}

	@Override
	public int getNumberOfCargoStacks() {
		return strategy.getNumberOfCargoStacks();
	}
}
