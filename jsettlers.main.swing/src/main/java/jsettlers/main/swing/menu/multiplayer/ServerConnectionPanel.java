/*******************************************************************************
 * Copyright (c) 2020
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
package jsettlers.main.swing.menu.multiplayer;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import jsettlers.graphics.localization.Labels;
import jsettlers.main.swing.settings.ServerEntry;
import jsettlers.network.client.IClientConnection;

public class ServerConnectionPanel extends JPanel {

	private ServerEntry entry;
	private JTabbedPane root = new JTabbedPane();
	private RemoteMapDirectoryPanel maps = null;

	public ServerConnectionPanel(ServerEntry entry, Runnable leave) {
		this.entry = entry;

		JTextArea logText = new JTextArea();
		logText.setEditable(false);
		entry.setConnectionLogListener(logText::setText);
		root.addTab(Labels.getString("multiplayer-log-title"), new JScrollPane(logText));
		root.addTab(Labels.getString("multiplayer-log-settings"), new EditServerEntryPanel(leave, () -> root.setSelectedIndex(0), entry));

		setLayout(new BorderLayout());
		add(root, BorderLayout.CENTER);
	}

	public void update() {
		IClientConnection connection = entry.getConnection();
		boolean conMaps = connection.getMaps("/") != null;
		if(!conMaps && maps != null) {
			root.removeTabAt(1);
			maps = null;
		} else if(conMaps && maps == null) {
			root.insertTab(Labels.getString("multiplayer-mapslist-title"), null, maps = new RemoteMapDirectoryPanel(connection),  null, 1);
		}

		if(maps != null) maps.update();
	}
}
