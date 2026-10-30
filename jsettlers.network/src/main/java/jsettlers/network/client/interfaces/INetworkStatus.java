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
package jsettlers.network.client.interfaces;

import java.util.List;

/**
 * Read only view on the state of a running network match. All methods may be called from any thread.
 * <p>
 * Every participant of a match is a client of the same server, so this information is available on all of them. The server does not distribute the connection quality of the other players, so the
 * timing values only describe this client.
 */
public interface INetworkStatus {
	/**
	 * @return True if this client is still part of the running match.
	 */
	boolean isConnected();

	/**
	 * @return The id of the match. It is the same on all clients.
	 */
	String getMatchId();

	/**
	 * @return The name of the player on this client.
	 */
	String getPlayerName();

	/**
	 * @return The names of the other players that are currently connected to the match.
	 */
	List<String> getOtherPlayerNames();

	/**
	 * @return The round trip time to the server in milliseconds.
	 */
	int getRoundTripTimeInMs();

	/**
	 * @return The averaged jitter of the round trip time in milliseconds.
	 */
	int getJitterInMs();

	/**
	 * @return The game time in milliseconds this client may still advance without further data from the server.
	 */
	int getLockstepLeadInMs();

	/**
	 * @return True if the game currently stands still, because the next lockstep has not been received.
	 */
	boolean isWaitingForLockstep();

	/**
	 * @return The number of times the game had to wait for a lockstep.
	 */
	int getLockstepWaitCount();

	/**
	 * @return The total time in milliseconds the game had to wait for locksteps.
	 */
	long getLockstepWaitTimeInMs();

	/**
	 * @return The largest recent clock difference to any other player in milliseconds. Positive means this client is ahead, negative means it is behind.
	 */
	int getClockOffsetInMs();
}
