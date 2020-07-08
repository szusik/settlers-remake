package jsettlers.logic.movable.military;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.AttackableHumanMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IBowmanMovable;
import jsettlers.logic.player.Player;

public class BowmanMovable extends AttackableHumanMovable implements IBowmanMovable {

	public BowmanMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable) {
		super(grid, movableType, position, player, movable);
	}

	@Override
	public void convertToPioneer() {
		createMovable(EMovableType.PIONEER, player, position, grid, this);
		killMovable();
	}
}
