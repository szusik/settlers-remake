/*
 * Copyright (c) 2026
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
package jsettlers.network.server.match.lockstep;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import jsettlers.network.NetworkConstants.ENetworkKey;
import jsettlers.network.server.packets.ServersideTaskPacket;

/**
 * Every client sends its tasks on its own thread, while the timer of the match takes them to send them to all clients.
 */
public class TaskCollectingListenerTest {
	private static final int SENDERS = 4;
	private static final int TASKS_PER_SENDER = 200000;

	@Test
	public void testNoTaskGetsLostOrChangesAfterBeingTaken() throws InterruptedException {
		TaskCollectingListener listener = new TaskCollectingListener();

		List<Thread> senders = new ArrayList<>();
		for (int i = 0; i < SENDERS; i++) {
			Thread sender = new Thread(() -> {
				for (int task = 0; task < TASKS_PER_SENDER; task++) {
					listener.receivePacket(ENetworkKey.SYNCHRONOUS_TASK, new ServersideTaskPacket(new byte[0]));
				}
			});
			senders.add(sender);
			sender.start();
		}

		int taken = 0;
		while (senders.stream().anyMatch(Thread::isAlive)) {
			List<ServersideTaskPacket> tasks = listener.getAndResetTasks();
			int sizeWhenTaken = tasks.size();
			Thread.yield();
			assertEquals("a taken list must not change, because it is sent to each client separately", sizeWhenTaken, tasks.size());
			taken += sizeWhenTaken;
		}
		for (Thread sender : senders) {
			sender.join();
		}
		taken += listener.getAndResetTasks().size();

		assertEquals(SENDERS * TASKS_PER_SENDER, taken);
	}
}
