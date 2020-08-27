package jsettlers.logic.movable.military;

import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IAttackable;
import jsettlers.logic.player.Player;

public class InfantryMovable extends SoldierMovable {
	private static final float INFANTRY_ATTACK_DURATION = 1;

	public InfantryMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable) {
		super(grid, movableType, position, player, movable, (short)0, Constants.SOLDIER_SEARCH_RADIUS, Constants.SOLDIER_SEARCH_RADIUS);
	}

	@Override
	protected boolean isEnemyAttackable(IAttackable enemy, boolean isInTower) {
		if(!enemy.isAlive()) return false;
		int maxDistance = position.getOnGridDistTo(enemy.getPosition());
		return (maxDistance == 1 || (!enemy.isTower() && getMovableType().isPikeman() && maxDistance <= 2));
	}

	@Override
	protected void startAttackAnimation(IAttackable enemy) {
		float duration = INFANTRY_ATTACK_DURATION;
		if(hasEffect(EEffectType.MOTIVATE_SWORDSMAN))  duration *= EEffectType.MOTIVATE_SWORDSMAN_ANIMATION_FACTOR;
		super.playAction(EMovableAction.ACTION1, duration);
	}

	@Override
	protected void hitEnemy(IAttackable enemy) {
		enemy.receiveHit(getMovableType().getStrength() * getCombatStrength(), position, player.playerId);
		// decrease the enemy's health
	}
}
