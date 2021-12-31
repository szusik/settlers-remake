package jsettlers.main.swing.menu.multiplayer.lan;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;

import jsettlers.graphics.localization.Labels;
import jsettlers.main.MultiplayerConnector;
import jsettlers.main.swing.JSettlersFrame;
import jsettlers.main.swing.lookandfeel.ELFStyle;
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

	private final JButton toggleClientButton;
	private final StartLanServerButton toggleServerButton;
	private final JList<InetAddress> localServerList;
	private IClientConnection connection;

	private final Logger logger;
	private final StringBuffer logText = new StringBuffer();

	private final JTextPane logPane = new JTextPane();
	private final AgingServerList serverList;
	private final JLabel statusLabel;
	private InetAddress serverIP = null;

	private ELANState state = ELANState.NONE;

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


		serverList = new AgingServerList();
		LanServerAddressBroadcastListener broadcastListener = new LanServerAddressBroadcastListener(serverList);
		broadcastListener.start();
		localServerList = serverList.createSwingComponent();
		localServerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		localServerList.addListSelectionListener(e -> updateGUIState());
		topPanel.add(localServerList);


		toggleClientButton = new JButton();
		toggleClientButton.putClientProperty(ELFStyle.KEY, ELFStyle.BUTTON_STONE);
		toggleClientButton.addActionListener(e -> setServerIP(localServerList.getSelectedValue()));
		topPanel.add(toggleClientButton);

		toggleServerButton = new StartLanServerButton(logger, this::setServerActive);
		topPanel.add(toggleServerButton);

		statusLabel = new JLabel();
		topPanel.add(statusLabel);

		overviewPanel.add(topPanel);

		logPane.setEditable(false);
		overviewPanel.add(new JScrollPane(logPane));

		closeConnection();

		root.addTab(Labels.getString("multiplayer-lan-settings"), overviewPanel);

		updateGUIState();
	}

	private void setServerIP(InetAddress serverIP) {
		if(state == ELANState.CLIENT) {
			this.serverIP = null;
			setState(ELANState.CLIENT, ELANState.NONE);
			closeConnection();
		} else {
			assert serverIP != null;
			this.serverIP = serverIP;
			setState(ELANState.NONE, ELANState.CLIENT);

			openConnection(serverIP.getHostAddress());
		}
	}

	private void setServerActive(boolean active) {
		if(active) {
			setState(ELANState.NONE, ELANState.HOST);
			openConnection(null);
		} else {
			setState(ELANState.HOST, ELANState.NONE);
			closeConnection();
		}
	}

	private void setState(ELANState prev, ELANState next) {
		assert state == prev;

		state = next;
		updateGUIState();
	}

	private void updateGUIState() {
		boolean serverEnabled = state != ELANState.CLIENT;
		boolean clientEnabled = state != ELANState.HOST;
		boolean toggleClientEnabled = state == ELANState.NONE && localServerList.getSelectedValue() != null || state == ELANState.CLIENT;

		localServerList.setEnabled(clientEnabled);
		if(clientEnabled) {
			toggleClientButton.setText(Labels.getString("multiplayer-lan-disconnect"));
		} else {
			toggleClientButton.setText(Labels.getString("multiplayer-lan-connect"));
		}
		toggleClientButton.setEnabled(toggleClientEnabled);
		toggleServerButton.setEnabled(serverEnabled);

		switch (state) {
			case CLIENT:
				statusLabel.setText("Connected to " + serverIP.getHostAddress());
			case HOST:
				statusLabel.setText("Hosting server (%d players connected)");
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
		serverList.update();

		if(state != ELANState.NONE && connection.hasConnectionFailed() && connection.isConnected()) {
			closeConnection();
			setState(state, ELANState.NONE);
		}
		return 1;
	}

	@Override
	protected int updateAfter(IClientConnection connection, int i) {
		return 0;
	}

	private enum ELANState {
		NONE,
		HOST,
		CLIENT
	}
}
