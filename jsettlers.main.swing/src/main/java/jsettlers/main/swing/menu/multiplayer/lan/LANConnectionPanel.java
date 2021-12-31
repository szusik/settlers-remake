package jsettlers.main.swing.menu.multiplayer.lan;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;

import jsettlers.graphics.localization.Labels;
import jsettlers.main.MultiplayerConnector;
import jsettlers.main.swing.JSettlersFrame;
import jsettlers.main.swing.menu.multiplayer.ServerConnectionPanel;
import jsettlers.main.swing.settings.SettingsManager;
import jsettlers.network.client.IClientConnection;
import jsettlers.network.client.NullConnection;
import jsettlers.network.infrastructure.log.ConsoleConsumerLogger;
import jsettlers.network.infrastructure.log.Logger;
import jsettlers.network.server.lan.LanServerAddressBroadcastListener;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.net.InetAddress;

public class LANConnectionPanel extends ServerConnectionPanel {

	private IClientConnection connection;

	private final Logger logger;
	private final StringBuffer logText = new StringBuffer();

	private final JTextPane logPane = new JTextPane();

	private final JList<InetAddress> localServerList;

	public LANConnectionPanel(JSettlersFrame settlersFrame) {
		super(settlersFrame, null);

		JPanel overviewPanel = new JPanel();
		overviewPanel.setLayout(new GridLayout(2, 1));
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new FlowLayout());

		logger = new ConsoleConsumerLogger("jsettlers-swing-lan", line -> {
			logText.append(line);
			synchronized (LANConnectionPanel.this) {
				logPane.setText(logText.toString());
			}
		});


		AgingServerList serverList = new AgingServerList();
		LanServerAddressBroadcastListener broadcastListener = new LanServerAddressBroadcastListener(serverList);
		broadcastListener.start();
		localServerList = serverList.createSwingComponent();
		localServerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		topPanel.add(localServerList);

		StartLanServerButton startServerPanel = new StartLanServerButton(logger, this::setServeActive);
		topPanel.add(startServerPanel);

		overviewPanel.add(topPanel);

		logPane.setEditable(false);
		overviewPanel.add(new JScrollPane(logPane));

		closeConnection();

		root.addTab(Labels.getString("multiplayer-lan-settings"), overviewPanel);
	}

	private void setServeActive(boolean active) {
		if(active) {
			openConnection(null);
		} else {
			closeConnection();
		}
	}

	private void openConnection(String host) {
		closeConnection();

		connection = new MultiplayerConnector(host,
				SettingsManager.getInstance().getUUID(),
				SettingsManager.getInstance().getUserName(),
				logger);
	}

	private void closeConnection() {
		if(connection != null) {
			connection.action(IClientConnection.EClientAction.CLOSE, null);
		}
		connection = new NullConnection();
	}

	@Override
	protected IClientConnection getConnection() {
		return connection;
	}

	@Override
	protected int updateBefore(IClientConnection connection, int i) {
		findServerPanel.update();
		return 1;
	}

	@Override
	protected int updateAfter(IClientConnection connection, int i) {
		return 0;
	}
}
