package jsettlers.main.swing.menu.multiplayer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import jsettlers.graphics.localization.Labels;
import jsettlers.main.MultiplayerConnector;
import jsettlers.main.swing.JSettlersFrame;
import jsettlers.main.swing.settings.SettingsManager;
import jsettlers.network.client.IClientConnection;
import jsettlers.network.infrastructure.log.ConsoleConsumerLogger;
import jsettlers.network.infrastructure.log.Logger;
import jsettlers.network.server.lan.LanServerAddressBroadcastListener;

import java.awt.BorderLayout;
import java.awt.GridLayout;

public class ConnectLanPanel extends ServerConnectionPanel {

	private IClientConnection connection;

	private final Logger log;
	private final StringBuffer logText = new StringBuffer();

	private final JTextPane logPane = new JTextPane();

	public ConnectLanPanel(JSettlersFrame settlersFrame) {
		super(settlersFrame, null);

		JPanel overviewPanel = new JPanel();
		overviewPanel.setLayout(new GridLayout(2, 1));
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new GridLayout(1, 2));

		log = new ConsoleConsumerLogger("jsettlers-swing-lan", line -> {
			logText.append(line);
			synchronized (ConnectLanPanel.this) {
				logPane.setText(logText.toString());
			}
		});

		JPanel connectPanel = new JPanel();
		topPanel.add(connectPanel);

		JPanel startServerPanel = new JPanel();
		topPanel.add(startServerPanel);

		overviewPanel.add(topPanel);

		logPane.setEditable(false);
		overviewPanel.add(new JScrollPane(logPane));

		root.addTab(Labels.getString("multiplayer-lan-settings"), overviewPanel);

		openConnection(null);
	}

	private void openConnection(String host) {
		if(connection != null) {
			connection.action(IClientConnection.EClientAction.CLOSE, null);
			connection = null;
		}

		connection = new MultiplayerConnector(host,
				SettingsManager.getInstance().getUUID(),
				SettingsManager.getInstance().getUserName(),
				log);
	}

	@Override
	protected IClientConnection getConnection() {
		return connection;
	}

	@Override
	protected int updateBefore(IClientConnection connection, int i) {
		return 1;
	}

	@Override
	protected int updateAfter(IClientConnection connection, int i) {
		return 0;
	}
}
