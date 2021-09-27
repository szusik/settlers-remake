package jsettlers.logic.map.grid.objects;

import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.objects.ProgressingObject;

public class SmokeMapObject extends ProgressingObject {

	public SmokeMapObject(ShortPoint2D pos, short duration) {
		super(pos);
		setDuration(duration/1000f);
	}

	@Override
	public EMapObjectType getObjectType() {
		return EMapObjectType.SMOKE;
	}

	@Override
	protected void changeState() {
	}

	@Override
	public boolean cutOff() {
		return false;
	}

	@Override
	public boolean canBeCut() {
		return false;
	}
}
