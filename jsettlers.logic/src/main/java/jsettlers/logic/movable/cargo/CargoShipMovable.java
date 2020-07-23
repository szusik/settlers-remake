package jsettlers.logic.movable.cargo;

import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.IGraphicsCargoShip;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

public class CargoShipMovable extends CargoMovable implements IGraphicsCargoShip {

	public static final int CARGO_STACKS = 3;

	private final EMaterialType[]	cargoType	= new EMaterialType[CARGO_STACKS];
	private final int[]				cargoCount	= new int[CARGO_STACKS];

	public CargoShipMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.CARGO_SHIP, position, player, movable);
	}

	private boolean checkStackNumber(int stack) {
		return stack >= 0 && stack < CARGO_STACKS;
	}

	@Override
	public EMaterialType getCargoType(int stack) {
		return this.cargoType[stack];
	}

	@Override
	public int getCargoCount(int stack) {
		return this.cargoCount[stack];
	}

	@Override
	public int getNumberOfCargoStacks() {
		return CARGO_STACKS;
	}

	public void setCargoCount(int count, int stack) {
		if (checkStackNumber(stack)) {
			this.cargoCount[stack] = count;
			if (this.cargoCount[stack] < 0) {
				this.cargoCount[stack] = 0;
			} else if (this.cargoCount[stack] > 8) {
				this.cargoCount[stack] = 8;
			}
		}
	}

	public void setCargoType(EMaterialType cargo, int stack) {
		this.cargoType[stack] = cargo;
	}
}
