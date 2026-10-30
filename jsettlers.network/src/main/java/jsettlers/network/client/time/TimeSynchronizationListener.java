/*******************************************************************************
 * Copyright (c) 2015
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
 *******************************************************************************/
package jsettlers.network.client.time;

import java.io.IOException;

import jsettlers.network.NetworkConstants;
import jsettlers.network.NetworkConstants.Client;
import jsettlers.network.NetworkConstants.ENetworkKey;
import jsettlers.network.common.packets.TimeSyncPacket;
import jsettlers.network.infrastructure.channel.GenericDeserializer;
import jsettlers.network.infrastructure.channel.listeners.PacketChannelListener;
import jsettlers.network.infrastructure.channel.ping.IRoundTripTimeSupplier;

/**
 * 
 * @author Andreas Eberle
 * 
 */
public class TimeSynchronizationListener extends PacketChannelListener<TimeSyncPacket> {
	private static final long CLOCK_OFFSET_WINDOW_MS = 1000;

	private final IRoundTripTimeSupplier rttSupplier;
	private final ISynchronizableClock clock;

	private long windowStartMs = 0;
	private int windowOffsetMs = 0;
	private int previousWindowOffsetMs = 0;

	public TimeSynchronizationListener(IRoundTripTimeSupplier rttSupplier, ISynchronizableClock clock) {
		super(NetworkConstants.ENetworkKey.TIME_SYNC, new GenericDeserializer<>(TimeSyncPacket.class));
		this.rttSupplier = rttSupplier;
		this.clock = clock;
	}

	@Override
	protected void receivePacket(ENetworkKey key, TimeSyncPacket packet) throws IOException {
		int expectedRemoteTime = packet.getTime() + rttSupplier.getRoundTripTime().getRtt() / 2;
		int localTime = clock.getTime();

		int deltaTime = localTime - expectedRemoteTime;
		recordClockOffset(deltaTime);

		if (deltaTime > Client.TIME_SYNC_TOLERATED_DIFFERENCE) {
			clock.pauseClockFor((int) (deltaTime * Client.TIME_SYNC_APPROACH_FACTOR));
		}
	}

	/**
	 * @return The largest clock offset to any other player seen in the last second. Packets do not identify their sender, so this can't be broken down per player.
	 */
	public synchronized int getClockOffsetInMs() {
		advanceWindow();
		return Math.abs(windowOffsetMs) > Math.abs(previousWindowOffsetMs) ? windowOffsetMs : previousWindowOffsetMs;
	}

	private synchronized void recordClockOffset(int offsetMs) {
		advanceWindow();
		if (Math.abs(offsetMs) > Math.abs(windowOffsetMs)) {
			windowOffsetMs = offsetMs;
		}
	}

	private void advanceWindow() {
		long now = System.currentTimeMillis();
		long elapsedMs = now - windowStartMs;
		if (elapsedMs >= CLOCK_OFFSET_WINDOW_MS) {
			previousWindowOffsetMs = elapsedMs < 2 * CLOCK_OFFSET_WINDOW_MS ? windowOffsetMs : 0;
			windowOffsetMs = 0;
			windowStartMs = now;
		}
	}

}
