package jsettlers.algorithms.fogofwar;

import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.mapobject.IStackMapObject;
import jsettlers.logic.map.grid.objects.AbstractHexMapObject;

public class HiddenMapObjectCreator {
	public static HiddenMapObject create(AbstractHexMapObject object) {
		EMapObjectType type = object.getObjectType();
		if(!type.persistent) return null;

		switch (type) {
			case ARROW: // TODO
			case BUILDING: // TODO
			case PLACEMENT_BUILDING:
			case SMOKE:
			case PIG:
			case DONKEY:
			case GHOST:
			case BUILDING_DECONSTRUCTION_SMOKE:
			case ATTACKABLE_TOWER:
			case INFORMABLE_MAP_OBJECT:
			case EYE:
			case SPELL_EFFECT:
				return null;
			case STACK_OBJECT:
				return new HiddenMapObject.HiddenStackMapObject(object);
		}

		return new HiddenMapObject(object);
	}
}
