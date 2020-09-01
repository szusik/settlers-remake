package jsettlers.logic.movable.civilian;

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

public class BearerMovable extends CivilianMovable implements IBearerMovable, IManageableBearer {

	private BearerMovable.EBearerState state = BearerMovable.EBearerState.JOBLESS;

	private IMaterialOffer   offer;
	private IMaterialRequest request;
	private EMaterialType    materialType;

	private IBarrack              barrack;
	private IManageableBearer.IWorkerRequester workerRequester;
	private WorkerCreationRequest workerCreationRequest;

	public BearerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.BEARER, position, player, movable);
	}

	@Override
	public ILogicMovable convertTo(EMovableType newMovableType) {
		return createMovable(newMovableType, player, position, grid, this);
	}

	@Override
	public void strategyStarted() {
		reportJobless();
	}

	public final void reportJobless() {
		state = BearerMovable.EBearerState.JOBLESS;
		grid.addJobless(this);
	}

	@Override
	protected void peacetimeAction() {
		switch (state) {
			case JOBLESS:
				break;

			case INIT_CONVERT_WITH_TOOL_JOB:
			case INIT_CARRY_JOB:
				state = BearerMovable.EBearerState.GOING_TO_OFFER;

				if (!position.equals(offer.getPosition())) { // if we are not at the offers position, go to it.
					if (!goToPos(offer.getPosition())) {
						handleJobFailed(true);
					}
					break;
				}
			case GOING_TO_OFFER:
				if (position.equals(offer.getPosition())) {
					state = BearerMovable.EBearerState.TAKING;
					if (!take(materialType, true)) {
						handleJobFailed(true);
					}
				} else {
					handleJobFailed(true);
				}
				break;

			case TAKING:
				if (workerCreationRequest != null) { // we handle a convert with tool job
					state = BearerMovable.EBearerState.DEAD_OBJECT;
					super.setMaterial(EMaterialType.NO_MATERIAL);
					convertTo(workerCreationRequest.requestedMovableType());
				} else {
					offer = null;
					state = BearerMovable.EBearerState.GOING_TO_REQUEST;
					if (!position.equals(request.getPosition()) && !super.goToPos(request.getPosition())) {
						handleJobFailed(true);
					}
				}
				break;

			case GOING_TO_REQUEST:
				if (position.equals(request.getPosition())) {
					state = BearerMovable.EBearerState.DROPPING;
					super.drop(materialType);
				} else {
					handleJobFailed(true);
				}
				break;

			case DROPPING:
				request = null;
				materialType = null;
				reportJobless();
				break;

			case INIT_CONVERT_JOB:
				state = BearerMovable.EBearerState.DEAD_OBJECT;
				convertTo(workerCreationRequest.requestedMovableType());
				break;

			case INIT_BECOME_SOLDIER_JOB:
				super.goToPos(barrack.getDoor());
				state = BearerMovable.EBearerState.GOING_TO_BARRACK;
				break;

			case GOING_TO_BARRACK:
				EMovableType movableType = barrack.popWeaponForBearer();
				if (movableType == null) { // weapon got missing, make this bearer jobless again
					this.barrack = null;
					reportJobless();
				} else {
					this.state = BearerMovable.EBearerState.DEAD_OBJECT;
					player.getEndgameStatistic().incrementAmountOfProducedSoldiers();
					ILogicMovable convertedMovable = convertTo(movableType);

					convertedMovable.moveTo(barrack.getSoldierTargetPosition(), EMoveToType.DEFAULT);
				}
				break;

			case DEAD_OBJECT:
				assert false : "we should never get here!";
		}
	}

	protected void tookMaterial() {
		if (offer != null) {
			offer.offerTaken();
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

	private void handleJobFailed(boolean reportAsJobless) {
		switch (state) {
			case INIT_CARRY_JOB:
			case GOING_TO_OFFER:
			case TAKING:
				if (getMaterial() == EMaterialType.NO_MATERIAL) {
					reoffer();
				}
				if (workerCreationRequest != null) {
					workerRequester.workerCreationRequestFailed(workerCreationRequest);
				}
			case GOING_TO_REQUEST:
				if (request != null) {
					request.deliveryAborted();
				}
				break;

			case DROPPING:
				if (request != null) {
					boolean offerMaterial = droppingMaterial();
					super.setMaterial(EMaterialType.NO_MATERIAL);
					grid.dropMaterial(position, materialType, offerMaterial, false);
				}
				break;

			case INIT_BECOME_SOLDIER_JOB:
			case GOING_TO_BARRACK:
				barrack.bearerRequestFailed();
				break;

			case INIT_CONVERT_WITH_TOOL_JOB:
				reoffer();
			case INIT_CONVERT_JOB:
				workerRequester.workerCreationRequestFailed(workerCreationRequest);
				break;

			case DEAD_OBJECT:
				break;
			case JOBLESS:
				break;
			default:
				break;
		}

		EMaterialType carriedMaterial = super.setMaterial(EMaterialType.NO_MATERIAL);
		if (carriedMaterial != EMaterialType.NO_MATERIAL) {
			grid.dropMaterial(position, materialType, true, false);
		}

		offer = null;
		request = null;
		materialType = null;
		workerCreationRequest = null;
		workerRequester = null;

		if (reportAsJobless) {
			state = BearerMovable.EBearerState.JOBLESS;
			reportJobless();
		}
	}

	private void reoffer() {
		offer.distributionAborted();
	}

	@Override
	protected boolean peacetimeCheckPathStepPreconditions(ShortPoint2D pathTarget, int step, EMoveToType moveToType) {
		if (request != null && !request.isActive()) {
			return false;
		}

		if (offer != null) {
			EOfferPriority minimumAcceptedPriority = request != null ? request.getMinimumAcceptedOfferPriority() : EOfferPriority.LOWEST;
			if (!offer.isStillValid(minimumAcceptedPriority)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void deliver(EMaterialType materialType, IMaterialOffer offer, IMaterialRequest request) {
		if (state == BearerMovable.EBearerState.JOBLESS) {
			this.offer = offer;
			this.request = request;
			this.materialType = materialType;
			this.state = BearerMovable.EBearerState.INIT_CARRY_JOB;

			offer.distributionAccepted();
			request.deliveryAccepted();
		}
	}

	@Override
	public boolean becomeWorker(IManageableBearer.IWorkerRequester requester, WorkerCreationRequest workerCreationRequest, IMaterialOffer offer) {
		if (state == BearerMovable.EBearerState.JOBLESS) {
			this.workerRequester = requester;
			this.workerCreationRequest = workerCreationRequest;
			this.offer = offer;
			if(offer != null) {
				this.state = BearerMovable.EBearerState.INIT_CONVERT_WITH_TOOL_JOB;
				this.materialType = workerCreationRequest.requestedMovableType().getTool();

				offer.distributionAccepted();
			} else {
				this.state = EBearerState.INIT_CONVERT_JOB;
				this.materialType = null;
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean becomeSoldier(IBarrack barrack) {
		if (state == BearerMovable.EBearerState.JOBLESS) {
			this.barrack = barrack;
			this.state = BearerMovable.EBearerState.INIT_BECOME_SOLDIER_JOB;

			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void strategyStopped() {
		if (state == BearerMovable.EBearerState.JOBLESS) {
			grid.removeJobless(this);
		} else {
			handleJobFailed(false);
		}
		state = BearerMovable.EBearerState.DEAD_OBJECT;
	}

	@Override
	protected void peacetimePathAborted(ShortPoint2D pathTarget) {
		if (state != BearerMovable.EBearerState.JOBLESS) {
			handleJobFailed(true);
		}
	}

	/**
	 * This enum defines the internal states of a bearer.
	 *
	 * @author Andreas Eberle
	 *
	 */
	public enum EBearerState {
		JOBLESS,

		INIT_CARRY_JOB,
		GOING_TO_REQUEST,
		GOING_TO_OFFER,
		TAKING,
		DROPPING,

		INIT_CONVERT_JOB,
		INIT_CONVERT_WITH_TOOL_JOB,

		DEAD_OBJECT,

		INIT_BECOME_SOLDIER_JOB,
		GOING_TO_BARRACK,
	}
}
