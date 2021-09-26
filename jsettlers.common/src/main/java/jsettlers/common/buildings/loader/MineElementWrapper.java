package jsettlers.common.buildings.loader;

import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import org.xml.sax.Attributes;

public class MineElementWrapper {

	private final EDirection dropDirection;
	private final EMaterialType[] foodOrder;
	private final float miningInterval;

	private static final String ATTR_DROP_DIRECTION = "dropDirection";
	private static final String ATTR_FOOD_ORDER = "foodOrder";
	private static final String ATTR_MINING_INTERVAL = "miningInterval";

	public MineElementWrapper() {
		dropDirection = EDirection.SOUTH_WEST;
		foodOrder = new EMaterialType[0];
		miningInterval = 1;
	}

	public MineElementWrapper(Attributes attributes) {
		dropDirection = EDirection.valueOf(attributes.getValue(ATTR_DROP_DIRECTION));
		foodOrder = JobElementWrapper.getMaterialTypeArray(attributes, ATTR_FOOD_ORDER);
		miningInterval = JobElementWrapper.getAttributeAsFloat(attributes, ATTR_MINING_INTERVAL);
	}

	public EDirection getDropDirection() {
		return dropDirection;
	}

	public EMaterialType[] getFoodOrder() {
		return foodOrder;
	}

	public float getMiningInterval() {
		return miningInterval;
	}
}
