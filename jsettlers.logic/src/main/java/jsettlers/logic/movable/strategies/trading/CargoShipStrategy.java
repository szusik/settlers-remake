/*******************************************************************************
 * Copyright (c) 2017 - 2018
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
package jsettlers.logic.movable.strategies.trading;

import java8.util.stream.Stream;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.utils.mutables.MutableBoolean;
import jsettlers.logic.buildings.ITradeBuilding;
import jsettlers.logic.buildings.trading.HarborBuilding;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.movable.cargo.CargoShipMovable;

/**
 *
 * @author Rudolf Polzer
 *
 */
public class CargoShipStrategy extends TradingStrategy<CargoShipMovable> {
	private static final short WAYPOINT_SEARCH_RADIUS = 50;

	public CargoShipStrategy(CargoShipMovable movable) {
		super(movable);
	}

	@Override
	protected boolean loadUp(ITradeBuilding tradeBuilding) {
		MutableBoolean loaded = new MutableBoolean(false);

		for (int stackIndex = 0; stackIndex < CargoShipMovable.CARGO_STACKS; stackIndex++) {
			if (movable.getCargoCount(stackIndex) > 0) {
				continue;
			}

			final int finalStackIndex = stackIndex;

			tradeBuilding.tryToTakeMaterial(Constants.STACK_SIZE).ifPresent(materialTypeWithCount -> {
				movable.setCargoType(materialTypeWithCount.materialType, finalStackIndex);
				movable.setCargoCount(materialTypeWithCount.count, finalStackIndex);

				loaded.value = true;
			});
		}

		return loaded.value;
	}

	protected void dropMaterialIfPossible() {
		for (int stack = 0; stack < CargoShipMovable.CARGO_STACKS; stack++) {
			int cargoCount = movable.getCargoCount(stack);
			EMaterialType material = movable.getCargoType(stack);
			while (cargoCount > 0) {
				super.getGrid().dropMaterial(movable.getPosition(), material, true, true);
				cargoCount--;
			}
			movable.setCargoCount(0, stack);
		}
	}

	protected Stream<? extends ITradeBuilding> getTradersWithWork() {
		return HarborBuilding.getAllHarbors(movable.getPlayer());
	}

	@Override
	protected short getWaypointSearchRadius() {
		return WAYPOINT_SEARCH_RADIUS;
	}
}
