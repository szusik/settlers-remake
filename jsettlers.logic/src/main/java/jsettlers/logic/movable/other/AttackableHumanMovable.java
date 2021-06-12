package jsettlers.logic.movable.other;

import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IAttackableHumanMovable;
import jsettlers.logic.movable.interfaces.IFerryMovable;
import jsettlers.logic.movable.interfaces.IHealerMovable;
import jsettlers.logic.player.Player;

public class AttackableHumanMovable extends AttackableMovable implements IAttackableHumanMovable {

	protected EMoveToType nextMoveToType;
	protected ShortPoint2D nextTarget = null;
	protected boolean goingToHealer = false;

	public AttackableHumanMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable, Root<? extends AttackableHumanMovable> behaviour) {
		super(grid, movableType, position, player, movable, behaviour);
	}

	@Override
	public void leaveFerryAt(ShortPoint2D position) {
		this.position = position;
		setState(Movable.EMovableState.DOING_NOTHING);

		grid.enterPosition(position, this, true);
	}

	@Override
	public void moveTo(ShortPoint2D targetPosition, EMoveToType moveToType) {
		if(!playerControlled) return;

		nextTarget = targetPosition;
		nextMoveToType = moveToType;
		goingToHealer = false;
	}

	@Override
	public void stopOrStartWorking(boolean stop) {
		if(!playerControlled) return;

		nextTarget = position;
		nextMoveToType = stop? EMoveToType.FORCED : EMoveToType.DEFAULT;
		goingToHealer = false;
	}

	@Override
	public void moveToFerry(IFerryMovable ferry, ShortPoint2D entrancePosition) {
		if(!playerControlled) return;

		ferryToEnter = ferry;
		nextTarget = entrancePosition;
		nextMoveToType = EMoveToType.FORCED;
		goingToHealer = false;
	}

	@Override
	public void heal() {
		health = getMovableType().getHealth();
	}

	@Override
	public boolean isGoingToTreatment() {
		return goingToHealer;
	}

	@Override
	public boolean needsTreatment() {
		if(health == getMovableType().getHealth()) return false;
		if(!playerControlled) return false;
		return true;
	}

	@Override
	public boolean pingWounded(IHealerMovable healer) {
		if(!needsTreatment() || isGoingToTreatment()) return false;

		nextTarget = healer.getHealSpot();
		nextMoveToType = EMoveToType.FORCED;
		goingToHealer = true;
		return true;
	}

	@Override
	public void defectTo(Player player) {
		Movable.createMovable(getMovableType(), player, position, grid, this);
	}

	@Override
	public ShortPoint2D getFoWPosition() {
		if(isOnFerry()) return null;

		return position;
	}
}
