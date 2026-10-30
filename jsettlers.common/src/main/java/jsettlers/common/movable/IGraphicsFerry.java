package jsettlers.common.movable;

import java.util.List;

public interface IGraphicsFerry extends IGraphicsMovable {
	int MAX_NUMBER_OF_PASSENGERS = 7;

	List<? extends IGraphicsMovable> getPassengers();
}
