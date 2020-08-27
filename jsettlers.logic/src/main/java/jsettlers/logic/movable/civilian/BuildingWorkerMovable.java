package jsettlers.logic.movable.civilian;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.jobs.EBuildingJobType;
import jsettlers.common.buildings.jobs.IBuildingJob;
import jsettlers.common.landscape.EResourceType;
import jsettlers.common.map.shapes.MapCircle;
import jsettlers.common.map.shapes.MapCircleIterator;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.EPriority;
import jsettlers.common.menu.messages.SimpleMessage;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.workers.DockyardBuilding;
import jsettlers.logic.buildings.workers.MillBuilding;
import jsettlers.logic.buildings.workers.SlaughterhouseBuilding;
import jsettlers.logic.map.grid.partition.manager.manageables.IManageableWorker;
import jsettlers.logic.map.grid.partition.manager.manageables.interfaces.IWorkerRequestBuilding;
import jsettlers.logic.movable.EGoInDirectionMode;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IAttackableHumanMovable;
import jsettlers.logic.movable.interfaces.IBuildingWorkerMovable;
import jsettlers.logic.movable.interfaces.IHealerMovable;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.player.Player;

public class BuildingWorkerMovable extends CivilianMovable implements IBuildingWorkerMovable, IManageableWorker {

	private transient IBuildingJob currentJob = null;
	protected IWorkerRequestBuilding building;

	private boolean done;

	private EMaterialType poppedMaterial;
	private int searchFailedCtr = 0;

	private ShortPoint2D markedPosition;
	private IAttackableHumanMovable nextPatient = null;

