package jsettlers.logic.movable.military;

import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IAttackable;
import jsettlers.logic.movable.interfaces.IBowmanMovable;
import jsettlers.logic.player.Player;

public class BowmanMovable extends SoldierMovable implements IBowmanMovable {
	private static final float BOWMAN_ATTACK_DURATION = 1.2f;

	public BowmanMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable) {
		super(grid, movableType, position, player, movable, Constants.BOWMAN_MIN_ATTACK_DISTANCE, Constants.BOWMAN_IN_TOWER_ATTACK_RADIUS, Constants.BOWMAN_ATTACK_RADIUS);
	}

	@Override
	public void convertToPioneer() {
		createMovable(EMovableType.PIONEER, player, position, grid, this);
		killMovable();
	}

	@Override
	protected boolean isEnemyAttackable(IAttackable enemy, boolean isInTower) {
		if (!enemy.isAlive()){
			return false;
		}

		ShortPoint2D pos = getAttackPosition();
		ShortPoint2D enemyPos = enemy.getPosition();

		int distance = pos.getOnGridDistTo(enemyPos);

		if (isInTower) {
			return Constants.BOWMAN_MIN_ATTACK_DISTANCE <= distance && distance <= Constants.BOWMAN_IN_TOWER_ATTACK_RADIUS;
		} else {
			return Constants.BOWMAN_MIN_ATTACK_DISTANCE <= distance && distance <= Constants.BOWMAN_ATTACK_RADIUS;
		}
	}

	@Override
	protected void startAttackAnimation(IAttackable enemy) {
		super.playAction(EMovableAction.ACTION1, BOWMAN_ATTACK_DURATION);

		if(!hasEffect(EEffectType.NO_ARROWS)) {
			grid.addArrowObject(enemy.getPosition(), position, player.playerId,
					getMovableType().getStrength() * getCombatStrength());
		}
	}
}
