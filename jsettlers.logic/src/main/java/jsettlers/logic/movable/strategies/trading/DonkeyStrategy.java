/*******************************************************************************
 * Copyright (c) 2016 - 2018
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

import java8.util.Optional;
import java8.util.stream.Stream;
import jsettlers.common.material.EMaterialType;
import jsettlers.logic.buildings.ITradeBuilding;
import jsettlers.logic.buildings.trading.MarketBuilding;
import jsettlers.logic.movable.cargo.DonkeyMovable;
import jsettlers.logic.buildings.ITradeBuilding.MaterialTypeWithCount;

/**
 *
 * @author Andreas Eberle
 *
 */
public class DonkeyStrategy extends TradingStrategy<DonkeyMovable> {
	private static final short WAYPOINT_SEARCH_RADIUS = 20;

	public DonkeyStrategy(DonkeyMovable movable) {
		super(movable);
	}

	protected boolean loadUp(ITradeBuilding tradeBuilding) {
		boolean loaded = false;
		for(int i = 0; i < DonkeyMovable.CARGO_COUNT; i++) {
			Optional<MaterialTypeWithCount> cargo = tradeBuilding.tryToTakeMaterial(1);

			if (!cargo.isPresent()) break;

			movable.setCargo(i, cargo.get().getMaterialType());
			loaded = true;
		}

		if(loaded) {
			setMaterial(EMaterialType.BASKET);
		}

		return loaded;
	}


	protected void dropMaterialIfPossible() {
		if(movable.getMaterial() == EMaterialType.NO_MATERIAL) return;

		for(int i = 0; i < DonkeyMovable.CARGO_COUNT; i++) {
			EMaterialType cargo = movable.getCargo(i);

			// all cargo is loadedd from zero to n, if this slot is empty all the following must be
			if(cargo == null) break;

			getGrid().dropMaterial(movable.getPosition(), cargo, true, true);
			movable.setCargo(i, null);
		}

		setMaterial(EMaterialType.NO_MATERIAL);
	}

	protected Stream<MarketBuilding> getTradersWithWork() {
		return MarketBuilding.getAllMarkets(movable.getPlayer());
	}

	@Override
	public boolean isAttackable() {
		return getState() == ETraderState.GOING_TO_TARGET;
	}

	@Override
	public boolean receiveHit() {
		if (getState() == ETraderState.GOING_TO_TARGET) {
			reset();
			abortPath();
		}
		return false;
	}

	@Override
	protected short getWaypointSearchRadius() {
		return WAYPOINT_SEARCH_RADIUS;
	}
}
