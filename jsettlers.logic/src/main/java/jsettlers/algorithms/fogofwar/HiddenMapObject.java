package jsettlers.algorithms.fogofwar;

import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.mapobject.IMapObject;
import jsettlers.logic.map.grid.objects.AbstractHexMapObject;

public class HiddenMapObject implements IMapObject {

	private final float progress;
	private final EMapObjectType type;

	private IMapObject nextObject;

	public HiddenMapObject(AbstractHexMapObject original) {
		this.progress = original.getStateProgress();
		this.type = original.getObjectType();
	}

	@Override
	public EMapObjectType getObjectType() {
		return type;
	}

	@Override
	public float getStateProgress() {
		return progress;
	}

	@Override
	public IMapObject getNextObject() {
		return nextObject;
	}

	public void setNextObject(IMapObject nextObject) {
		this.nextObject = nextObject;
	}

	@Override
	public IMapObject getMapObject(EMapObjectType type) {
		if(this.type.equals(type)) return this;

		return nextObject.getMapObject(type);
	}
}
