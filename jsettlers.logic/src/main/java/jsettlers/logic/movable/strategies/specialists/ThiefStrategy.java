/*******************************************************************************
 * Copyright (c) 2020
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
package jsettlers.logic.movable.strategies.specialists;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.MovableStrategy;
import jsettlers.logic.movable.specialist.ThiefMovable;

/**
 * 
 * @author Andreas Eberle
 * 
 */
public final class ThiefStrategy extends MovableStrategy<ThiefMovable> {
	private static final long serialVersionUID = -1359250497501671076L;

	private static final float ACTION1_DURATION = 1f;

	private EThiefState state = EThiefState.JOBLESS;

	private ShortPoint2D returnPos = null;
	private EMaterialType stolenMaterial = EMaterialType.NO_MATERIAL;

	public ThiefStrategy(ThiefMovable movable) {
		super(movable);
	}

	@Override
	protected void action() {
		if(isOnOwnGround() && stolenMaterial != EMaterialType.NO_MATERIAL) {
			getGrid().dropMaterial(movable.getPosition(), stolenMaterial, true, false);
			setMaterial(EMaterialType.NO_MATERIAL);
			stolenMaterial = EMaterialType.NO_MATERIAL;
		}

		ShortPoint2D pos = movable.getPosition();
		switch (state) {
			case JOBLESS:
				break;
			case GOING_TO_POS:
				if(stolenMaterial == EMaterialType.NO_MATERIAL) {
					if(canWorkOnPos(pos)) {
						playAction(EMovableAction.ACTION1, ACTION1_DURATION);
						state = EThiefState.PLAYING_ACTION1;
					} else {
						if(!findWorkablePosition()) {
							state = EThiefState.JOBLESS;
						}
					}
				}
				break;
			case PLAYING_ACTION1:
				if(canWorkOnPos(pos)) {
					executeAction(pos);
				}

				movable.moveTo(returnPos, EMoveToType.DEFAULT);
				state = EThiefState.JOBLESS;
				break;
		}
	}


	private boolean findWorkablePosition() {
		ShortPoint2D pos = movable.getPosition();
		if (preSearchPath(true, pos.x, pos.y, (short) 10, ESearchType.FOREIGN_MATERIAL)) {
			followPresearchedPath();
			return true;
		} else {
			return false;
		}
	}

	private boolean canWorkOnPos(ShortPoint2D pos) {
		return getGrid().fitsSearchType(movable, pos.x, pos.y, ESearchType.FOREIGN_MATERIAL);
	}

	private void executeAction(ShortPoint2D pos) {
		stolenMaterial = getGrid().takeMaterial(pos);
		setMaterial(stolenMaterial);
	}

	@Override
	protected void moveToPathSet(ShortPoint2D oldPosition, ShortPoint2D oldTargetPos, ShortPoint2D targetPos, EMoveToType moveToType) {
		state = moveToType.isWorkOnDestination()?EThiefState.GOING_TO_POS : EThiefState.JOBLESS;

		if(movable.getMaterial() == EMaterialType.NO_MATERIAL) {
			returnPos = movable.getPosition();
		} else {
			returnPos = null;
		}
	}

	@Override
	protected void stopOrStartWorking(boolean stop) {
		if(stop) {
			state = EThiefState.JOBLESS;
		} else {
			state = EThiefState.GOING_TO_POS;
		}
	}

	@Override
	protected void pathAborted(ShortPoint2D pathTarget) {
		state = EThiefState.JOBLESS;
	}

	@Override
	protected void strategyKilledEvent(ShortPoint2D pathTarget) {
		abortPath();
		if(stolenMaterial != EMaterialType.NO_MATERIAL) {
			drop(stolenMaterial);
		}
	}

	private enum EThiefState {
		JOBLESS,
		GOING_TO_POS,
		PLAYING_ACTION1,
		RAISING_UP,
	}
}
