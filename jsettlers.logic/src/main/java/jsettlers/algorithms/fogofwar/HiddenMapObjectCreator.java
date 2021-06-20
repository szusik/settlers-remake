package jsettlers.algorithms.fogofwar;

import jsettlers.logic.map.grid.objects.AbstractHexMapObject;

class HiddenMapObjectCreator {
	public static HiddenMapObject create(AbstractHexMapObject object) {
		return new HiddenMapObject(object);
	}
}
