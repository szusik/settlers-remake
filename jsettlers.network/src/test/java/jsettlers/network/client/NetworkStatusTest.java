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
package jsettlers.network.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.BooleanSupplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jsettlers.network.NetworkConstants;
import jsettlers.network.TestUtils;
import jsettlers.network.client.task.packets.SyncTasksPacket;
import jsettlers.network.common.packets.MapInfoPacket;
import jsettlers.network.infrastructure.channel.AsyncChannel;
import jsettlers.network.infrastructure.channel.Channel;
import jsettlers.network.server.ServerManager;
import jsettlers.network.server.db.inMemory.InMemoryDB;
import jsettlers.network.server.match.EPlayerState;
import jsettlers.network.synchronic.timer.NetworkTimer;

/**
 * Tests the network status of {@link NetworkClient}s that play a match on a real {@link ServerManager}.
 */
public class NetworkStatusTest {
	private static final long TIMEOUT_MS = 5000;

	private final InMemoryDB db = new InMemoryDB();
	private final ServerManager manager = new ServerManager(db);

	private final NetworkTimer timer1 = new NetworkTimer();
	private final NetworkTimer timer2 = new NetworkTimer();

	private Channel serverChannel1;
	private Channel serverChannel2;
	private NetworkClient client1;
	private NetworkClient client2;

	@Before
	public void setUp() throws IOException {
		manager.start();

		AsyncChannel[] channels = TestUtils.setUpAsyncLoopbackChannels();
		serverChannel1 = channels[1];
		client1 = new NetworkClient(channels[0], null, timer1);
		manager.identifyNewChannel(serverChannel1);

		channels = TestUtils.setUpAsyncLoopbackChannels();
		serverChannel2 = channels[1];
		client2 = new NetworkClient(channels[0], null, timer2);
		manager.identifyNewChannel(serverChannel2);
	}

	@After
	public void tearDown() {
		client1.close();
		serverChannel1.close();
		client2.close();
		serverChannel2.close();
	}

	@Test
	public void testClockReportsWaitingForLockstep() throws InterruptedException {
		NetworkTimer timer = new NetworkTimer();
		timer.scheduleSyncTasksPacket(new SyncTasksPacket(0, Collections.emptyList()));
		timer.startExecution();
		try {
			waitFor(timer::isWaitingForLockstep);
			assertEquals(0, timer.getLockstepWaitCount());

			timer.scheduleSyncTasksPacket(new SyncTasksPacket(1, Collections.emptyList()));
			waitFor(() -> timer.getLockstepWaitCount() > 0);
		} finally {
			timer.stopExecution();
		}
	}

	@Test
	public void testBothClientsSeeThePlayersAndLeavingPlayers() throws InterruptedException {
		startMatch();

		assertTrue(client1.isConnected());
		assertTrue(client2.isConnected());
		assertEquals("first", client1.getPlayerName());
		assertEquals("second", client2.getPlayerName());
		assertEquals(Arrays.asList("second"), client1.getOtherPlayerNames());
		assertEquals(Arrays.asList("first"), client2.getOtherPlayerNames());

		client2.close();
		serverChannel2.close();

		waitFor(() -> client1.getOtherPlayerNames().isEmpty());
		assertTrue(client1.isConnected());
		assertFalse(client2.isConnected());
	}

	@Test
	public void testRunningMatchReceivesLocksteps() throws InterruptedException {
		startMatch();

		waitFor(() -> timer1.getTime() > 1000 && timer2.getTime() > 1000);

		waitFor(() -> client1.getLockstepLeadInMs() > 0 || client2.getLockstepLeadInMs() > 0);
		assertTrue(client1.getRoundTripTimeInMs() >= 0);
	}

	@Test
	public void testClientNoticesClockDifferenceToOtherPlayer() throws InterruptedException {
		startMatch();
		waitFor(() -> timer1.getTime() > 500 && timer2.getTime() > 500);

		timer2.setTime(timer2.getTime() + 2000);

		waitFor(() -> client1.getClockOffsetInMs() < -NetworkConstants.Client.TIME_SYNC_TOLERATED_DIFFERENCE);
		waitFor(() -> client2.getClockOffsetInMs() > NetworkConstants.Client.TIME_SYNC_TOLERATED_DIFFERENCE);
	}

	@Test
	public void testSavedGameContinuesAtItsGameTime() throws InterruptedException {
		final int savedTime = 1800000;
		timer1.resumeAtTime(savedTime);
		timer2.resumeAtTime(savedTime);

		startMatch();
		waitFor(() -> timer1.getTime() > savedTime + 1000 && timer2.getTime() > savedTime + 1000);

		assertFalse(client1.isWaitingForLockstep() && client2.isWaitingForLockstep());
		assertTrue("the server must not run away from the clients", client1.getLockstepLeadInMs() < 2000);
	}

	private void startMatch() throws InterruptedException {
		client1.logIn("id1", "first", packet -> {
		});
		client2.logIn("id2", "second", packet -> {
		});
		waitFor(() -> client1.getState() == EPlayerState.LOGGED_IN && client2.getState() == EPlayerState.LOGGED_IN);

		client1.openNewMatch("match", 2, new MapInfoPacket("map", "map", "", "", 2), 1L, null, packet -> {
		}, null);
		waitFor(() -> client1.getState() == EPlayerState.IN_MATCH);

		client2.joinMatch(db.getJoinableMatches().get(0).getId(), null, packet -> {
		}, null);
		waitFor(() -> client2.getState() == EPlayerState.IN_MATCH && client1.getMatchInfo().getPlayers().length == 2);

		client1.setReadyState(true);
		client2.setReadyState(true);
		Thread.sleep(150);

		client1.startMatch();
		waitFor(() -> client1.isConnected() && client2.isConnected());
		timer1.startExecution();
		timer2.startExecution();
	}

	private static void waitFor(BooleanSupplier condition) throws InterruptedException {
		long deadline = System.currentTimeMillis() + TIMEOUT_MS;
		while (!condition.getAsBoolean()) {
			assertTrue("condition not reached in time", System.currentTimeMillis() < deadline);
			Thread.sleep(10);
		}
	}
}
