package jsettlers.algorithms.simplebehaviortree.nodes;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.NodeStatus;
import jsettlers.algorithms.simplebehaviortree.Tick;

public final class AlwaysFail<T> extends Node<T> {
	private static final long serialVersionUID = 5577842967150867903L;

	@Override
	protected NodeStatus onTick(Tick<T> tick) {
		return NodeStatus.FAILURE;
	}
}
