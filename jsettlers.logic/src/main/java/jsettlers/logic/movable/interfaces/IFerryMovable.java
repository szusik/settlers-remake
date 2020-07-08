package jsettlers.logic.movable.interfaces;

public interface IFerryMovable extends ILogicMovable {

	void unloadFerry();

	boolean addPassenger(IAttackableHumanMovable movable);
}