	public BuildingWorkerMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable replace) {
		super(grid, movableType, position, player, replace);
	}

	@Override
	public EBuildingType getGarrisonedBuildingType() {
		if(building != null) {
			return building.getBuildingVariant().getType();
		} else {
			return null;
		}
	}

	@Override
	protected void strategyStarted() {
		reportAsJobless();
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		String currentJobName = ois.readUTF();
		if (currentJobName.equals("null")) {
			currentJob = null;
		} else {
			currentJob = building.getBuildingVariant().getJobByName(currentJobName);
		}
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		if (currentJob != null) {
			oos.writeUTF(currentJob.getName());
		} else {
			oos.writeUTF("null");
		}
	}

	@Override
	protected void peacetimeAction() {
		if (isJobless()) {
			return;
		}

		if (building.isDestroyed()) { // check if building is still ok
			buildingDestroyed();
			return;
		}

		switch (currentJob.getType()) {
			case GO_TO:
				gotoAction();
				break;

			case TRY_TAKING_RESOURCE:
				clearMark();
				if (tryTakingResource()) {
					jobFinished();
				} else {
					jobFailed();
				}
				break;

			case TRY_TAKING_FOOD:
				if (building.tryTakingFood(currentJob.getFoodOrder())) {
					jobFinished();
				} else {
					jobFailed();
				}
				break;

			case WAIT: {
				short waitTime = (short) (currentJob.getTime() * 1000);
				super.sleep(waitTime);
				jobFinished();
				break;
			}

			case WALK:
				IBuildingJob job = currentJob;
				super.goInDirection(currentJob.getDirection(), EGoInDirectionMode.GO_IF_ALLOWED_WAIT_TILL_FREE);
				if (currentJob == job) { // the path could fail and call abortPath().
					jobFinished();
				}
				break;

			case SHOW: {
				if (building.getPriority() == EPriority.STOPPED) {
					break;
				}

				ShortPoint2D pos = getCurrentJobPos();
				if (currentJob.getDirection() != null) {
					super.lookInDirection(currentJob.getDirection());
				}
				super.setPosition(pos);
				super.setVisible(true);
				jobFinished();
				break;
			}

			case HIDE:
				super.setVisible(false);
				jobFinished();
				break;

			case SET_MATERIAL:
				super.setMaterial(currentJob.getMaterial());
				jobFinished();
				break;

			case TAKE:
				takeAction();
				break;

			case DROP:
				dropAction(currentJob.getMaterial());
				break;
			case DROP_POPPED:
				dropAction(poppedMaterial);
				break;

			case PRE_SEARCH:
				preSearchPathAction(true);
				break;

			case PRE_SEARCH_IN_AREA:
				preSearchPathAction(false);
				break;

			case FOLLOW_SEARCHED:
				followPreSearchedAction();
				break;

			case LOOK_AT_SEARCHED:
				lookAtSearched();
				break;

			case GO_TO_DOCK:
				gotoDockAction();
				break;

			case BUILD_SHIP:
				if (building instanceof DockyardBuilding) {
					((DockyardBuilding) building).buildShipAction();
				}
				jobFinished();
				break;

			case LOOK_AT:
				super.lookInDirection(currentJob.getDirection());
				jobFinished();
				break;

			case EXECUTE:
				executeAction();
				break;

			case PLAY_ACTION1:
				super.playAction(EMovableAction.ACTION1, currentJob.getTime());
				jobFinished();
				break;
			case PLAY_ACTION2:
				super.playAction(EMovableAction.ACTION2, currentJob.getTime());
				jobFinished();
				break;
			case PLAY_ACTION3:
				super.playAction(EMovableAction.ACTION3, currentJob.getTime());
				jobFinished();
				break;

			case AVAILABLE:
				if (grid.canTakeMaterial(getCurrentJobPos(), currentJob.getMaterial())) {
					jobFinished();
				} else {
					jobFailed();
				}
				break;

			case NOT_FULL:
				if (grid.canPushMaterial(getCurrentJobPos())) {
					jobFinished();
				} else {
					jobFailed();
				}
				break;

			case SMOKE_ON:
			case SMOKE_OFF: {
				grid.placeSmoke(getCurrentJobPos(), currentJob.getType() == EBuildingJobType.SMOKE_ON);
				building.addMapObjectCleanupPosition(getCurrentJobPos(), EMapObjectType.SMOKE);
				jobFinished();
				break;
			}

			case START_WORKING:
			case STOP_WORKING:
				if (building instanceof SlaughterhouseBuilding) {
					((SlaughterhouseBuilding) building).requestSound();
				}
				if (building instanceof MillBuilding) {
					((MillBuilding) building).setRotating(currentJob.getType() == EBuildingJobType.START_WORKING);
				}
				jobFinished();
				break;

			case PIG_IS_ADULT:
				if (grid.isPigAdult(getCurrentJobPos())) {
					jobFinished();
				} else {
					jobFailed();
				}
				break;

			case PIG_IS_THERE:
				if (grid.hasPigAt(getCurrentJobPos())) {
					jobFinished();
				} else {
					jobFailed();
				}
				break;

			case PIG_PLACE:
			case PIG_REMOVE:
				placeOrRemovePigAction();
				break;

			case POP_TOOL:
				popToolRequestAction();
				break;

			case POP_WEAPON:
				popWeaponRequestAction();
				break;

			case GROW_DONKEY:
				growDonkeyAction();
				break;

			case CAN_HEAL: {
				ShortPoint2D jobPos = getCurrentJobPos();

				ILogicMovable movable = grid.getMovableAt(jobPos.x, jobPos.y);
				if(movable instanceof IAttackableHumanMovable && ((IAttackableHumanMovable)movable).needsTreatment()) {
					nextPatient = (IAttackableHumanMovable) movable;
					jobFinished();
				} else {
					nextPatient = null;
					if(movable != null) movable.push(this);
					jobFailed();
				}
				break;
			}

			case CALL_WOUNDED: {
				// check if patient is still interested
				IAttackableHumanMovable patient = ((IHealerMovable)this).getPatient();
				if(patient != null) {
					int healX = position.x + 2;
					int healY = position.y + 2;

					if(patient.getPath() == null ||
							!patient.getPath().getTargetPosition().equals(healX, healY)) {
						// reset patient
						((IHealerMovable)this).requestTreatment(null);
					}
				}

				patient = ((IHealerMovable)this).getPatient();
				if(patient == null) {
					IAttackableHumanMovable bestPatient = null;
					float patientHealth = Float.MAX_VALUE;
					MapCircleIterator iter = new MapCircleIterator(new MapCircle(building.getWorkAreaCenter(), building.getBuildingVariant().getWorkRadius()));

					int width = grid.getWidth();
					int height = grid.getHeight();
					while(iter.hasNext()) {
						ShortPoint2D next = iter.next();
						if(next.x > 0 && next.x < width && next.y > 0 && next.y < height) {
							ILogicMovable potentialPatient = grid.getMovableAt(next.x, next.y);
							if (potentialPatient instanceof IAttackableHumanMovable &&
									potentialPatient.getPlayer() == player &&
									((IAttackableHumanMovable)potentialPatient).needsTreatment()) {
								float newHealth = potentialPatient.getHealth();
								if (newHealth < patientHealth) {
									bestPatient = (IAttackableHumanMovable) potentialPatient;
									patientHealth = newHealth;
								}
							}
						}
					}

					if(bestPatient != null && bestPatient.pingWounded((IHealerMovable) this)) {
						jobFinished();
					} else {
						jobFailed();
					}
				} else {
					jobFinished();
				}
				break;
			}

			case HEAL:
				if(nextPatient != null) {
					nextPatient.heal();
					((IHealerMovable)this).requestTreatment(null);
					jobFinished();
				} else {
					jobFailed();
				}
				break;
		}
	}

	private boolean isJobless() {
		return currentJob == null;
	}

	private void gotoDockAction() {
		DockyardBuilding dockyard = (DockyardBuilding) building;
		if (!done) {
			this.done = true;
			ShortPoint2D dockEndPosition = dockyard.getDock().getEndPosition();
			if (!super.goToPos(dockEndPosition)) {
				jobFailed();
			}
		} else {
			jobFinished(); // start next action
		}
	}

	private void followPreSearchedAction() {
		ShortPoint2D pathTargetPos = super.followPresearchedPath();
		mark(pathTargetPos);
		jobFinished();
	}

	private void placeOrRemovePigAction() {
		ShortPoint2D pos = getCurrentJobPos();
		grid.placePigAt(pos, currentJob.getType() == EBuildingJobType.PIG_PLACE);
		building.addMapObjectCleanupPosition(pos, EMapObjectType.PIG);
		jobFinished();
	}

	private void growDonkeyAction() {
		ShortPoint2D pos = getCurrentJobPos();
		if (grid.feedDonkeyAt(pos)) {
			building.addMapObjectCleanupPosition(pos, EMapObjectType.DONKEY);
			jobFinished();
		} else {
			jobFailed();
		}
	}

	private void popWeaponRequestAction() {
		poppedMaterial = building.getMaterialProduction().getWeaponToProduce();

		if (poppedMaterial != null) {
			jobFinished();
		} else {
			jobFailed();
		}
	}

	private void popToolRequestAction() {
		ShortPoint2D pos = building.getDoor();

		poppedMaterial = building.getMaterialProduction().drawRandomAbsolutelyRequestedTool(); // first priority: Absolutely set tool production requests of user
		if (poppedMaterial == null) {
			poppedMaterial = grid.popToolProductionRequest(pos); // second priority: Tools needed by settlers (automated production)
		}
		if (poppedMaterial == null) {
			poppedMaterial = building.getMaterialProduction().drawRandomRelativelyRequestedTool(); // third priority: Relatively set tool production requests of user
		}

		if (poppedMaterial != null) {
			jobFinished();
		} else {
			jobFailed();
		}
	}

	private void executeAction() {
		clearMark();
		if (grid.executeSearchType(this, position, currentJob.getSearchType())) {
			jobFinished();
		} else {
			jobFailed();
		}
	}

	private void takeAction() {
		if (super.take(currentJob.getMaterial(), currentJob.isTakeMaterialFromMap())) {
			jobFinished();
		} else {
			jobFailed();
		}
	}

	private void dropAction(EMaterialType materialType) {
		super.drop(materialType);
		if (materialType == EMaterialType.GOLD) {
			player.getEndgameStatistic().incrementAmountOfProducedGold();
		}
		jobFinished();
	}

	/**
	 * @param dijkstra
	 * 		if true, dijkstra algorithm is used<br>
	 * 		if false, in area finder is used.
	 */
	private void preSearchPathAction(boolean dijkstra) {
		super.setPosition(getCurrentJobPos());

		ShortPoint2D workAreaCenter = building.getWorkAreaCenter();

		boolean pathFound = super.preSearchPath(dijkstra, workAreaCenter.x, workAreaCenter.y, building.getBuildingVariant().getWorkRadius(),
				currentJob.getSearchType());

		if (pathFound) {
			jobFinished();
			searchFailedCtr = 0;
			this.building.setCannotWork(false);
		} else {
			jobFailed();
			searchFailedCtr++;

			if (searchFailedCtr > 10) {
				this.building.setCannotWork(true);
				player.showMessage(SimpleMessage.cannotFindWork(building));
			}
		}
	}

	private boolean tryTakingResource() {
		if(building.getBuildingVariant().isVariantOf(EBuildingType.FISHER)) {
			EDirection fishDirection = getDirection();
			return grid.tryTakingResource(fishDirection.getNextHexPoint(position), EResourceType.FISH);
		} else if(building.getBuildingVariant().isMine()) {
			return building.tryTakingResource();
		} else {
			return false;
		}
	}

	private void gotoAction() {
		if (!done) {
			this.done = true;
			if (!super.goToPos(getCurrentJobPos())) {
				jobFailed();
			}
		} else {
			jobFinished(); // start next action
		}
	}

	private void jobFinished() {
		this.currentJob = this.currentJob.getNextSucessJob();
		done = false;
	}

	private void jobFailed() {
		this.currentJob = this.currentJob.getNextFailJob();
		done = false;
	}

	private ShortPoint2D getCurrentJobPos() {
		return currentJob.calculatePoint(building);
	}

	private void lookAtSearched() {
		EDirection direction = grid.getDirectionOfSearched(position, currentJob.getSearchType());
		if (direction != null) {
			super.lookInDirection(direction);
			jobFinished();
		} else {
			jobFailed();
		}
	}

	@Override
	public void setWorkerJob(IWorkerRequestBuilding building) {
		this.building = building;
		this.currentJob = building.getBuildingVariant().getStartJob();
		super.enableNothingToDoAction(false);
		this.done = false;
		building.occupyBuilding(this);
	}

	@Override
	public void buildingDestroyed() {
		super.setVisible(true);
		super.abortPath();

		reportAsJobless();
		dropCurrentMaterial();
		clearMark();
	}

	private void dropCurrentMaterial() {
		EMaterialType material = getMaterial();
		if (material.isDroppable()) {
			grid.dropMaterial(position, material, true, false);
		}
		super.setMaterial(EMaterialType.NO_MATERIAL);
	}

	private void reportAsJobless() {
		grid.addJobless(this);
		super.enableNothingToDoAction(true);
		this.currentJob = null;
		this.building = null;
	}

	private void mark(ShortPoint2D position) {
		clearMark();
		markedPosition = position;
		grid.setMarked(position, true);
	}

	private void clearMark() {
		if (markedPosition != null) {
			grid.setMarked(markedPosition, false);
			markedPosition = null;
		}
	}

	@Override
	protected void strategyStopped() { // used in overriding methods
		dropCurrentMaterial();

		if (isJobless()) {
			grid.removeJobless(this);
		} else {
			super.enableNothingToDoAction(true);
			currentJob = null;
		}

		if (building != null) {
			building.leaveBuilding(this);
		}

		clearMark();
	}

	@Override
	protected void peacetimePathAborted(ShortPoint2D pathTarget) {
		if (currentJob != null) {
			jobFailed();
		}
		clearMark();
	}

	@Override
	protected boolean peacetimeCheckPathStepPreconditions(ShortPoint2D pathTarget, int step, EMoveToType moveToType) {
		return isJobless() || building != null;
	}
}
