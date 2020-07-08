package jsettlers.logic.movable.specialist;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.AttackableHumanMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IPioneerMovable;
import jsettlers.logic.player.Player;

public class PioneerMovable extends AttackableHumanMovable implements IPioneerMovable {

	public PioneerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.PIONEER, position, player, movable);
	}

	@Override
	public boolean convertToBearer() {
		if(!player.equals(grid.getPlayerAt(position))) return false;

		createMovable(EMovableType.BEARER, player, position, grid, this);
		killMovable();

		return true;
	}
}
