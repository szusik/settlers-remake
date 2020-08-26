package jsettlers.logic.movable.cargo;

import java8.util.Optional;
import java8.util.stream.Stream;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.menu.messages.SimpleMessage;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.ITradeBuilding;
import jsettlers.logic.buildings.trading.MarketBuilding;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

public class DonkeyMovable extends CargoMovable {

	private static final short WAYPOINT_SEARCH_RADIUS = 20;
	public static final int CARGO_COUNT = 2;

	private final EMaterialType[] cargo = new EMaterialType[CARGO_COUNT];

	public DonkeyMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable) {
		super(grid, movableType, position, player, movable, WAYPOINT_SEARCH_RADIUS, MarketBuilding::getAllMarkets);
	}

	public void setCargo(int index, EMaterialType material) {
		cargo[index] = material;
	}

	public EMaterialType getCargo(int index) {
		return cargo[index];
	}

	@Override
	protected void action() {
		super.action();

		attackable = getState() == CargoMovable.ETraderState.GOING_TO_TARGET;
	}


	protected boolean loadUp(ITradeBuilding tradeBuilding) {
		boolean loaded = false;
		for(int i = 0; i < DonkeyMovable.CARGO_COUNT; i++) {
			Optional<ITradeBuilding.MaterialTypeWithCount> cargo = tradeBuilding.tryToTakeMaterial(1);

			if (!cargo.isPresent()) break;

			setCargo(i, cargo.get().getMaterialType());
			loaded = true;
		}

		if(loaded) {
			setMaterial(EMaterialType.BASKET);
		}

		return loaded;
	}


	protected void dropMaterialIfPossible() {
		if(getMaterial() == EMaterialType.NO_MATERIAL) return;

		for(int i = 0; i < DonkeyMovable.CARGO_COUNT; i++) {
			EMaterialType cargo = getCargo(i);

			// all cargo is loaded from zero to n, if this slot is empty all the following must be
			if(cargo == null) break;

			grid.dropMaterial(position, cargo, true, true);
			setCargo(i, null);
		}

		setMaterial(EMaterialType.NO_MATERIAL);
	}

	@Override
	public void receiveHit(float hitStrength, ShortPoint2D attackerPos, byte attackingPlayer) {
		if(getState() == CargoMovable.ETraderState.GOING_TO_TARGET) {
			reset();
			abortPath();
		}

		player.showMessage(SimpleMessage.donkeyAttacked(attackingPlayer, attackerPos));
	}
}
