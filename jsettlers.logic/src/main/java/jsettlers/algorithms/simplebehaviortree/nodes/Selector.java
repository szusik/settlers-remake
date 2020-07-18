package jsettlers.algorithms.simplebehaviortree.nodes;

import jsettlers.algorithms.simplebehaviortree.Composite;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.NodeStatus;
import jsettlers.algorithms.simplebehaviortree.Tick;

public class Selector<T> extends Composite<T> {
	private static final long serialVersionUID = 6187523767823138311L;

	private int index;

	public Selector(Node<T>[] children) {
		super(children);
	}

	@Override
	protected NodeStatus onTick(Tick<T> tick) {
		for (; index < children.size(); index++) {
			NodeStatus status = children.get(index).execute(tick);

			if(status != NodeStatus.FAILURE) {
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
