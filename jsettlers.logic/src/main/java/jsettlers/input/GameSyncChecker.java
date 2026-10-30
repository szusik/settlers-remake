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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import jsettlers.input.tasks.SyncCheckGuiTask;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.MovableManager;
import jsettlers.network.client.interfaces.ITaskScheduler;
import jsettlers.network.synchronic.timer.INetworkTimerable;

/**
 * Detects whether the game state of the human players diverged. Every client publishes checksums of its state at the same game times. They travel as normal tasks, so every client compares them with
 * its own values without any support by the server. Computer players have no client of their own, they are simulated by all human players.
 * <p>
 * Everything except the getters runs on the game clock thread.
 */
public class GameSyncChecker implements INetworkTimerable {
	public static final short CHECK_INTERVAL_MS = 10000;
	private static final int STORED_CHECKS = 32;

	private static final class Checksums {
		final long random;
		final long movables;

		Checksums(long random, long movables) {
			this.random = random;
			this.movables = movables;
		}
	}

	private final ITaskScheduler taskScheduler;
	private final byte playerId;
	private final String playerName;

	private final TreeMap<Integer, Checksums> ownChecksums = new TreeMap<>();
	private final TreeMap<Integer, List<SyncCheckGuiTask>> earlyReports = new TreeMap<>();

	private volatile int lastCheckGameTimeMs = -1;
	private volatile int divergenceGameTimeMs = -1;
	private volatile String divergedPlayerName = null;

	public GameSyncChecker(ITaskScheduler taskScheduler, byte playerId, String playerName) {
		this.taskScheduler = taskScheduler;
		this.playerId = playerId;
		this.playerName = playerName;
	}

	/**
	 * @return The game time of the last check or -1 if there hasn't been a check yet.
	 */
	public int getLastCheckGameTimeInMs() {
		return lastCheckGameTimeMs;
	}

	/**
	 * @return True if the game state differs from the state of another player. This never resets, because a diverged game can't recover by itself.
	 */
	public boolean hasDiverged() {
		return divergedPlayerName != null;
	}

	/**
	 * @return The name of the first player with a different game state or null if there is none.
	 */
	public String getDivergedPlayerName() {
		return divergedPlayerName;
	}

	/**
	 * @return The game time of the first check that found a difference or -1 if there is none.
	 */
	public int getDivergenceGameTimeInMs() {
		return divergenceGameTimeMs;
	}

	@Override
	public void timerEvent() {
		int gameTime = MatchConstants.clock().getTime();
		Checksums checksums = new Checksums(MatchConstants.random().getDrawChecksum(), MovableManager.calculateChecksum());

		ownChecksums.put(gameTime, checksums);
		if (ownChecksums.size() > STORED_CHECKS) {
			ownChecksums.pollFirstEntry();
		}
		lastCheckGameTimeMs = gameTime;

		taskScheduler.scheduleTask(new SyncCheckGuiTask(playerId, playerName, gameTime, checksums.random, checksums.movables));

		List<SyncCheckGuiTask> reports = earlyReports.remove(gameTime);
		if (reports != null) {
			reports.forEach(report -> compare(report, checksums));
		}
		earlyReports.headMap(gameTime).clear();
	}

	/**
	 * Called when the task of a client gets executed.
	 */
	public void receiveReport(SyncCheckGuiTask report) {
		if (report.getPlayerId() == playerId) {
			return;
		}

		Checksums checksums = ownChecksums.get(report.getGameTime());
		if (checksums != null) {
			compare(report, checksums);
		} else if (report.getGameTime() > lastCheckGameTimeMs) { // the other client reached this game time first
			earlyReports.computeIfAbsent(report.getGameTime(), time -> new ArrayList<>()).add(report);
		}
	}

	private void compare(SyncCheckGuiTask report, Checksums checksums) {
		boolean randomDiffers = report.getRandomChecksum() != checksums.random;
		boolean movablesDiffer = report.getMovablesChecksum() != checksums.movables;
		if (!randomDiffers && !movablesDiffer) {
			return;
		}

		System.err.println("Game state diverged from " + report.getPlayerName() + " at game time " + report.getGameTime() + ": random differs: " + randomDiffers + ", movables differ: " + movablesDiffer);
		if (divergedPlayerName == null) {
			divergenceGameTimeMs = report.getGameTime();
			divergedPlayerName = report.getPlayerName();
		}
	}
}
