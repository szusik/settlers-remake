package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.map.grid.partition.manager.manageables.IManageableBearer;
import jsettlers.logic.map.grid.partition.manager.manageables.interfaces.IBarrack;
import jsettlers.logic.map.grid.partition.manager.materials.interfaces.IMaterialOffer;
import jsettlers.logic.map.grid.partition.manager.materials.interfaces.IMaterialRequest;
import jsettlers.logic.map.grid.partition.manager.materials.offers.EOfferPriority;
import jsettlers.logic.map.grid.partition.manager.objects.WorkerCreationRequest;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IBearerMovable;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class BearerMovable extends Movable implements IBearerMovable, IManageableBearer {

	private IMaterialOffer   offer;
	private IMaterialRequest request;
	private EMaterialType    materialType;

	private IBarrack              barrack;
	private IManageableBearer.IWorkerRequester workerRequester;
	private WorkerCreationRequest workerCreationRequest;

	private boolean registered = false;

	public BearerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.BEARER, position, player, movable, tree);
	}

	private static final Root<BearerMovable> tree = new Root<>(createBearerBehaviour());

	@Override
	public ILogicMovable convertTo(EMovableType newMovableType) {
		return createMovable(newMovableType, player, position, grid, this);
	}

	private static Node<BearerMovable> createBearerBehaviour() {
		return guardSelector(
				guard(mov -> false,
					alwaysSucceed()
				),
				guard(mov -> mov.barrack != null,
					selector(
						sequence(
							goToPos(mov -> mov.barrack.getDoor(), mov -> false),
							condition(mov -> {
								EMovableType soldierType = mov.barrack.popWeaponForBearer();
								if(soldierType == null) return false;

								mov.player.getEndgameStatistic().incrementAmountOfProducedSoldiers();
								ILogicMovable convertedMovable = mov.convertTo(soldierType);

								// TODO change
								convertedMovable.moveTo(mov.barrack.getSoldierTargetPosition(), EMoveToType.DEFAULT);
								return true;
							})
						),
						BehaviorTreeHelper.action(BearerMovable::abortJob)
					)
				),
				guard(mov -> mov.workerCreationRequest != null,
					selector(
						sequence(
							selector(
								condition(mov -> mov.offer == null),
								handleOffer()
							),
							BehaviorTreeHelper.action(mov -> {
								EMovableType newMovable = mov.workerCreationRequest.requestedMovableType();
								mov.workerCreationRequest = null;
								mov.setMaterial(EMaterialType.NO_MATERIAL);
								mov.convertTo(newMovable);
							})
						),
						BehaviorTreeHelper.action(BearerMovable::abortJob)
					)
				),
				guard(mov -> mov.request != null && mov.request.isActive(),
					resetAfter(
						mov -> {
							EMaterialType carriedMaterial = mov.setMaterial(EMaterialType.NO_MATERIAL);
							boolean success = mov.request != null && mov.position.equals(mov.request.getPosition());

							if (carriedMaterial != EMaterialType.NO_MATERIAL) {
								mov.grid.dropMaterial(mov.position, carriedMaterial, !success, false);
							}
						},
						selector(
							sequence(
								handleOffer(),
								goToPos(mov -> mov.request.getPosition(), mov -> mov.request != null && mov.request.isActive()), //TODO
								drop(Movable::getMaterial)
							),
							BehaviorTreeHelper.action(BearerMovable::abortJob)
						)
					)
				),
				guard(mov -> !mov.registered,
					BehaviorTreeHelper.action(mov -> {
						mov.offer = null;
						mov.request = null;
						mov.barrack = null;
						mov.materialType = null;
						mov.workerRequester = null;
						mov.workerCreationRequest = null;
						mov.registered = true;
						mov.pathStep = null;
						mov.enableNothingToDoAction(true);
						mov.grid.addJobless(mov);
					})
				)
		);
	}

	private static Node<BearerMovable> handleOffer() {
		return sequence(
				goToPos(mov -> mov.offer.getPosition(), mov -> {
					if(mov.request != null && !mov.request.isActive()) return false; // TODO
					EOfferPriority minimumAcceptedPriority = mov.request != null ? mov.request.getMinimumAcceptedOfferPriority() : EOfferPriority.LOWEST;
					return mov.offer.isStillValid(minimumAcceptedPriority);
				}),
				take(mov -> mov.materialType, mov -> true)
		);
	}

	private void abortJob() {
		if(offer != null) offer.distributionAborted();

		if(workerCreationRequest != null) {
			workerRequester.workerCreationRequestFailed(workerCreationRequest);
			workerRequester = null;
			workerCreationRequest = null;
		}

		if(request != null) {
			request.deliveryAborted();
			request = null;
		}

		if(barrack != null) {
			barrack.bearerRequestFailed();
			barrack = null;
		}
	}

	protected void tookMaterial() {
		if (offer != null) {
			offer.offerTaken();
			offer = null;
		}
	}

	@Override
	public boolean droppingMaterial() {
		if (request != null) {
			if (request.isActive() && request.getPosition().equals(position)) {
				request.deliveryFulfilled();
				request = null;
				return false;
			} else {
				request.deliveryAborted();
				request = null;
			}
		}
		return true; // offer the material
	}

	@Override
	public void deliver(EMaterialType materialType, IMaterialOffer offer, IMaterialRequest request) {
		if(!registered) return;

		this.offer = offer;
		this.request = request;
		this.materialType = materialType;
		registered = false;

		offer.distributionAccepted();
		request.deliveryAccepted();
	}

	@Override
	public boolean becomeWorker(IManageableBearer.IWorkerRequester requester, WorkerCreationRequest workerCreationRequest, IMaterialOffer offer) {
		if(!registered) return false;

		this.workerRequester = requester;
		this.workerCreationRequest = workerCreationRequest;
		this.offer = offer;
		if(offer != null) {
			this.materialType = workerCreationRequest.requestedMovableType().getTool();
			offer.distributionAccepted();
		}
		registered = false;
		return true;
	}

	@Override
	public boolean becomeSoldier(IBarrack barrack) {
		if(!registered) return false;

		this.barrack = barrack;
		registered = false;
		return true;
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		abortJob();

		grid.removeJobless(this);
	}

}
