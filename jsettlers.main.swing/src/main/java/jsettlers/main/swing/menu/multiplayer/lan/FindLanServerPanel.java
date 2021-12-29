package jsettlers.main.swing.menu.multiplayer.lan;

import go.graphics.swing.util.MutableListModel;
import jsettlers.network.NetworkConstants;
import jsettlers.network.server.lan.LanServerAddressBroadcastListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FindLanServerPanel extends JPanel {

	private final Consumer<String> openConnection;
	private final AgingServerListener listener = new AgingServerListener();
	private final JList<Map.Entry<InetAddress, Long>> connectionList;
	private final MutableListModel<Map.Entry<InetAddress, Long>> serverListModel = new MutableListModel<>();

	public FindLanServerPanel(Consumer<String> openConnection) {
		this.openConnection = openConnection;

		LanServerAddressBroadcastListener broadcastListener = new LanServerAddressBroadcastListener(listener);
		broadcastListener.start();

		connectionList = new JList<>(serverListModel);
		connectionList.setBorder(new LineBorder(Color.RED));
		connectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		connectionList.setCellRenderer(new ServerEntryRenderer());

		setLayout(new BorderLayout());
		add(new JScrollPane(connectionList), BorderLayout.CENTER);
	}

	public void update() {
		final long removeTime = System.nanoTime() - NetworkConstants.Server.MAX_BROADCAST_DELAY*1000L*1000L;
		synchronized (listener.servers) {
			listener.servers.entrySet().removeIf(entry -> entry.getValue() <= removeTime);
		}

		serverListModel.setData(listener.servers.entrySet(), Map.Entry.comparingByKey(Comparator.comparing(InetAddress::getHostAddress)));

		invalidate();
	}

	private class AgingServerListener implements LanServerAddressBroadcastListener.ILanServerAddressListener {

		private final Map<InetAddress, Long> servers = Collections.synchronizedMap(new HashMap<>());

		@Override
		public boolean foundServerAddress(InetAddress address) {
			servers.put(address, System.nanoTime());

			SwingUtilities.invokeLater(FindLanServerPanel.this::update);
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

			Map.Entry<InetAddress, Long> value = (Map.Entry<InetAddress, Long>) val;

			String hostAddr = value.getKey().getHostAddress();
			String hostname = value.getKey().getCanonicalHostName();

			String ipString = hostAddr;
			if(!hostAddr.equals(hostname)) {
				ipString = hostAddr + "(" + hostname + ")";
			}

			String timeString = "";
			long since = (System.nanoTime() - value.getValue())/(1000L*1000L*1000L);
			if(since > NetworkConstants.Server.MAX_BROADCAST_DELAY/1000) {
				timeString = " / (" + since + "s ago)";
			}

			setText(ipString + timeString);
			return this;
		}
	}
}
