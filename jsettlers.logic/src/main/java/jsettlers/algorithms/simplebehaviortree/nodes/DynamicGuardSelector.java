/*
 * Copyright (c) 2018
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package jsettlers.algorithms.simplebehaviortree.nodes;

import jsettlers.algorithms.simplebehaviortree.Composite;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.NodeStatus;
import jsettlers.algorithms.simplebehaviortree.Tick;

import static jsettlers.algorithms.simplebehaviortree.NodeStatus.*;

public class DynamicGuardSelector<T> extends Composite<T> {

	public DynamicGuardSelector(Guard<T>[] childrenGuards) {
		super(childrenGuards);
	}

	@Override
	protected NodeStatus onTick(Tick<T> tick) {
		for(Node<T> child : children) {
			Guard<T> guard = (Guard<T>)child;

			if(guard.checkGuardCondition(tick)) {
				Node<T> runningChild = tick.getProperty(getId());
				if(runningChild != null && runningChild != guard) runningChild.close(tick);

				NodeStatus returnStatus = guard.execute(tick);

				switch (returnStatus) {
					case RUNNING:
						tick.setProperty(getId(), guard);
						return RUNNING;
					case SUCCESS:
						return SUCCESS;
						// continue with next node
					case FAILURE:
						break;
				}
			}
		}

		return NodeStatus.FAILURE;
	}

	@Override
	protected void onOpen(Tick<T> tick) {
		tick.setProperty(getId(), null);
	}

	@Override
	protected void onClose(Tick<T> tick) {
		Node<T> runningChild = tick.getProperty(getId());
		if(runningChild != null) runningChild.close(tick);
	}
}
