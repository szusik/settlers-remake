package jsettlers.logic.movable.cargo;

import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

public class DonkeyMovable extends CargoMovable {

	public static final int CARGO_COUNT = 2;

	private final EMaterialType[] cargo = new EMaterialType[CARGO_COUNT];

	public DonkeyMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable) {
		super(grid, movableType, position, player, movable);
	}

	public void setCargo(int index, EMaterialType material) {
		cargo[index] = material;
	}

	public EMaterialType getCargo(int index) {
		return cargo[index];
	}
}
