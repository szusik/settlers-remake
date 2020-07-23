package jsettlers.logic.movable.other;

import java.util.List;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.IGraphicsFerry;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IAttackableHumanMovable;
import jsettlers.logic.movable.interfaces.IFerryMovable;
import jsettlers.logic.player.Player;

public class FerryMovable extends AttackableMovable implements IGraphicsFerry, IFerryMovable {

	public FerryMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.FERRY, position, player, movable);
	}

	@Override
	public List<? extends IAttackableHumanMovable> getPassengers() {
		return strategy.getPassengers();
	}

	@Override
	public boolean addPassenger(IAttackableHumanMovable movable) {
		return strategy.addPassenger(movable);
	}

	@Override
	public void unloadFerry() {
		if (this.getMovableType() != EMovableType.FERRY) {
			return;
		}
		strategy.unloadFerry();
	}

}
