package jsettlers.logic.movable.civilian;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.jobs.EBuildingJobType;
import jsettlers.common.buildings.jobs.IBuildingJob;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.menu.messages.SimpleMessage;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.map.grid.partition.manager.manageables.interfaces.IWorkerRequestBuilding;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class LegacyBuildingWorkerMovable extends BuildingWorkerMovable {

	private transient IBuildingJob currentJob = null;

	private EMaterialType poppedMaterial;

	public LegacyBuildingWorkerMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable replace) {
		super(grid, movableType, position, player, replace, tree);
	}

	private static final Root<LegacyBuildingWorkerMovable> tree = new Root<>(createBuildingWorkerBehaviour());

	private static Node<LegacyBuildingWorkerMovable> createBuildingWorkerBehaviour() {
		return defaultFramework(
				guard(mov -> mov.currentJob != null,
					selector(
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.GO_TO),
							nodeToJob(goToPos(LegacyBuildingWorkerMovable::getCurrentJobPos, LegacyBuildingWorkerMovable::pathStep))
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.TRY_TAKING_RESOURCE),
							nodeToJob(condition(LegacyBuildingWorkerMovable::tryTakingResource))
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.TRY_TAKING_FOOD),
							nodeToJob(condition(mov -> mov.building.tryTakingFood(mov.currentJob.getFoodOrder())))
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.WAIT),
							BehaviorTreeHelper.sleep(mov -> (int)(mov.currentJob.getTime()*1000)),
							jobFinishedNode()
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.WALK),
							nodeToJob(goInDirectionWaitFree(mov -> mov.currentJob.getDirection(), LegacyBuildingWorkerMovable::pathStep))
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.SHOW),
							waitFor(isAllowedToWork()),
							action(mov -> {
								ShortPoint2D pos = mov.getCurrentJobPos();
								if (mov.currentJob.getDirection() != null) {
									mov.lookInDirection(mov.currentJob.getDirection());
								}
								mov.setPosition(pos);
							}),
							show(),
							jobFinishedNode()
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.HIDE),
							hide(),
							jobFinishedNode()
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.SET_MATERIAL),
							action(mov -> {mov.setMaterial(mov.currentJob.getMaterial());}),
							jobFinishedNode()
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.TAKE),
							nodeToJob(take(mov -> mov.currentJob.getMaterial(), mov -> mov.currentJob.isTakeMaterialFromMap(), mov -> {}))
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.DROP),
							nodeToJob(dropProduced(mov -> mov.currentJob.getMaterial()))
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.DROP_POPPED),
							nodeToJob(dropProduced(mov -> mov.poppedMaterial))
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.LOOK_AT),
							action(mov -> {mov.setDirection(mov.currentJob.getDirection());}),
							jobFinishedNode()
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.PLAY_ACTION1),
							playAction(EMovableAction.ACTION1, mov -> (short)(mov.currentJob.getTime()*1000)),
							jobFinishedNode()
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.PLAY_ACTION2),
							playAction(EMovableAction.ACTION2, mov -> (short)(mov.currentJob.getTime()*1000)),
							jobFinishedNode()
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.PLAY_ACTION3),
							playAction(EMovableAction.ACTION3, mov -> (short)(mov.currentJob.getTime()*1000)),
							jobFinishedNode()
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.AVAILABLE),
							nodeToJob(condition(mov -> mov.grid.canTakeMaterial(mov.getCurrentJobPos(), mov.currentJob.getMaterial())))
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.NOT_FULL),
							nodeToJob(condition(mov -> mov.grid.canPushMaterial(mov.getCurrentJobPos())))
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.POP_TOOL),
							nodeToJob(condition(LegacyBuildingWorkerMovable::popToolRequestAction))
						),
						sequence(
							condition(mov -> mov.currentJob.getType() == EBuildingJobType.POP_WEAPON),
							nodeToJob(
								condition(mov -> {
									mov.poppedMaterial = mov.building.getMaterialProduction().getWeaponToProduce();
									return mov.poppedMaterial != null;
								})
							)
						),
						// unknown job type
						action(LegacyBuildingWorkerMovable::abortJob)
					)
				)
		);
	}

	private static Node<LegacyBuildingWorkerMovable> jobFailedNode() {
		return action(LegacyBuildingWorkerMovable::jobFailed);
	}

	private static Node<LegacyBuildingWorkerMovable> jobFinishedNode() {
		return action(LegacyBuildingWorkerMovable::jobFinished);
	}

	private static Node<LegacyBuildingWorkerMovable> nodeToJob(Node<LegacyBuildingWorkerMovable> child) {
		return selector(
				sequence(
					child,
					jobFinishedNode()
				),
				jobFailedNode()
		);
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		String currentJobName = ois.readUTF();
		if (currentJobName.equals("null")) {
			currentJob = null;
		} else {
			EBuildingType building = (EBuildingType) ois.readObject();
			ECivilisation civilisation = (ECivilisation) ois.readObject();
			currentJob = building.getVariant(civilisation).getJobByName(currentJobName);
		}
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		if (currentJob != null) {
			oos.writeUTF(currentJob.getName());
			oos.writeObject(building.getBuildingVariant().getType());
			oos.writeObject(building.getPlayer().getCivilisation());
		} else {
			oos.writeUTF("null");
		}
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();
	}

	private boolean isJobless() {
		return currentJob == null;
	}

	private boolean popToolRequestAction() {
		poppedMaterial = building.getMaterialProduction().drawRandomAbsolutelyRequestedTool(); // first priority: Absolutely set tool production requests of user
		if (poppedMaterial == null) {
			poppedMaterial = grid.popToolProductionRequest(building.getDoor()); // second priority: Tools needed by settlers (automated production)
		}
		if (poppedMaterial == null) {
			poppedMaterial = building.getMaterialProduction().drawRandomRelativelyRequestedTool(); // third priority: Relatively set tool production requests of user
		}

		return poppedMaterial != null;
	}

	/**
	 * @param dijkstra
	 * 		if true, dijkstra algorithm is used<br>
	 * 		if false, in area finder is used.
	 */
	private boolean preSearchPathAction(boolean dijkstra) {
		super.setPosition(getCurrentJobPos());

		ShortPoint2D workAreaCenter = building.getWorkAreaCenter();

		boolean pathFound = super.preSearchPath(dijkstra, workAreaCenter.x, workAreaCenter.y, building.getBuildingVariant().getWorkRadius(),
				currentJob.getSearchType());

		if (pathFound) {
			searchFailedCtr = 0;
			this.building.setCannotWork(false);
			return true;
		} else {
			searchFailedCtr++;

			if (searchFailedCtr > 10) {
				this.building.setCannotWork(true);
				player.showMessage(SimpleMessage.cannotFindWork(building));
			}
			return false;
		}
	}

	private void jobFinished() {
		this.currentJob = this.currentJob.getNextSucessJob();
	}

	private void jobFailed() {
		this.currentJob = this.currentJob.getNextFailJob();
	}

	private ShortPoint2D getCurrentJobPos() {
		return currentJob.calculatePoint(building);
	}

	private boolean lookAtSearched() {
		EDirection direction = grid.getDirectionOfSearched(position, currentJob.getSearchType());
		if (direction != null) {
			setDirection(direction);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void setWorkerJob(IWorkerRequestBuilding building) {
		super.setWorkerJob(building);

		this.currentJob = building.getBuildingVariant().getStartJob();
	}

	@Override
	protected void abortJob() {
		if(building != null) {
			currentJob = null;
		}

		super.abortJob();
	}

	@Override
	public void buildingDestroyed() {
		super.buildingDestroyed();

		currentJob = null;
	}

	private boolean pathStep() {
		return isJobless() || building != null; // TODO
	}
}
