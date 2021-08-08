/*******************************************************************************
 * Copyright (c) 2015 - 2017
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
package jsettlers.common.buildings.jobs;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.material.ESearchType;

/**
 * The jobs types that can be done by building workers.
 * @see IBuildingJob
 */
public enum EBuildingJobType {
	/**
	 * Waits a given time.
	 * <p>
	 * Parameter: time (in seconds)
	 * <p>
	 * Success: The time elapsed.
	 * <p>
	 * Fail: impossible.
	 * 
	 * @see BuildingJob#getTime();
	 */
	WAIT,

	/**
	 * Lets the settler walk in a given direction. The settler may wait. The settler may walk on blocked tiles with this command.
	 * <p>
	 * Parameter: direction
	 * <p>
	 * Success: The settler is at the position
	 * <p>
	 * Fail: Should not happen normally.
	 */
	WALK,

	/**
	 * Shows the settler at a given position. The settler just appears there. The position may be blocked.
	 * <p>
	 * Parameter: dx, dy
	 * <p>
	 * Success: The settler appeared.
	 * <p>
	 * Fail: The settler could not appear at the given position.
	 */
	SHOW,

	/**
	 * Lets the settler disappear.
	 * <p>
	 * Parameter: none
	 * <p>
	 * Success: The settler disappeared instantly.
	 * <p>
	 * Fail: impossible
	 */
	HIDE,

	/**
	 * Sets the material property of the settler.
	 * <p>
	 * Parameter: material
	 * <p>
	 * Success: always
	 * <p>
	 * Fail: never
	 */
	SET_MATERIAL,

	/**
	 * Picks up the specified material. Does not change the material property of the settler
	 * <p>
	 * Parameter: material
	 * <p>
	 * Success: There was a material at that position, one item was removed.
	 * <p>
	 * Fail: There was no given material at that position.
	 */
	TAKE,

	/**
	 * Lets the settler drop the given material to the stack at the position.
	 * <p>
	 * The given material that is dropped is independent from the material the settler is having, and the material property is not changed by this call.
	 * <p>
	 * Parameter: material
	 * <p>
	 * Success: When the settler dropped the material.
	 * <p>
	 * Fail: If the drop is impossible, e.g. because there is already material at that position.
	 */
	DROP,

	/**
	 * Goes to the position relative to the building.
	 * <p>
	 * Success: The settler is at the position
	 * <p>
	 * Fail: The position is unreachable.
	 */
	GO_TO,

	/**
	 * Look at
	 * <p>
	 * Parameter: direction
	 * <p>
	 * Success: The settler looks at the given new direction.
	 * <p>
	 * Fail: impossible
	 */
	LOOK_AT,

	/**
	 * Plays an action animation.
	 * <p>
	 * Parameter: time (the time the action should take)
	 * <p>
	 * Success: The animation was played.
	 * <p>
	 * Fail: should not happen.
	 */
	PLAY_ACTION1,

	/**
	 * @see EBuildingJobType#PLAY_ACTION1
	 */
	PLAY_ACTION2,

	/**
	 * @see EBuildingJobType#PLAY_ACTION1
	 */
	PLAY_ACTION3,

	/**
	 * Tests whether there is a material at the given position.
	 * <p>
	 * Parameters: dx, dy, material
	 * <p>
	 * Success: There is material at that position.
	 * <p>
	 * Fail: There is no matching material at that position
	 */
	AVAILABLE,

	/**
	 * Tests if the stack at the position is full
	 * <p>
	 * Parameters: dx, dy, material
	 * <p>
	 * Success: The material may be placed at the given position
	 * <p>
	 * Fail: There is a full stack at that position, a wrong stack or it is blocked otherwise.
	 */
	NOT_FULL,

	/**
	 * If the settler should be productive, this method succeeds, it fails otherwise.
	 */
	TRY_TAKING_RESOURCE,

	/**
	 * Used for mines to check if they have food to use. Supplies parameter foodOrder.
	 */
	TRY_TAKING_FOOD,

	/**
	 * Pops a tool from the list of tools that should be produced.
	 * <p>
	 * fails if there is noting to do.
	 */
	POP_TOOL,

	/**
	 * Pops a weapon from the list of tools that should be produced.
	 * <p>
	 * fails if there is noting to do.
	 */
	POP_WEAPON,

	/**
	 * Drops a tool/weapon that was requested with {@link #POP_TOOL} or {@link #POP_WEAPON}
	 */
	DROP_POPPED,
}
