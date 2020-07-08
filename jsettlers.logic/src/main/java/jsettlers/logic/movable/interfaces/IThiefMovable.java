package jsettlers.logic.movable.interfaces;

import jsettlers.logic.movable.interfaces.ILogicMovable;

public interface IThiefMovable extends ILogicMovable {

	void uncoveredBy(byte teamId);
}
