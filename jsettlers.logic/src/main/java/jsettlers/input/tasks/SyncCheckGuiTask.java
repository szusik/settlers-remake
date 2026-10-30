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
package jsettlers.input.tasks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Publishes the state checksums one client calculated at a game time, so that the other clients can compare them with their own.
 */
public class SyncCheckGuiTask extends SimpleGuiTask {
	private int gameTime;
	private long randomChecksum;
	private long movablesChecksum;
	private String playerName;

	public SyncCheckGuiTask() {
	}

	public SyncCheckGuiTask(byte playerId, String playerName, int gameTime, long randomChecksum, long movablesChecksum) {
		super(EGuiAction.SYNC_CHECK, playerId);
		this.playerName = playerName;
		this.gameTime = gameTime;
		this.randomChecksum = randomChecksum;
		this.movablesChecksum = movablesChecksum;
	}

	public String getPlayerName() {
		return playerName;
	}

	public int getGameTime() {
		return gameTime;
	}

	public long getRandomChecksum() {
		return randomChecksum;
	}

	public long getMovablesChecksum() {
		return movablesChecksum;
	}

	@Override
	protected void serializeTask(DataOutputStream dos) throws IOException {
		super.serializeTask(dos);
		dos.writeUTF(playerName);
		dos.writeInt(gameTime);
		dos.writeLong(randomChecksum);
		dos.writeLong(movablesChecksum);
	}

	@Override
	protected void deserializeTask(DataInputStream dis) throws IOException {
		super.deserializeTask(dis);
		playerName = dis.readUTF();
		gameTime = dis.readInt();
		randomChecksum = dis.readLong();
		movablesChecksum = dis.readLong();
	}
}
