package jsettlers.algorithms.simplebehaviortree.nodes;

import jsettlers.algorithms.simplebehaviortree.Composite;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.NodeStatus;
import jsettlers.algorithms.simplebehaviortree.Tick;

public class MemSelector<T> extends Composite<T> {
	private static final long serialVersionUID = -4098732000225742833L;

	private int index = 0;

	@SafeVarargs
	public MemSelector(Node<T>... children) {
		super(children);
	}

	@Override
	protected NodeStatus onTick(Tick<T> tick) {
		for (; index < children.size(); index++) {
			NodeStatus status = children.get(index).execute(tick);
			if (status != NodeStatus.FAILURE) {
				return status;
			}
		}
		return NodeStatus.FAILURE;
	}

	@Override
	protected void onOpen(Tick<T> tick) {
		index = 0;
	}
}
