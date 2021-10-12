package jsettlers.common.map.partition;

import jsettlers.common.movable.EMovableType;

public interface IProfessionSettings {

	ISingleProfessionLimit getSettings(EMovableType movableType);

	float getTargetMovableRatio(EMovableType movableType);

	int getTargetMovableCount(EMovableType movableType);

	float getCurrentMovableRatio(EMovableType movableType);

	int getCurrentMovableCount(EMovableType movableType);
}
