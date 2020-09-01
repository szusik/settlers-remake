package jsettlers.common.action;

import java.util.Objects;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;

public class ChangeMovableRatioAction extends PointAction {

	private final EMovableType moveableType;

	public ChangeMovableRatioAction(EActionType action, EMovableType moveableType, ShortPoint2D position) {
		super(action, position);
		this.moveableType = moveableType;
	}

	public EMovableType getMoveableType() {
		return moveableType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(moveableType, getPosition(), getActionType());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ChangeMovableRatioAction)) {
			return false;
		}
		ChangeMovableRatioAction other = (ChangeMovableRatioAction) obj;
		return moveableType == other.moveableType && getActionType() == other.getActionType();
	}

	@Override
	public String toString() {
		return "ChangeMovableRatioAction [moveableType=" + moveableType + ", action=" + getActionType() + ",position=" + getPosition() + "]";
	}
}
