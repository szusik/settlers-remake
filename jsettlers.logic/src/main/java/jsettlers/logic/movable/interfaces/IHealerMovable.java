package jsettlers.logic.movable.interfaces;

public interface IHealerMovable extends ILogicMovable {

	boolean requestTreatment(IAttackableHumanMovable movable);

	IAttackableHumanMovable getPatient();
}
