/*******************************************************************************
 * Copyright (c) 2015
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.logic.movable.strategies.military;

import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.ESoldierClass;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.IAttackable;
import jsettlers.logic.movable.military.InfantryMovable;

/**
 * Strategy for swordsman and pikeman {@link Movable}s.
 * 
 * @author Andreas Eberle
 * 
 */
public final class InfantryStrategy extends SoldierStrategy<InfantryMovable> {
	private static final long serialVersionUID = -2367165698305111060L;
	private static final float INFANTRY_ATTACK_DURATION = 1;

	public InfantryStrategy(InfantryMovable movable, EMovableType movableType) {
		super(movable, movableType);
	}

	@Override
	protected boolean isEnemyAttackable(IAttackable enemy, boolean isInTower) {
		if(!enemy.isAlive()) return false;
		int maxDistance = movable.getPosition().getOnGridDistTo(enemy.getPosition());
		return (maxDistance == 1 || (!enemy.isTower() && movable.getMovableType().isPikeman() && maxDistance <= 2));
	}

	@Override
	protected void startAttackAnimation(IAttackable enemy) {
		float duration = INFANTRY_ATTACK_DURATION;
		if(movable.hasEffect(EEffectType.MOTIVATE_SWORDSMAN))  duration *= EEffectType.MOTIVATE_SWORDSMAN_ANIMATION_FACTOR;
		super.playAction(EMovableAction.ACTION1, duration);
	}

	@Override
	protected void hitEnemy(IAttackable enemy) {
		enemy.receiveHit(movable.getMovableType().getStrength() * getCombatStrength(), movable.getPosition(), movable.getPlayer().playerId);
		// decrease the enemy's health
	}

	@Override
	protected short getMaxSearchDistance(boolean isInTower) {
		return Constants.SOLDIER_SEARCH_RADIUS;
	}

	@Override
	protected short getMinSearchDistance() {
		return 0;
	}
}
