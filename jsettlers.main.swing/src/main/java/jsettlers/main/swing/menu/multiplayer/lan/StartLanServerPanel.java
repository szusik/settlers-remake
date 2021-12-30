package jsettlers.main.swing.menu.multiplayer.lan;

import jsettlers.network.infrastructure.log.Logger;
import jsettlers.network.server.GameServerThread;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.function.Consumer;

public class StartLanServerPanel extends JPanel {

	private GameServerThread serverHandle;
	private final Logger logger;
	private final Consumer<Boolean> serverActive;

	public StartLanServerPanel(Logger logger, Consumer<Boolean> serverActive) {
		this.serverActive = serverActive;
		this.logger = logger;
		setLayout(new FlowLayout());

		JToggleButton startServer = new JToggleButton("server active");
		startServer.addActionListener(e -> startServer.setSelected(setServerState(startServer.isSelected())));
		add(startServer);
	}

	private boolean setServerState(boolean active) {
		boolean serverState = serverHandle != null;
		if(active == serverState) return serverState;

		if(active) {
			try {
				serverHandle = new GameServerThread(true, logger);
				serverHandle.start();
			} catch (IOException e) {
				logger.error(e);
			}
		} else {
			serverHandle.shutdown();
			serverHandle = null;
		}

		boolean activeAfter = serverHandle != null;
		serverActive.accept(activeAfter);
		return activeAfter;
	}

	public void update() {

	}
}
