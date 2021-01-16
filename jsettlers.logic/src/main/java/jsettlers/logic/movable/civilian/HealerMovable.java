package jsettlers.logic.movable.civilian;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IAttackableHumanMovable;
import jsettlers.logic.movable.interfaces.IHealerMovable;
import jsettlers.logic.player.Player;

public class HealerMovable extends LegacyBuildingWorkerMovable implements IHealerMovable {

	private IAttackableHumanMovable patient = null;

	public HealerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.HEALER, position, player, replace);
	}

	@Override
	public IAttackableHumanMovable getPatient() {
		return patient;
	}

	@Override
	public boolean requestTreatment(IAttackableHumanMovable movable) {
		ShortPoint2D healSpot = new ShortPoint2D(position.x+2, position.y+2);
		if(patient != null &&
				patient.getPath() != null &&
				patient.getPath().getTargetPosition().equals(healSpot)) return false;

		patient = movable;
		return true;
	}
}
