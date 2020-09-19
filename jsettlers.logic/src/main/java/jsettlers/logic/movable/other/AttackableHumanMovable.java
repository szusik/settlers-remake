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
	}

	@Override
	public void stopOrStartWorking(boolean stop) {
		nextTarget = position;
		nextMoveToType = stop? EMoveToType.FORCED : EMoveToType.DEFAULT;
	}

	@Override
	public void moveToFerry(IFerryMovable ferry, ShortPoint2D entrancePosition) {
		if(!playerControlled) return;

		ferryToEnter = ferry;
		nextTarget = entrancePosition;
		nextMoveToType = EMoveToType.FORCED;
	}

	private ShortPoint2D targetingHealSpot = null;

	@Override
	public void heal() {
		health = getMovableType().getHealth();
	}

	@Override
	public boolean needsTreatment() {
		if(health == getMovableType().getHealth()) return false;
		if(path != null && path.getTargetPosition().equals(targetingHealSpot)) return false;
		return true;
	}

	@Override
	public boolean pingWounded(IHealerMovable healer) {
		if(!healer.requestTreatment(this)) return false;

		targetingHealSpot = new ShortPoint2D(healer.getPosition().x+2, healer.getPosition().y+2);
		moveTo(targetingHealSpot, EMoveToType.FORCED);
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
