package jsettlers.main.swing.menu.multiplayer.lan;


import jsettlers.network.NetworkConstants;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;
import java.net.InetAddress;

public class ServerListCellRenderer extends DefaultListCellRenderer {

	private final AgingServerList serverList;

	public ServerListCellRenderer(AgingServerList serverList) {
		this.serverList = serverList;
	}

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
		long since = (System.nanoTime() - serverList.getLastReceiveTime(value))/(1000L*1000L*1000L);
		if(since > NetworkConstants.Server.MAX_BROADCAST_DELAY/1000) {
			timeString = " / (" + since + "s ago)";
		}

		setText(ipString + timeString);
		return this;
	}
}