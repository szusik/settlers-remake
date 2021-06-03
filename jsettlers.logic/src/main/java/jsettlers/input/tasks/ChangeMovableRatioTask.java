package jsettlers.input.tasks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;

public class ChangeMovableRatioTask extends SimpleGuiTask {

	private ShortPoint2D position;
	private EMovableType moveableType;

	public ChangeMovableRatioTask() {
	}

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
	protected void serializeTask(DataOutputStream dos) throws IOException {
		super.serializeTask(dos);
		SimpleGuiTask.serializePosition(dos, position);
		dos.writeInt(moveableType.ordinal());
	}

	@Override
	protected void deserializeTask(DataInputStream dis) throws IOException {
		super.deserializeTask(dis);
		position = SimpleGuiTask.deserializePosition(dis);
		moveableType = EMovableType.VALUES[dis.readInt()];
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
