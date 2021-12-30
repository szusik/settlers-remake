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
package jsettlers.network.server.lan;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

import jsettlers.network.NetworkConstants;

/**
 * This thread broadcasts a small package over the network to inform LAN members of the server address
 * 
 * @author Andreas Eberle
 * 
 */
public final class LanServerBroadcastThread extends Thread {

	private final AtomicBoolean canceled = new AtomicBoolean(false);
	private DatagramSocket socket;

	public LanServerBroadcastThread() {
		super("LanServerBroadcastThread");
		super.setDaemon(true);
	}

	@Override
	public void run() {
		try {
			socket = new DatagramSocket();

			while (!canceled.get()) {
				try {
					Thread.sleep(NetworkConstants.Server.BROADCAST_DELAY);

					byte[] data = NetworkConstants.Server.BROADCAST_MESSAGE.getBytes();

					broadcast(NetworkConstants.Server.BROADCAST_PORT, socket, data);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} finally {
			if (socket != null)
				socket.close();
		}
	}

	private void broadcast(int udpPort, DatagramSocket socket, byte[] data) throws IOException {
		InetAddress dst6 = InetAddress.getByAddress(NetworkConstants.Server.MULTICAST_IP6);
		InetAddress dst4 = InetAddress.getByAddress(new byte[]{-1, -1, -1, -1});

		socket.send(new DatagramPacket(data, data.length, dst6, udpPort));
		socket.send(new DatagramPacket(data, data.length, dst4, udpPort));
	}

	public void shutdown() {
		canceled.set(true);
		socket.close();
		this.interrupt();
	}
}
