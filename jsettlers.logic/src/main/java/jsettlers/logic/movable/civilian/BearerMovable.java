package jsettlers.logic.movable.civilian;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IBearerMovable;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.player.Player;

public class BearerMovable extends Movable implements IBearerMovable {

	public BearerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.BEARER, position, player, movable);
	}

	@Override
	public ILogicMovable convertTo(EMovableType newMovableType) {
		Movable newMovable = createMovable(newMovableType, player, position, grid, this);
		killMovable();
		return newMovable;
	}
}
