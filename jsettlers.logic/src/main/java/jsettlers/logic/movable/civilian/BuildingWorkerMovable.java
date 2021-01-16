package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.algorithms.simplebehaviortree.nodes.Guard;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.landscape.EResourceType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.menu.messages.SimpleMessage;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.map.grid.partition.manager.manageables.IManageableWorker;
import jsettlers.logic.map.grid.partition.manager.manageables.interfaces.IWorkerRequestBuilding;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IBuildingWorkerMovable;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class BuildingWorkerMovable extends CivilianMovable implements IBuildingWorkerMovable, IManageableWorker {

	protected IWorkerRequestBuilding building;
	private boolean registered = false;
	protected int searchFailedCtr = 0;

	public BuildingWorkerMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable replace, Root<? extends BuildingWorkerMovable> behaviour) {
		super(grid, movableType, position, player, replace, behaviour);
	}

	protected static <T extends BuildingWorkerMovable> Guard<T> handleBuildingDestroyedGuard() {
		return guard(mov -> mov.building != null && mov.building.isDestroyed(),
				BehaviorTreeHelper.action(BuildingWorkerMovable::buildingDestroyed)
		);
	}

	protected static <T extends BuildingWorkerMovable> Guard<T> registerMovableGuard() {
		return guard(mov -> !((BuildingWorkerMovable)mov).registered,
				BehaviorTreeHelper.action(mov -> {
					mov.dropCurrentMaterial();
					mov.building = null;
					((BuildingWorkerMovable)mov).registered = true;
					mov.pathStep = null;
					mov.grid.addJobless(mov);
				})
		);
	}

	@Override
	protected boolean isBusy() {
		return super.isBusy() || !registered;
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
	protected void decoupleMovable() {
		super.decoupleMovable();

		dropCurrentMaterial();

		if(building != null) {
			building.leaveBuilding(this);
		} else {
			grid.removeJobless(this);
		}
	}

	/*protected void dropAction(EMaterialType materialType) {
		if (materialType == EMaterialType.GOLD) {
			player.getEndgameStatistic().incrementAmountOfProducedGold();
		}
	}*/

	/*
	 * @param dijkstra
	 * 		if true, dijkstra algorithm is used<br>
	 * 		if false, in area finder is used.
	 */
	protected static <T extends BuildingWorkerMovable> Node<T> preSearchPath(boolean dijkstra, ESearchType searchType) {
		return BehaviorTreeHelper.condition(mov -> {
			ShortPoint2D workAreaCenter = mov.building.getWorkAreaCenter();

			boolean pathFound = mov.preSearchPath(dijkstra, workAreaCenter.x, workAreaCenter.y, mov.building.getBuildingVariant().getWorkRadius(),
					searchType);

			if (pathFound) {
				mov.searchFailedCtr = 0;
				mov.building.setCannotWork(false);
				return true;
			} else {
				mov.searchFailedCtr++;

				if (mov.searchFailedCtr > 10) {
					mov.building.setCannotWork(true);
					mov.player.showMessage(SimpleMessage.cannotFindWork(mov.building));
				}
				return false;
			}
		});
	}

	protected boolean tryTakingResource() {
		if(building.getBuildingVariant().isVariantOf(EBuildingType.FISHER)) {
			EDirection fishDirection = getDirection();
			return grid.tryTakingResource(fishDirection.getNextHexPoint(position), EResourceType.FISH);
		} else if(building.getBuildingVariant().isMine()) {
			return building.tryTakingResource();
		} else {
			return false;
		}
	}

	@Override
	public void setWorkerJob(IWorkerRequestBuilding building) {
		this.building = building;
		building.occupyBuilding(this);
		this.registered = false;
	}

	@Override
	protected void abortJob() {
		if(building != null) {
			building.leaveBuilding(this);
			building = null;
		}

		dropCurrentMaterial();
	}

	@Override
	public void buildingDestroyed() {
		setVisible(true);

		dropCurrentMaterial();

		building = null;
	}

	protected void dropCurrentMaterial() {
		EMaterialType material = getMaterial();
		if (material.isDroppable()) {
			grid.dropMaterial(position, material, true, false);
		}
		super.setMaterial(EMaterialType.NO_MATERIAL);
	}

	protected boolean tmpPathStep() {
		return building != null;
	}
}
