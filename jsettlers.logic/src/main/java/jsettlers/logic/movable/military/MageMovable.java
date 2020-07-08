package jsettlers.logic.movable.military;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.ESpellType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.AttackableHumanMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IMageMovable;
import jsettlers.logic.movable.strategies.military.MageStrategy;
import jsettlers.logic.player.Player;

public class MageMovable extends AttackableHumanMovable implements IMageMovable {

	public MageMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.MAGE, position, player, movable);
	}

	@Override
	public void moveToCast(ShortPoint2D at, ESpellType spell) {
		super.moveTo(at, EMoveToType.DEFAULT);
		((MageStrategy)strategy).castSpellAt(spell, at);
	}


	/**
	 * Tests if this movable can receive moveTo requests and if so, directs it to go to the given position.
	 *
	 * @param targetPosition
	 * 		Desired position the movable should move to
	 */
	@Override
	public void moveTo(ShortPoint2D targetPosition, EMoveToType moveToType) {
		super.moveTo(targetPosition, moveToType);
		((MageStrategy) strategy).castSpellAt(null, null);
	}
}
