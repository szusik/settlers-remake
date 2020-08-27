/*******************************************************************************
 * Copyright (c) 2015 - 2018
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
package jsettlers.logic.movable;

import java.io.Serializable;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.civilian.BricklayerMovable;
import jsettlers.logic.movable.civilian.BuildingWorkerMovable;
import jsettlers.logic.movable.civilian.DiggerMovable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.strategies.BricklayerStrategy;
import jsettlers.logic.movable.strategies.BuildingWorkerStrategy;
import jsettlers.logic.movable.strategies.DiggerStrategy;
import jsettlers.logic.movable.strategies.DummyStrategy;

/**
 * Abstract super class of all movable strategies.
 *
 * @author Andreas Eberle
 */
public abstract class MovableStrategy<T extends Movable> implements Serializable {
	private static final long serialVersionUID = 3135655342562634378L;

	protected final T movable;

	protected MovableStrategy(T movable) {
		this.movable = movable;
	}

	public static MovableStrategy<?> getStrategy(Movable movable, EMovableType movableType) {
		switch (movableType) {
			case BEARER:
			case SWORDSMAN_L1:
			case SWORDSMAN_L2:
			case SWORDSMAN_L3:
			case PIKEMAN_L1:
			case PIKEMAN_L2:
			case PIKEMAN_L3:
			case BOWMAN_L1:
			case BOWMAN_L2:
			case BOWMAN_L3:
				return new DummyStrategy(movable);

			case BAKER:
			case CHARCOAL_BURNER:
			case FARMER:
			case FISHERMAN:
			case FORESTER:
			case MELTER:
			case MILLER:
			case MINER:
			case PIG_FARMER:
			case DONKEY_FARMER:
			case LUMBERJACK:
			case SAWMILLER:
			case SLAUGHTERER:
			case SMITH:
			case STONECUTTER:
			case WATERWORKER:
			case WINEGROWER:
			case HEALER:
			case DOCKWORKER:
				return new BuildingWorkerStrategy<>((BuildingWorkerMovable) movable);

			case DIGGER:
				return new DiggerStrategy((DiggerMovable) movable);

			case BRICKLAYER:
				return new BricklayerStrategy((BricklayerMovable) movable);

			case PIONEER:
			case GEOLOGIST:
			case THIEF:
			case MAGE:
			case DONKEY:
			case FERRY:
			case CARGO_SHIP:
				return new DummyStrategy(movable);

			default:
				assert false : "requested movableType: " + movableType + " but have no strategy for this type!";
				return null;
		}
	}

	protected void action() {
	}

	protected final EMaterialType setMaterial(EMaterialType materialType) {
		return movable.setMaterial(materialType);
	}

	protected final void playAction(EMovableAction movableAction, float duration) {
		movable.playAction(movableAction, duration);
	}

	protected final void lookInDirection(EDirection direction) {
		movable.lookInDirection(direction);
	}

	protected final boolean goToPos(ShortPoint2D targetPos) {
		return movable.goToPos(targetPos);
	}

	protected final AbstractMovableGrid getGrid() {
		return movable.grid;
	}

	/**
	 * Tries to go a step in the given direction.
	 *
	 * @param direction
	 * 		direction to go
	 * @param mode
	 * 		The mode used for this operation
	 * @return true if the step can and will immediately be executed. <br>
	 * false if the target position is generally blocked or a movable occupies that position.
	 */
	protected final boolean goInDirection(EDirection direction, EGoInDirectionMode mode) {
		return movable.goInDirection(direction, mode);
	}

	public final void setPosition(ShortPoint2D pos) {
		movable.setPosition(pos);
	}

	protected final void setVisible(boolean visible) {
		movable.setVisible(visible);
	}

	/**
	 * @param dijkstra
	 * 		if true, dijkstra algorithm is used<br>
	 * 		if false, in area finder is used.
	 * @param centerX
	 * @param centerY
	 * @param radius
	 * @param searchType
	 * @return true if a path has been found.
	 */
	protected final boolean preSearchPath(boolean dijkstra, short centerX, short centerY, short radius, ESearchType searchType) {
		return movable.preSearchPath(dijkstra, centerX, centerY, radius, searchType);
	}

	protected final ShortPoint2D followPresearchedPath() {
		return movable.followPresearchedPath();
	}

	protected final void enableNothingToDoAction(boolean enable) {
		movable.enableNothingToDoAction(enable);
	}

	protected final boolean fitsSearchType(ShortPoint2D pos, ESearchType searchType) {
		return movable.grid.fitsSearchType(movable, pos.x, pos.y, searchType);
	}

	protected final boolean fitsSearchType(int x, int y, ESearchType searchType) {
		return movable.grid.fitsSearchType(movable, x, y, searchType);
	}

	protected final boolean isValidPosition(ShortPoint2D position) {
		return movable.grid.isValidPosition(movable, position.x, position.y);
	}

	protected final boolean isValidPosition(int x, int y) {
		return movable.grid.isValidPosition(movable, x, y);
	}

	public final ShortPoint2D getPosition() {
		return movable.getPosition();
	}

	protected final void abortPath() {
		movable.abortPath();
	}

	/**
	 * Checks preconditions before the next path step can be gone.
	 *
	 * @param pathTarget
	 * 		Target of the current path.
	 * @param step
	 * 		The number of the current step where 1 means the first step.
	 * @param moveToType TODO
	 * @return true if the path should be continued<br>
	 * false if it must be stopped.
	 */
	protected boolean checkPathStepPreconditions(ShortPoint2D pathTarget, int step, EMoveToType moveToType) {
		return true;
	}

	/**
	 * This method is called when a movable is killed or converted to another strategy and can be used for finalization work in the strategy.
	 *
	 * @param pathTarget
	 * 		if the movable is currently walking on a path, this is the target of the path<br>
	 * 		else it is null.
	 */
	protected void strategyKilledEvent(ShortPoint2D pathTarget) { // used in overriding methods
	}

	/**
	 * @param oldPosition
	 * 		The position the movable was positioned before the new path has been calculated and the first step on the new path has been done.
	 * @param oldTargetPos
	 * 		The target position of the old path or null if no old path was set.
	 * @param targetPos
	 * 		The new target position.
	 * @param moveToType TODO
	 */
	protected void moveToPathSet(ShortPoint2D oldPosition, ShortPoint2D oldTargetPos, ShortPoint2D targetPos, EMoveToType moveToType) {
	}

	public void setPlayerControlled(boolean playerControlled) {
		movable.playerControlled = playerControlled;
	}

	protected void stopOrStartWorking(boolean stop) {
	}

	protected void sleep(short sleepTime) {
		movable.sleep(sleepTime);
	}

	protected void pathAborted(ShortPoint2D pathTarget) {
	}

	/**
	 * This method is called before a material is dropped during a {@link EMovableType}.DROP action.
	 *
	 * @return If true is returned, the dropped material is offered, if false, it isn't.
	 */
	protected boolean droppingMaterial() {
		return true;
	}

	protected boolean take(EMaterialType materialToTake, boolean takeFromMap) {
		return movable.take(materialToTake, takeFromMap);
	}

	protected void drop(EMaterialType materialToDrop) {
		movable.drop(materialToDrop);
	}

	protected boolean isOnOwnGround() {
		return movable.isOnOwnGround();
	}

	protected void tookMaterial() {
	}

	public EBuildingType getBuildingType() {
		return null;
	}
}
