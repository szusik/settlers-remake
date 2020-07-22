package jsettlers.algorithms.simplebehaviortree;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Tick<T> implements Serializable {
	private static final long serialVersionUID = 3673558738736795584L;

	public final Root<T> root;
	public final T       target;

	private final Set<Node<T>> openNodes = new HashSet<>();

	public Tick(T target, Root<T> root) {
		this.root = root;
		this.target = target;
	}

	public NodeStatus tick() {
		return root.execute(this);
	}

	public boolean isOpen(Node<T> node) {
		return openNodes.contains(node);
	}

	public void visitNode(Node<T> node) {
		openNodes.add(node);
	}

	public void tickNode(Node<T> node) {
	}

	public void leaveNode(Node<T> node) {
		openNodes.remove(node);
	}




	private Map<Integer, Object> properties = new HashMap<>();

	public <I> I getProperty(int id) {
		return (I) properties.get(id);
	}

	public void setProperty(int id, Object value) {
		properties.put(id, value);
	}
}
