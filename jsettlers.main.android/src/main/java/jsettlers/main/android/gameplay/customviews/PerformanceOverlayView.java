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

package jsettlers.main.android.gameplay.customviews;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.os.Process;
import android.os.SystemClock;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import jsettlers.input.GameSyncChecker;
import jsettlers.network.NetworkConstants;
import jsettlers.network.client.interfaces.INetworkStatus;

/**
 * Shows the RAM and CPU usage of this process and, during a network match, the connection state and the connected players. It updates itself while its window is visible.
 * <p>
 * The CPU value is relative to all cores, so 100% means that every core is fully used by the game. System wide values are not available to apps since Android 8.
 * <p>
 * The SYNC state describes the timing of the match. The STATE line shows whether the game state of the players still matches.
 */
public class PerformanceOverlayView extends AppCompatTextView {
	private static final long UPDATE_INTERVAL_MS = 1000;
	private static final String RESIDENT_MEMORY_PREFIX = "VmRSS:";

	private final Runnable updateRunnable = this::update;

	private boolean running = false;
	private long lastCpuTimeMs;
	private long lastWallTimeMs;
	private INetworkStatus networkStatus;
	private GameSyncChecker syncChecker;

	public PerformanceOverlayView(Context context) {
		super(context);
	}

	public PerformanceOverlayView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PerformanceOverlayView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * @param networkStatus
	 *            The status to display or null if this is not a network match.
	 * @param syncChecker
	 *            The checker of the game state or null if this is not a network match.
	 */
	public void setNetworkStatus(INetworkStatus networkStatus, GameSyncChecker syncChecker) {
		this.networkStatus = networkStatus;
		this.syncChecker = syncChecker;
	}

	@Override
	public void setVisibility(int visibility) {
		super.setVisibility(visibility);
		updateRunningState(getWindowVisibility());
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		super.onWindowVisibilityChanged(visibility);
		updateRunningState(visibility);
	}

	private void updateRunningState(int windowVisibility) {
		if (windowVisibility == VISIBLE && getVisibility() == VISIBLE) {
			start();
		} else {
			stop();
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		stop();
	}

	private void start() {
		if (running) {
			return;
		}
		running = true;
		lastCpuTimeMs = Process.getElapsedCpuTime();
		lastWallTimeMs = SystemClock.elapsedRealtime();
		postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
	}

	private void stop() {
		running = false;
		removeCallbacks(updateRunnable);
	}

	private void update() {
		long cpuTimeMs = Process.getElapsedCpuTime();
		long wallTimeMs = SystemClock.elapsedRealtime();
		float cpuPercent = 100f * (cpuTimeMs - lastCpuTimeMs) / (wallTimeMs - lastWallTimeMs) / Runtime.getRuntime().availableProcessors();
		lastCpuTimeMs = cpuTimeMs;
		lastWallTimeMs = wallTimeMs;

		String text = String.format(Locale.ROOT, "RAM %d MB  CPU %.0f%%", readResidentMemoryKb() / 1024, cpuPercent);
		if (networkStatus != null) {
			text += "\n" + formatNetworkStatus(networkStatus);
		}
		if (syncChecker != null) {
			text += "\n" + formatSyncCheck(syncChecker);
		}
		setText(text);
		postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
	}

	private static String formatNetworkStatus(INetworkStatus status) {
		List<String> otherPlayers = status.getOtherPlayerNames();

		StringBuilder text = new StringBuilder();
		text.append(String.format(Locale.ROOT, "NET %d ms \u00b1%d  lead %d ms\n", status.getRoundTripTimeInMs(), status.getJitterInMs(), status.getLockstepLeadInMs()));
		text.append(String.format(Locale.ROOT, "SYNC %s  clock %+d ms  stalls %d (%.1f s)\n", getSyncState(status), status.getClockOffsetInMs(), status.getLockstepWaitCount(),
				status.getLockstepWaitTimeInMs() / 1000f));
		text.append("Players: ").append(otherPlayers.size() + 1).append('\n');
		text.append(status.getPlayerName()).append(" (you)");
		for (String player : otherPlayers) {
			text.append('\n').append(player);
		}
		return text.toString();
	}

	private static String formatSyncCheck(GameSyncChecker checker) {
		if (checker.hasDiverged()) {
			return "STATE DIVERGED from " + checker.getDivergedPlayerName() + " at " + formatGameTime(checker.getDivergenceGameTimeInMs());
		} else if (checker.getLastCheckGameTimeInMs() < 0) {
			return "STATE not checked yet";
		}
		return "STATE OK at " + formatGameTime(checker.getLastCheckGameTimeInMs());
	}

	private static String formatGameTime(int gameTimeMs) {
		int seconds = gameTimeMs / 1000;
		return String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
	}

	private static String getSyncState(INetworkStatus status) {
		if (!status.isConnected()) {
			return "DISCONNECTED";
		} else if (status.isWaitingForLockstep()) {
			return "WAITING";
		} else if (Math.abs(status.getClockOffsetInMs()) > NetworkConstants.Client.TIME_SYNC_TOLERATED_DIFFERENCE) {
			return "ADJUSTING";
		}
		return "OK";
	}

	/**
	 * @return The physical memory of this process in KB, or 0 if it can't be read. In contrast to the Java heap, this includes native allocations.
	 */
	private static long readResidentMemoryKb() {
		try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/status"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith(RESIDENT_MEMORY_PREFIX)) {
					return Long.parseLong(line.substring(RESIDENT_MEMORY_PREFIX.length()).replace("kB", "").trim());
				}
			}
		} catch (IOException | NumberFormatException e) {
			e.printStackTrace();
		}
		return 0;
	}
}
