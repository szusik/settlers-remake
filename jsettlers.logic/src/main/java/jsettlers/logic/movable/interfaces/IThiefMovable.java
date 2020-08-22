package jsettlers.logic.movable.interfaces;

import jsettlers.common.movable.IGraphicsThief;

public interface IThiefMovable extends IAttackableHumanMovable, IGraphicsThief {

	void uncoveredBy(byte teamId);
}
