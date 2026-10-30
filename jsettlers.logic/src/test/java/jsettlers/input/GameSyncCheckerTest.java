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
package jsettlers.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jsettlers.input.tasks.SyncCheckGuiTask;
import jsettlers.logic.constants.ExtendedRandom;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.network.client.task.packets.TaskPacket;
import jsettlers.network.synchronic.timer.NetworkTimer;

public class GameSyncCheckerTest {
	private static final byte OWN_PLAYER = 0;
	private static final byte OTHER_PLAYER = 1;

	private final List<SyncCheckGuiTask> sentTasks = new ArrayList<>();
	private final GameSyncChecker checker = new GameSyncChecker(task -> sentTasks.add((SyncCheckGuiTask) task), OWN_PLAYER, "me");

	@Before
	public void setUp() {
		MatchConstants.init(new NetworkTimer(true), 1);
		MatchConstants.clock().setTime(10000);
	}

	@After
	public void tearDown() {
		MatchConstants.clearState();
	}

	@Test
	public void testEqualStateIsNotReported() {
		checker.timerEvent();

		checker.receiveReport(reportOf(sentTasks.get(0), 0, 0));

		assertFalse(checker.hasDiverged());
		assertEquals(10000, checker.getLastCheckGameTimeInMs());
	}

	@Test
	public void testDifferentRandomIsReported() {
		checker.timerEvent();

		checker.receiveReport(reportOf(sentTasks.get(0), 1, 0));

		assertDivergedAt(10000);
	}

	@Test
	public void testDifferentMovablesAreReported() {
		checker.timerEvent();

		checker.receiveReport(reportOf(sentTasks.get(0), 0, 1));

		assertDivergedAt(10000);
	}

	@Test
	public void testReportForFutureGameTimeIsComparedLater() {
		checker.receiveReport(new SyncCheckGuiTask(OTHER_PLAYER, "other", 10000, 42, 42));
		assertFalse(checker.hasDiverged());

		checker.timerEvent();

		assertDivergedAt(10000);
	}

	@Test
	public void testOwnReportIsIgnored() {
		checker.receiveReport(new SyncCheckGuiTask(OWN_PLAYER, "me", 10000, 42, 42));

		checker.timerEvent();

		assertFalse(checker.hasDiverged());
		assertNull(checker.getDivergedPlayerName());
	}

	@Test
	public void testRandomChecksumDependsOnDraws() {
		ExtendedRandom first = new ExtendedRandom(1);
		ExtendedRandom second = new ExtendedRandom(1);
		first.nextInt();
		second.nextInt();
		assertEquals(first.getDrawChecksum(), second.getDrawChecksum());

		first.nextInt();
		assertTrue(first.getDrawChecksum() != second.getDrawChecksum());
	}

	@Test
	public void testTaskSerialization() throws IOException {
		SyncCheckGuiTask task = new SyncCheckGuiTask(OTHER_PLAYER, "other", 10000, -1, Long.MAX_VALUE);

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		task.serialize(new DataOutputStream(bytes));
		SyncCheckGuiTask read = (SyncCheckGuiTask) TaskPacket.DEFAULT_DESERIALIZER.deserialize(null, new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

		assertEquals("other", read.getPlayerName());
		assertEquals(OTHER_PLAYER, read.getPlayerId());
		assertEquals(10000, read.getGameTime());
		assertEquals(-1, read.getRandomChecksum());
		assertEquals(Long.MAX_VALUE, read.getMovablesChecksum());
	}

	private void assertDivergedAt(int gameTime) {
		assertTrue(checker.hasDiverged());
		assertEquals("other", checker.getDivergedPlayerName());
		assertEquals(gameTime, checker.getDivergenceGameTimeInMs());
	}

	private static SyncCheckGuiTask reportOf(SyncCheckGuiTask own, long randomDelta, long movablesDelta) {
		return new SyncCheckGuiTask(OTHER_PLAYER, "other", own.getGameTime(), own.getRandomChecksum() + randomDelta, own.getMovablesChecksum() + movablesDelta);
	}
}
