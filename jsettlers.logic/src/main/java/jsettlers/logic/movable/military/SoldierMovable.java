package jsettlers.logic.movable.military;

import jsettlers.algorithms.path.Path;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.buildings.OccupierPlace;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.military.occupying.IOccupyableBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.EGoInDirectionMode;
import jsettlers.logic.movable.interfaces.IAttackable;
import jsettlers.logic.movable.interfaces.IThiefMovable;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.ISoldierMovable;
import jsettlers.logic.movable.specialist.ThiefMovable;
import jsettlers.logic.player.Player;

public abstract class SoldierMovable extends AttackableHumanMovable implements ISoldierMovable {

	private ESoldierState state = ESoldierState.SEARCH_FOR_ENEMIES;
	private IOccupyableBuilding building;
	private IAttackable enemy;
	private ShortPoint2D oldPathTarget;

	private boolean inSaveGotoMode = false;

	private boolean isInTower;

	private ShortPoint2D inTowerAttackPosition;

	private boolean defending;

	private short minSearchDistance;
	private short towerMaxSearchDistance;
	private short defaultMaxSearchDistance;

	public SoldierMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable, short minSearchDistance, short towerMaxSearchDistance, short defaultMaxSearchDistance) {
		super(grid, movableType, position, player, movable);

		this.minSearchDistance = minSearchDistance;
		this.towerMaxSearchDistance = towerMaxSearchDistance;
		this.defaultMaxSearchDistance = defaultMaxSearchDistance;
	}

	@Override
	protected void action() {
		switch (state) {
			case AGGRESSIVE:
				break;

			case FORCED_MOVE:
				break;

			case HITTING:
				if (!isEnemyAttackable(enemy, isInTower)) {
					changeStateTo(SoldierMovable.ESoldierState.SEARCH_FOR_ENEMIES);
				} else {
					hitEnemy(enemy); // after the animation, execute the actual hit.

					if (state != SoldierMovable.ESoldierState.HITTING) {
						break; // the soldier could have entered an attacked tower
					}

					if (!enemy.isAlive()) {
						enemy = null;
						changeStateTo(SoldierMovable.ESoldierState.SEARCH_FOR_ENEMIES);
						break; // don't directly walk on the enemy's position, because there may be others to walk in first
					}
				}
				changeStateTo(SoldierMovable.ESoldierState.SEARCH_FOR_ENEMIES);
			case SEARCH_FOR_ENEMIES:
				if(hasEffect(EEffectType.FROZEN)) break; // we can neither move nor hit

				IAttackable oldEnemy = enemy;
				enemy = grid.getEnemyInSearchArea(getAttackPosition(), this, minSearchDistance, isInTower? towerMaxSearchDistance : defaultMaxSearchDistance, !defending);
				if(enemy instanceof IThiefMovable) ((ThiefMovable)enemy).uncoveredBy(player.getTeamId());

				// check if we have a new enemy. If so, go in unsafe mode again.
				if (oldEnemy != null && oldEnemy != enemy) {
					inSaveGotoMode = false;
				}

				// no enemy found, go back in normal mode
				if (enemy == null) {
					if (minSearchDistance > 0) {
						IAttackable toCloseEnemy = grid.getEnemyInSearchArea(
								getAttackPosition(), this, (short) 0, minSearchDistance, !defending);
						if (toCloseEnemy != null) {
							if (!isInTower) { // we are in danger because an enemy entered our range where we can't attack => run away
								EDirection escapeDirection = EDirection.getApproxDirection(toCloseEnemy.getPosition(), position);
								goInDirection(escapeDirection, EGoInDirectionMode.GO_IF_ALLOWED_AND_FREE);
								moveTo(null, EMoveToType.DEFAULT); // reset moveToRequest, so the soldier doesn't go there after fleeing.

							} // else { // we are in the tower, so wait and check again next time.

							break;
						}
					}
					if (defending) {
						building.towerDefended(this);
						defending = false;
					}
					changeStateTo(SoldierMovable.ESoldierState.AGGRESSIVE);

				} else if (isEnemyAttackable(enemy, isInTower)) { // if enemy is close enough, attack it
					lookInDirection(EDirection.getApproxDirection(position, enemy.getPosition()));
					startAttackAnimation(enemy);
					changeStateTo(SoldierMovable.ESoldierState.HITTING);

				} else if (!isInTower) {
					changeStateTo(SoldierMovable.ESoldierState.SEARCH_FOR_ENEMIES);
					goToEnemy(enemy);

				} else {
					changeStateTo(SoldierMovable.ESoldierState.SEARCH_FOR_ENEMIES);

				}

				break;

			case INIT_GOTO_TOWER:
				changeStateTo(SoldierMovable.ESoldierState.GOING_TO_TOWER); // change state before requesting path because of checkPathStepPreconditions()
				if (!position.equals(building.getDoor()) && !goToPos(building.getDoor())) {
					notifyTowerThatRequestFailed();
				}
				break;

			case GOING_TO_TOWER:
				if (!building.isDestroyed() && building.getPlayer() == player) {
					OccupierPlace place = building.addSoldier(this);
					setVisible(false);
					setPosition(place.getPosition().calculatePoint(building.getPosition()));
					enableNothingToDoAction(false);

					if (isBowman()) {
						this.inTowerAttackPosition = building.getTowerBowmanSearchPosition(place);
						changeStateTo(SoldierMovable.ESoldierState.SEARCH_FOR_ENEMIES);
					} else {
						changeStateTo(SoldierMovable.ESoldierState.AGGRESSIVE);
					}

					isInTower = true;
				} else {
					playerControlled = true;
					changeStateTo(SoldierMovable.ESoldierState.AGGRESSIVE); // do a check of the surrounding to find possible enemies.
					building = null;
				}
				break;
		}
	}

	private void notifyTowerThatRequestFailed() {
		if (building.getPlayer() == player) { // only notify, if the tower still belongs to this player
			building.requestFailed(this);
			building = null;
			playerControlled = true;
			state = SoldierMovable.ESoldierState.AGGRESSIVE;
		}
	}

	protected ShortPoint2D getAttackPosition() {
		return isInTower && isBowman() ? inTowerAttackPosition : position;
	}

	private boolean isBowman() {
		return getMovableType().isBowman();
	}

	private void goToEnemy(IAttackable enemy) {
		if (inSaveGotoMode) {
			goToSavely(enemy);
		} else {
			EDirection dir = EDirection.getApproxDirection(position, enemy.getPosition());

			if (goInDirection(dir, EGoInDirectionMode.GO_IF_ALLOWED_AND_FREE)) {
				return;
			} else {
				inSaveGotoMode = true;
				goToSavely(enemy);
			}
		}
	}

	private void goToSavely(IAttackable enemy) {
		goToPos(enemy.getPosition());
	}

	private void changeStateTo(SoldierMovable.ESoldierState state) {
		this.state = state;
		switch (state) {
			case AGGRESSIVE:
				if (oldPathTarget != null) {
					goToPos(oldPathTarget);
					oldPathTarget = null;
				}
				break;

			default:
				break;
		}
	}

	protected void hitEnemy(IAttackable enemy) {
	}

	protected abstract void startAttackAnimation(IAttackable enemy);

	protected abstract boolean isEnemyAttackable(IAttackable enemy, boolean isInTower);

	@Override
	public boolean moveToTower(IOccupyableBuilding building) {
		if (state != SoldierMovable.ESoldierState.GOING_TO_TOWER && state != SoldierMovable.ESoldierState.INIT_GOTO_TOWER) {
			this.building = building;
			playerControlled = false;
			changeStateTo(SoldierMovable.ESoldierState.INIT_GOTO_TOWER);
			abortPath();
			this.oldPathTarget = null; // this prevents that the soldiers go to this position after he leaves the tower.
			return true;
		}

		return false;
	}

	public void leaveTower(ShortPoint2D newPosition) {
		if (isInTower) {
			setPosition(newPosition);
			enableNothingToDoAction(true);
			setVisible(true);
			setSelected(false);

			isInTower = false;
			building = null;
			defending = false;
			changeStateTo(SoldierMovable.ESoldierState.SEARCH_FOR_ENEMIES);

		} else if (state == SoldierMovable.ESoldierState.INIT_GOTO_TOWER || state == SoldierMovable.ESoldierState.GOING_TO_TOWER) {
			abortPath();
			building = null;
			changeStateTo(SoldierMovable.ESoldierState.SEARCH_FOR_ENEMIES);
		}
		playerControlled = true;
	}

	@Override
	public void informAboutAttackable(IAttackable other) {
		if (state == SoldierMovable.ESoldierState.AGGRESSIVE && (!isInTower || isBowman())) {
			changeStateTo(SoldierMovable.ESoldierState.SEARCH_FOR_ENEMIES); // this searches for the enemy on the next timer click
		}
	}

	public void defendTowerAt(ShortPoint2D pos) {
		setPosition(pos);
		changeStateTo(SoldierMovable.ESoldierState.SEARCH_FOR_ENEMIES);
		defending = true;
	}

	@Override
	protected boolean checkPathStepPreconditions(ShortPoint2D pathTarget, int step, EMoveToType moveToType) {
		if (state == SoldierMovable.ESoldierState.INIT_GOTO_TOWER) {
			return false; // abort previous path when we just got a tower set
		}

		boolean result = !((state == SoldierMovable.ESoldierState.SEARCH_FOR_ENEMIES || state == SoldierMovable.ESoldierState.HITTING) && step >= 2);
		if (!result && oldPathTarget == null) {
			oldPathTarget = pathTarget;
		}

		if (state == SoldierMovable.ESoldierState.GOING_TO_TOWER && (building == null || building.isDestroyed() || building.getPlayer() != player)) {
			result = false;
		}

		if (enemy != null && state == SoldierMovable.ESoldierState.SEARCH_FOR_ENEMIES && isEnemyAttackable(enemy, false)) {
			result = false;
		}

		return result;
	}

	@Override
	protected void moveToPathSet(ShortPoint2D oldPosition, ShortPoint2D oldTargetPos, ShortPoint2D targetPos, EMoveToType moveToType) {
		if (targetPos != null && this.oldPathTarget != null) {
			oldPathTarget = null; // reset the path target to be able to get the new one when we hijack the path
			inSaveGotoMode = false;
		}
		changeStateTo(moveToType.isAttackOnTheWay() ? SoldierMovable.ESoldierState.SEARCH_FOR_ENEMIES : SoldierMovable.ESoldierState.FORCED_MOVE);
	}

	@Override
	public Path findWayAroundObstacle(ShortPoint2D position, Path path) {
		if (state == SoldierMovable.ESoldierState.SEARCH_FOR_ENEMIES || state == SoldierMovable.ESoldierState.FORCED_MOVE) {
			EDirection direction = EDirection.getDirection(position, path.getNextPos());

			EDirection rightDir = direction.getNeighbor(-1);
			ShortPoint2D rightPos = rightDir.getNextHexPoint(position);
			EDirection leftDir = direction.getNeighbor(1);
			ShortPoint2D leftPos = leftDir.getNextHexPoint(position);

			ShortPoint2D freePosition = getRandomFreePosition(rightPos, leftPos);

			if (freePosition != null) {
				return new Path(freePosition);

			} else {
				EDirection twoRightDir = direction.getNeighbor(-2);
				ShortPoint2D twoRightPos = twoRightDir.getNextHexPoint(position);
				EDirection twoLeftDir = direction.getNeighbor(2);
				ShortPoint2D twoLeftPos = twoLeftDir.getNextHexPoint(position);

				freePosition = getRandomFreePosition(twoRightPos, twoLeftPos);

				if (freePosition != null) {
					return new Path(freePosition);
				} else {
					return path;
				}
			}
		} else {
			return super.findWayAroundObstacle(position, path);
		}
	}

	private ShortPoint2D getRandomFreePosition(ShortPoint2D pos1, ShortPoint2D pos2) {
		boolean pos1Free = grid.isFreePosition(pos1.x, pos1.y);
		boolean pos2Free = grid.isFreePosition(pos2.x, pos2.y);

		if (pos1Free && pos2Free) {
			return MatchConstants.random().nextBoolean() ? pos1 : pos2;
		} else if (pos1Free) {
			return pos1;
		} else if (pos2Free) {
			return pos2;
		} else {
			return null;
		}
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		if (building != null) {
			if (isInTower) {
				building.removeSoldier(this);
			} else {
				notifyTowerThatRequestFailed();
			}
		}
	}

	@Override
	protected void pathAborted(ShortPoint2D pathTarget) {
		switch (state) {
			case INIT_GOTO_TOWER:
			case GOING_TO_TOWER:
				notifyTowerThatRequestFailed();
				break;
			default:
				state = SoldierMovable.ESoldierState.AGGRESSIVE;
				break;
		}
	}

	protected float getCombatStrength() {
		boolean alliedGround = player.hasSameTeam(grid.getPlayerAt(getPosition()));

		float strengthMod = 1;
		if(alliedGround && hasEffect(EEffectType.DEFEATISM)) strengthMod *= EEffectType.DEFEATISM_DAMAGE_FACTOR;
		if(!alliedGround && hasEffect(EEffectType.INCREASED_MORALE)) strengthMod *= EEffectType.INCREASED_MORALE_DAMAGE_FACTOR;
		if(hasEffect(EEffectType.MOTIVATE_SWORDSMAN)) strengthMod *= EEffectType.MOTIVATE_SWORDSMAN_DAMAGE_FACTOR;

		return player.getCombatStrengthInformation().getCombatStrength(isOnOwnGround()) * strengthMod;
	}

	/**
	 * Internal state of the {@link SoldierMovable} class.
	 *
	 * @author Andreas Eberle
	 */
	public enum ESoldierState {
		AGGRESSIVE,

		SEARCH_FOR_ENEMIES,
		HITTING,

		INIT_GOTO_TOWER,
		GOING_TO_TOWER,
		FORCED_MOVE,
	}
}
