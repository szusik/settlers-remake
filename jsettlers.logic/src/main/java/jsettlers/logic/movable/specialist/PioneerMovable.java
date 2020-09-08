package jsettlers.logic.movable.specialist;

import jsettlers.algorithms.path.Path;
import jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.MutablePoint2D;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.utils.mutables.MutableDouble;
import jsettlers.logic.movable.interfaces.IFerryMovable;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IPioneerMovable;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class PioneerMovable extends AttackableHumanMovable implements IPioneerMovable {

	private static final float ACTION1_DURATION = 1.2f;

	private EMoveToType nextMoveToType;
	private ShortPoint2D nextTarget = null;
	private ShortPoint2D currentTarget = null;

	public PioneerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.PIONEER, position, player, movable, behaviour);
	}

	private static final Root<PioneerMovable> behaviour = new Root<>(createPioneerBehaviour());

	private static Node<PioneerMovable> createPioneerBehaviour() {
		return guardSelector(
				guard(mov -> mov.nextTarget != null,
					sequence(
						BehaviorTreeHelper.action(mov -> {
							mov.currentTarget = mov.nextTarget;
							mov.nextTarget = null;
						}),
						findWorkablePosition()
					)
				),
				guard(mov -> mov.currentTarget != null,
					resetAfter(mov -> mov.currentTarget = null,

						sequence(
							goToPos(mov -> mov.currentTarget, mov -> mov.currentTarget != null && mov.nextTarget == null), // TODO
							repeat(mov -> true,
								sequence(
									findWorkablePosition(),
									resetAfter(mov -> mov.grid.setMarked(mov.currentTarget, false),

										sequence(
											BehaviorTreeHelper.action(mov -> {mov.grid.setMarked(mov.currentTarget, true);}),
											goToPos(mov -> mov.currentTarget, mov -> mov.currentTarget != null && mov.nextTarget == null), // TODO
											ignoreFailure(workOnPosition())
										)
									)
								)
							)
						)
					)
				)
		);
	}

	@Override
	public boolean convertToBearer() {
		if(!player.equals(grid.getPlayerAt(position))) return false;

		createMovable(EMovableType.BEARER, player, position, grid, this);

		return true;
	}

	protected static Node<PioneerMovable> findWorkablePosition() {
		return selector(
				condition(mov -> {
					MutablePoint2D bestNeighbourPos = new MutablePoint2D(-1, -1);
					MutableDouble bestNeighbourDistance = new MutableDouble(Double.MAX_VALUE); // distance from start point

					HexGridArea.streamBorder(mov.currentTarget, 6)
							.filterBounds(mov.grid.getWidth(), mov.grid.getHeight())
							.filter((x, y) -> mov.grid.fitsSearchType(mov, x, y, ESearchType.UNENFORCED_FOREIGN_GROUND))
							.forEach((x, y) -> {
								double distance = ShortPoint2D.getOnGridDist(x - mov.currentTarget.x, y - mov.currentTarget.y);
								if (distance < bestNeighbourDistance.value) {
									bestNeighbourDistance.value = distance;
									bestNeighbourPos.x = x;
									bestNeighbourPos.y = y;
								}
							});

					if(bestNeighbourDistance.value != Double.MAX_VALUE) {
						mov.currentTarget = bestNeighbourPos.createShortPoint2D();
						return true;
					}
					return false;
				}),

				condition(mov -> {
					Path dijkstraPath = mov.grid.searchDijkstra(mov, mov.currentTarget.x, mov.currentTarget.y, (short) 30, ESearchType.UNENFORCED_FOREIGN_GROUND);
					if (dijkstraPath != null) {
						mov.currentTarget = dijkstraPath.getTargetPosition();
						return true;
					}
					return false;
				})
		);
	}

	protected static Node<PioneerMovable> workOnPosition() {
		return sequence(
				condition(mov -> {
					mov.grid.setMarked(mov.currentTarget, false);
					boolean success = mov.grid.fitsSearchType(mov, mov.currentTarget.x, mov.currentTarget.y, ESearchType.UNENFORCED_FOREIGN_GROUND);
					mov.grid.setMarked(mov.currentTarget, true);

					return success;
				}),

				playAction(EMovableAction.ACTION1, mov -> (short)(ACTION1_DURATION*1000)),

				BehaviorTreeHelper.action(mov -> {
					mov.grid.changePlayerAt(mov.currentTarget, mov.getPlayer());
				})
		);
	}

	@Override
	public void moveTo(ShortPoint2D targetPosition, EMoveToType moveToType) {
		if(playerControlled && (!targetPosition.equals(currentTarget) || nextMoveToType != moveToType)) {
			nextTarget = targetPosition;
			nextMoveToType = moveToType;
		}
	}

	@Override
	public void moveToFerry(IFerryMovable ferry, ShortPoint2D entrancePosition) {
		if(playerControlled && (!entrancePosition.equals(currentTarget) || ferryToEnter != ferry)) {
			ferryToEnter = ferry;
			nextTarget = entrancePosition;
			nextMoveToType = EMoveToType.FORCED;
		}
	}

	@Override
	public void stopOrStartWorking(boolean stop) {
		nextTarget = stop? null: position;
	}
}
