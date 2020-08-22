package jsettlers.logic.movable.specialist;

import java.util.BitSet;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.movable.interfaces.IThiefMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

public class ThiefMovable extends AttackableHumanMovable implements IThiefMovable {

	private BitSet uncoveredBy = new BitSet();

	public ThiefMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.THIEF, position, player, movable);
	}


	@Override
	public boolean isUncoveredBy(byte teamId) {
		return uncoveredBy.get(teamId);
	}

	@Override
	public void uncoveredBy(byte teamId) {
		uncoveredBy.set(teamId);
	}
}
