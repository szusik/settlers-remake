package jsettlers.main.swing.menu.multiplayer.lan;

import jsettlers.graphics.localization.Labels;
import jsettlers.network.NetworkConstants;
import jsettlers.network.server.lan.LanServerAddressBroadcastListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public class FindLanServerPanel extends JPanel {

	private final Consumer<String> openConnection;
	private final AgingServerListener listener = new AgingServerListener();
	private final DefaultListModel<InetAddress> serverListModel = new DefaultListModel<>();

	public FindLanServerPanel(Consumer<String> openConnection) {
		this.openConnection = openConnection;

		setBorder(new TitledBorder(Labels.getString("multiplayer-lan-find-server")));
		LanServerAddressBroadcastListener broadcastListener = new LanServerAddressBroadcastListener(listener);
		broadcastListener.start();

		JList<InetAddress> connectionList = new JList<>(serverListModel);
		connectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		connectionList.setCellRenderer(new ServerEntryRenderer());

		setLayout(new BorderLayout());
		add(new JScrollPane(connectionList), BorderLayout.CENTER);
	}

	public void update() {
		final long removeTime = System.nanoTime() - 3*NetworkConstants.Server.MAX_BROADCAST_DELAY*1000L*1000L;
		synchronized (listener.servers) {
			Iterator<Map.Entry<InetAddress, Long>> iter = listener.servers.entrySet().iterator();
			while(iter.hasNext()) {
				Map.Entry<InetAddress, Long> entry = iter.next();
				if(entry.getValue() <= removeTime) {
					serverListModel.removeElement(entry.getKey());
					iter.remove();
				}
			}
		}
	}

	private class AgingServerListener implements LanServerAddressBroadcastListener.ILanServerAddressListener {

		private final Map<InetAddress, Long> servers = Collections.synchronizedMap(new HashMap<>());

		@Override
		public boolean foundServerAddress(InetAddress address) {
			Object prev = servers.put(address, System.nanoTime());

			if(prev == null) {
				SwingUtilities.invokeLater(() -> serverListModel.addElement(address));
			}
			return false;
		}
	}

	private class ServerEntryRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList list,
													  Object val,
													  int index,
													  boolean isSelected,
													  boolean cellHasFocus) {
			super.getListCellRendererComponent(list, val, index, isSelected, cellHasFocus);

			InetAddress value = (InetAddress) val;

			String hostAddr = value.getHostAddress();
			String hostname = value.getCanonicalHostName();

			String ipString = hostAddr;
			if(!hostAddr.equals(hostname)) {
				ipString = hostAddr + "(" + hostname + ")";
			}

			String timeString = "";
			long since = (System.nanoTime() - listener.servers.get(value))/(1000L*1000L*1000L);
			if(since > NetworkConstants.Server.MAX_BROADCAST_DELAY/1000) {
				timeString = " / (" + since + "s ago)";
			}

			setText(ipString + timeString);
			return this;
		}
	}
}
