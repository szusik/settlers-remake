package jsettlers.algorithms.simplebehaviortree.nodes;

import jsettlers.algorithms.simplebehaviortree.Composite;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.NodeStatus;
import jsettlers.algorithms.simplebehaviortree.Tick;

public class Selector<T> extends Composite<T> {
	private static final long serialVersionUID = 6187523767823138311L;

	@SafeVarargs
	public Selector(Node<T>... children) {
		super(children);
	}

	@Override
	protected NodeStatus onTick(Tick<T> tick) {
		for (Node<T> node : children) {
			NodeStatus status = node.execute(tick);
			if (!status.equals(NodeStatus.FAILURE)) {
				return status;
			}
		}
		return NodeStatus.FAILURE;
	}
}
