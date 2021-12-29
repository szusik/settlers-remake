package go.graphics.swing.util;

import java8.util.Lists2;

import javax.swing.AbstractListModel;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MutableListModel<T> extends AbstractListModel<T> {

	private List<T> data = Lists2.of();

	public void setData(Set<T> set, Comparator<T> cmp) {
		this.data = set.stream().sorted(cmp).collect(Collectors.toList());
	}

	public void setData(List<T> data) {
		this.data.clear();
		this.data.addAll(data);
	}

	@Override
	public int getSize() {
		return data.size();
	}

	@Override
	public T getElementAt(int index) {
		return data.get(index);
	}
}
