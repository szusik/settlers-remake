package jsettlers.logic.movable.military;

import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

public class InfantryMovable extends SoldierMovable {
	private static final float INFANTRY_ATTACK_DURATION = 1;

	public InfantryMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable) {
		super(grid, movableType, position, player, movable);
	}

	@Override
	protected boolean isEnemyAttackable() {
		if(!enemy.isAlive()) return false;
		int maxDistance = position.getOnGridDistTo(enemy.getPosition());
		return (maxDistance == 1 || (!enemy.isTower() && getMovableType().isPikeman() && maxDistance <= 2)) || (defending && maxDistance <= 3);
	}

	@Override
	protected short getAttackDuration() {
		short duration = (short)(INFANTRY_ATTACK_DURATION*1000);
		if(hasEffect(EEffectType.MOTIVATE_SWORDSMAN))  duration *= EEffectType.MOTIVATE_SWORDSMAN_ANIMATION_FACTOR;

		return duration;
	}

	@Override
	protected short getMinSearchDistance() {
		return 0;
	}

	@Override
	protected void hitEnemy() {
		enemy.receiveHit(getMovableType().getStrength() * getCombatStrength(), position, player.playerId);
		// decrease the enemy's health
	}
}
