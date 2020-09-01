package jsettlers.input.tasks;

import java.util.Objects;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;

public class ChangeMovableRatioTask extends SimpleGuiTask {

	private final ShortPoint2D position;
	private final EMovableType moveableType;

	public ChangeMovableRatioTask(EGuiAction action, byte playerId, ShortPoint2D position, EMovableType moveableType) {
		super(action, playerId);
		this.position = position;
		this.moveableType = moveableType;
	}

	public ShortPoint2D getPosition() {
		return position;
	}

	public EMovableType getMoveableType() {
		return moveableType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(moveableType, position);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof ChangeMovableRatioTask)) {
			return false;
		}
		ChangeMovableRatioTask other = (ChangeMovableRatioTask) obj;
		return moveableType == other.moveableType && Objects.equals(position, other.position) && getGuiAction() == other.getGuiAction();
	}

	@Override
	public String toString() {
		return "ChangeMovableRatioTask [position=" + position + ", moveableType=" + moveableType + ", action=" + getGuiAction() + "]";
	}
}
