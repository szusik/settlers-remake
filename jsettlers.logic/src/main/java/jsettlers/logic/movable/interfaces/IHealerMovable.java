package jsettlers.logic.movable.interfaces;

public interface IHealerMovable extends IBuildingWorkerMovable {

	boolean requestTreatment(IAttackableHumanMovable movable);

	IAttackableHumanMovable getPatient();
}
