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
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class GeologistMovable extends AttackableHumanMovable {

	private static final float ACTION1_DURATION = 1.4f;
	private static final float ACTION2_DURATION = 1.5f;

	private EMoveToType nextMoveToType;
	private ShortPoint2D nextTarget = null;
	private ShortPoint2D currentTarget = null;

	public GeologistMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.GEOLOGIST, position, player, movable, behaviour);
	}

	private static final Root<GeologistMovable> behaviour = new Root<>(createGeologistBehaviour());

	public static Node<GeologistMovable> createGeologistBehaviour() {
		return guardSelector(
				guard(mov -> mov.nextTarget != null,
					BehaviorTreeHelper.action(mov -> {
						mov.currentTarget = mov.nextTarget;
						mov.nextTarget = null;
					})
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

	protected static Node<GeologistMovable> findWorkablePosition() {
		return selector(
				condition(mov -> {
					MutablePoint2D bestNeighbourPos = new MutablePoint2D(-1, -1);
					MutableDouble bestNeighbourDistance = new MutableDouble(Double.MAX_VALUE); // distance from start point

					HexGridArea.streamBorder(mov.currentTarget, 2)
							.filterBounds(mov.grid.getWidth(), mov.grid.getHeight())
							.filter((x, y) -> mov.grid.fitsSearchType(mov, x, y, ESearchType.RESOURCE_SIGNABLE))
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
					Path dijkstraPath = mov.grid.searchDijkstra(mov, mov.currentTarget.x, mov.currentTarget.y, (short) 30, ESearchType.RESOURCE_SIGNABLE);
					if (dijkstraPath != null) {
						mov.currentTarget = dijkstraPath.getTargetPosition();
						return true;
					}
					return false;
				})
		);
	}

	protected static Node<GeologistMovable> workOnPosition() {
		return sequence(
				condition(mov -> {
					mov.grid.setMarked(mov.currentTarget, false);
					boolean success = mov.grid.fitsSearchType(mov, mov.currentTarget.x, mov.currentTarget.y, ESearchType.RESOURCE_SIGNABLE);
					mov.grid.setMarked(mov.currentTarget, true);

					return success;
				}),

				playAction(EMovableAction.ACTION1, mov -> (short)(ACTION1_DURATION*1000)),
				playAction(EMovableAction.ACTION2, mov -> (short)(ACTION2_DURATION*1000)),

				condition(mov -> {
					mov.grid.setMarked(mov.currentTarget, false);
					boolean success = mov.grid.executeSearchType(mov, mov.currentTarget, ESearchType.RESOURCE_SIGNABLE);
					mov.grid.setMarked(mov.currentTarget, true);

					return success;
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
