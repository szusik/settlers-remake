package jsettlers.algorithms.simplebehaviortree.nodes;

import jsettlers.algorithms.simplebehaviortree.Composite;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.NodeStatus;
import jsettlers.algorithms.simplebehaviortree.Tick;

public class Sequence<T> extends Composite<T> {
	private static final long serialVersionUID = -1764866628237365366L;

	@SafeVarargs
	public Sequence(Node<T>... children) {
		super(children);
	}

	@Override
	protected NodeStatus onTick(Tick<T> tick) {
		for (Node<T> node : children) {
			NodeStatus status = node.execute(tick);
			if (status != NodeStatus.SUCCESS) {
				return status;
			}
		}
		return NodeStatus.SUCCESS;
	}
}
