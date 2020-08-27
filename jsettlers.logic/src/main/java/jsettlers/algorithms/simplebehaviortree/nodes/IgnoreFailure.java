package jsettlers.algorithms.simplebehaviortree.nodes;

import jsettlers.algorithms.simplebehaviortree.Decorator;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.NodeStatus;
import jsettlers.algorithms.simplebehaviortree.Tick;

import static jsettlers.algorithms.simplebehaviortree.NodeStatus.*;

public class IgnoreFailure<T> extends Decorator<T> {

	public IgnoreFailure(Node<T> child) {
		super(child);
	}

	@Override
	protected NodeStatus onTick(Tick<T> tick) {
		NodeStatus returnStatus = child.execute(tick);

		switch (returnStatus) {
			case RUNNING:
				return RUNNING;
			default:
			case FAILURE:
			case SUCCESS:
				return SUCCESS;
		}
	}
}
