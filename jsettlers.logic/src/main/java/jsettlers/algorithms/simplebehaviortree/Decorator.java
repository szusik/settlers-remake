package jsettlers.algorithms.simplebehaviortree;

public abstract class Decorator<T> extends Node<T> {

	protected final Node<T> child;

	protected Decorator(Node<T> child) {
		this.child = child;
	}

	@Override
	int initiate(int maxId) {
		maxId = super.initiate(maxId);

		if(child != null) {
			maxId = child.initiate(maxId);
		}
		return maxId;
	}
}
