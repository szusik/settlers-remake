package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.RelativePoint;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.partition.manager.manageables.IManageableDigger;
import jsettlers.logic.map.grid.partition.manager.manageables.interfaces.IDiggerRequester;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class DiggerMovable extends CivilianMovable implements IManageableDigger {

	private IDiggerRequester requester = null;
	private boolean registered = false;
	private ShortPoint2D targetPosition;

	public DiggerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.DIGGER, position, player, movable, tree);
	}

	private static final Root<DiggerMovable> tree = new Root<>(createDiggerBehaviour());

	private static Node<DiggerMovable> createDiggerBehaviour() {
		return guardSelector(
				fleeIfNecessary(),
				guard(mov -> mov.requester != null && mov.requester.isDiggerRequestActive(),
					selector(
						repeat(DiggerMovable::flattenPositionsRemaining,
							sequence(
								condition(DiggerMovable::findDiggablePosition),
								resetAfter(
									mov -> mov.grid.setMarked(mov.targetPosition, false),
									sequence(
										BehaviorTreeHelper.action(mov -> {mov.grid.setMarked(mov.targetPosition, true);}),
										goToPos(mov -> mov.targetPosition, mov -> mov.requester != null && mov.requester.isDiggerRequestActive()), // TODO
										playAction(EMovableAction.ACTION1, (short)1000),
										BehaviorTreeHelper.action(DiggerMovable::executeDigg)
									)
								)
							)
						),
						BehaviorTreeHelper.action(DiggerMovable::abortJob)
					)
				),
				guard(mov -> !mov.registered,
					BehaviorTreeHelper.action(mov -> {
						mov.requester = null;
						mov.registered = true;
						mov.grid.addJobless(mov);
					})
				),
				doingNothingGuard()
		);
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		if(requester != null) {
			abortJob();
		} else {
			grid.removeJobless(this);
		}
	}

	@Override
	public boolean setDiggerJob(IDiggerRequester requester) {
		if (this.requester == null) {
			this.requester = requester;
			registered = false;
			return true;
		} else {
			return false;
		}
	}

	private void executeDigg() {
		grid.changeHeightTowards(position.x, position.y, requester.getAverageHeight());
	}

	private boolean flattenPositionsRemaining() {
		for (RelativePoint relativePosition : requester.getBuildingVariant().getProtectedTiles()) {
			if (needsToBeWorkedOn(relativePosition.calculatePoint(requester.getPosition()))) {
				return true;
			}
		}
		return false;
	}

	private boolean findDiggablePosition() {
		RelativePoint[] blockedTiles = requester.getBuildingVariant().getProtectedTiles();
		ShortPoint2D buildingPos = requester.getPosition();
		int offset = MatchConstants.random().nextInt(blockedTiles.length);

		for (int i = 0; i < blockedTiles.length; i++) {
			ShortPoint2D pos = blockedTiles[(i + offset) % blockedTiles.length].calculatePoint(buildingPos);
			if (!grid.isMarked(pos) && needsToBeWorkedOn(pos)) {
				targetPosition = pos;
				return true;
			}
		}
		return false;
	}

	private boolean needsToBeWorkedOn(ShortPoint2D pos) {
		return needsToChangeHeight(pos) || isNotFlattened(pos);
	}

	private boolean isNotFlattened(ShortPoint2D pos) {
		// some places can't be flattened
		if(!grid.canChangeLandscapeTo(pos.x, pos.y, ELandscapeType.FLATTENED)) return false;

		return grid.getLandscapeTypeAt(pos.x, pos.y) != ELandscapeType.FLATTENED;
	}

	private boolean needsToChangeHeight(ShortPoint2D pos) {
		return grid.getHeightAt(pos) != requester.getAverageHeight();
	}

	@Override
	protected void abortJob() {
		if(requester != null) {
			requester.diggerRequestFailed();
			requester = null;
		}
	}
}
