package jsettlers.logic.movable.civilian;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.ICivilianMovable;
import jsettlers.logic.player.Player;

public class CivilianMovable extends Movable implements ICivilianMovable {

	private boolean fleeing;

	protected CivilianMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable replace) {
		super(grid, movableType, position, player, replace);

		fleeing = false;
	}

	@Override
	public void checkPlayerOfPosition() {
		// civilians are only allowed on their players ground => abort current task and flee to nearest own ground
		fleeing = grid.getPlayerAt(position) != player;
	}

	public boolean isFleeing() {
		return fleeing;
	}
}
