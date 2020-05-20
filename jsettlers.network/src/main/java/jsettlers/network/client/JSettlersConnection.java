package jsettlers.network.client;

import jsettlers.network.infrastructure.log.Logger;

public class JSettlersConnection implements IClientConnection {

	public JSettlersConnection(String address, String nickname, String uuid, Logger log) {

	}

	@Override
	public boolean hasConnectionFailed() {
		return false;
	}

	@Override
	public boolean isConnected() {
		return false;
	}

	@Override
	public RemoteMapDirectory getMaps(String directory) {
		return null;
	}

	@Override
	public long getDownloadProgress() {
		return 0;
	}

	@Override
	public long getDownloadSize() {
		return 0;
	}

	@Override
	public void action(EClientAction action, Object argument) {

	}
}
