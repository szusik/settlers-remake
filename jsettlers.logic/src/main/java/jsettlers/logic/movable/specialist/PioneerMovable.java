package jsettlers.logic.movable.specialist;

import jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EDirection;
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
	private ShortPoint2D goToTarget = null;

	private ShortPoint2D centerPosition = null;
	private ShortPoint2D workTarget = null;
	private EDirection workDirection = null;

	public PioneerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.PIONEER, position, player, movable, behaviour);
	}

	private static final Root<PioneerMovable> behaviour = new Root<>(createPioneerBehaviour());

	private static Node<PioneerMovable> createPioneerBehaviour() {
		return guardSelector(
				guard(mov -> mov.nextTarget != null,
					BehaviorTreeHelper.action(mov -> {
						if(mov.nextMoveToType.isWorkOnDestination()) {
							mov.currentTarget = mov.nextTarget;
						} else {
							mov.goToTarget = mov.nextTarget;
						}
						mov.nextTarget = null;
					})
				),
				guard(mov -> mov.goToTarget != null,
					resetAfter(
						mov -> mov.goToTarget = null,
						goToPos(mov -> mov.goToTarget, mov -> mov.nextTarget == null && mov.goToTarget != null) // TODO
					)
				),
				guard(mov -> mov.currentTarget != null,
					resetAfter(mov -> {
						mov.currentTarget = null;
						mov.workDirection = null;
						mov.workTarget = null;
					},

						sequence(
							BehaviorTreeHelper.action(mov -> {
								if(mov.centerPosition == null) {
									mov.centerPosition = mov.currentTarget;
								}
							}),
							goToPos(mov -> mov.currentTarget, mov -> mov.currentTarget != null && mov.nextTarget == null), // TODO
							repeat(mov -> true,
								resetAfter(mov -> {
									if(mov.workTarget != null) {
										mov.grid.setMarked(mov.workTarget, false);
										mov.workTarget = null;
									}
								},

									sequence(
										findWorkablePosition(),
										ignoreFailure(workOnPosition())
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
				sequence(
					condition(mov -> {
						mov.workDirection = null;
						double[] bestNeighbourDistance = new double[] { Double.MAX_VALUE }; // distance from start point

						ShortPoint2D position = mov.getPosition();
						HexGridArea.stream(position.x, position.y, 1, 6)
								.filter((x, y) -> mov.grid.isValidPosition(mov, x, y) && mov.canWorkOnPos(x, y))
								.forEach((x, y) -> {
									double distance = ShortPoint2D.getOnGridDist(x - mov.currentTarget.x, y - mov.currentTarget.y);
									if (distance < bestNeighbourDistance[0]) {
										bestNeighbourDistance[0] = distance;
										mov.workDirection = EDirection.getApproxDirection(position.x, position.y, x, y);
									}
								});
						return mov.workDirection != null;
					}),
					BehaviorTreeHelper.action(mov -> {
						mov.workTarget = mov.workDirection.getNextHexPoint(mov.position);
						mov.grid.setMarked(mov.workTarget, true);
					}),
					goInDirectionIfAllowedAndFree(mov -> mov.workDirection)
				),
				sequence(
					BehaviorTreeHelper.action(mov -> {
						mov.centerPosition = null;
					}),
					alwaysFail()
				),
				sequence(
					condition(mov -> mov.preSearchPath(true, mov.position.x, mov.position.y, (short) 30, ESearchType.UNENFORCED_FOREIGN_GROUND)),
					BehaviorTreeHelper.action(mov -> {
						mov.workTarget = mov.path.getTargetPosition();
						mov.grid.setMarked(mov.workTarget, true);
					}),
					followPresearchedPath(mov -> mov.currentTarget != null && mov.nextTarget == null) // TODO
				)
		);
	}

	private boolean canWorkOnPos(int x, int y) {
		return grid.fitsSearchType(this, x, y, ESearchType.UNENFORCED_FOREIGN_GROUND);
	}

	protected static Node<PioneerMovable> workOnPosition() {
		return sequence(
				condition(mov -> {
					mov.grid.setMarked(mov.workTarget, false);
					boolean success = mov.grid.fitsSearchType(mov, mov.workTarget.x, mov.workTarget.y, ESearchType.UNENFORCED_FOREIGN_GROUND);
					mov.grid.setMarked(mov.workTarget, true);

					return success;
				}),

				playAction(EMovableAction.ACTION1, mov -> (short)(ACTION1_DURATION*1000)),

				BehaviorTreeHelper.action(mov -> {
					mov.grid.changePlayerAt(mov.workTarget, mov.getPlayer());
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
		nextTarget = position;
		nextMoveToType = stop? EMoveToType.FORCED : EMoveToType.DEFAULT;
	}
}
