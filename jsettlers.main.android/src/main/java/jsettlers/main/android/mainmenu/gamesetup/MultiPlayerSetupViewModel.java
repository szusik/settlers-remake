package jsettlers.main.android.mainmenu.gamesetup;

import java.util.Iterator;
import java.util.List;

import jsettlers.common.menu.IJoinPhaseMultiplayerGameConnector;
import jsettlers.common.menu.IMultiplayerListener;
import jsettlers.common.menu.IMultiplayerPlayer;
import jsettlers.common.menu.IMultiplayerSlot;
import jsettlers.common.menu.IStartingGame;
import jsettlers.common.utils.collections.ChangingList;
import jsettlers.common.utils.collections.IChangingListListener;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.main.android.core.AndroidPreferences;
import jsettlers.main.android.core.GameStarter;
import jsettlers.main.android.mainmenu.gamesetup.playeritem.Civilisation;
import jsettlers.main.android.mainmenu.gamesetup.playeritem.PlayerSlotPresenter;
import jsettlers.main.android.mainmenu.gamesetup.playeritem.PlayerType;
import jsettlers.main.android.mainmenu.gamesetup.playeritem.ReadyListener;
import jsettlers.main.android.mainmenu.gamesetup.playeritem.StartPosition;
import jsettlers.main.android.mainmenu.gamesetup.playeritem.Team;

/**
 * Created by Tom Pratt on 07/10/2017.
 */

public class MultiPlayerSetupViewModel extends MapSetupViewModel implements IMultiplayerListener, IChangingListListener<IMultiplayerSlot>, ReadyListener {

	private final GameStarter gameStarter;
	private final AndroidPreferences androidPreferences;
	protected final IJoinPhaseMultiplayerGameConnector connector;
	private final MapLoader mapLoader;

	public MultiPlayerSetupViewModel(GameStarter gameStarter, AndroidPreferences androidPreferences, IJoinPhaseMultiplayerGameConnector connector, MapLoader mapLoader) {
		super(gameStarter, mapLoader);
		this.gameStarter = gameStarter;
		this.androidPreferences = androidPreferences;
		this.connector = connector;
		this.mapLoader = mapLoader;

		connector.setMultiplayerListener(this);
		connector.getSlots().setListener(this);

		for (PlayerSlotPresenter playerSlotPresenter : playerSlotPresenters) {
			setAllSlotPlayerTypes(playerSlotPresenter);
		}

		updateSlots();
	}

	@Override
	public void startGame() {
		connector.startGame();
	}

	/**
	 * IMultiplayerListener implementation
	 */
	@Override
	public void gameAborted() {
		gameStarter.setJoinPhaseMultiPlayerConnector(null);
		// TODO pop
	}

	@Override
	public void gameIsStarting(IStartingGame game) {
		gameStarter.setJoinPhaseMultiPlayerConnector(null);
		gameStarter.setStartingGame(game);
		showMapEvent.postValue(null);
	}

	/**
	 * ChangingListListener implementation
	 */
	@Override
	public void listChanged(ChangingList<? extends IMultiplayerSlot> list) {
		updateSlots();
		if (playerSlots.getValue() != null) {
			playerSlots.postValue(playerSlots.getValue());
		}
		// updateViewItems(); // trigger a notify data set changed for now. Probably want to update the view more dynamically at some point
	}

	/**
	 * ReadyListener implementation
	 */
	@Override
	public void readyChanged(boolean ready) {
		connector.setReady(ready);
	}

	protected void setAllPlayerSlotsEnabled(boolean enabled) {
		for (PlayerSlotPresenter playerSlotPresenter : playerSlotPresenters) {
			playerSlotPresenter.setControlsEnabled(enabled);
		}
	}

	private void updateSlots() {
		Iterator<IMultiplayerSlot> slotIter = connector.getSlots().getItems().iterator();

		for (int i = 0; i < playerSlotPresenters.size() && slotIter.hasNext(); i++) {
			IMultiplayerSlot remoteSlot = slotIter.next();

			PlayerSlotPresenter playerSlotPresenter = playerSlotPresenters.get(i);

			IMultiplayerPlayer player = remoteSlot.getPlayer();
			if (player != null) {
				playerSlotPresenter.setName(player.getName());
				playerSlotPresenter.setReady(player.isReady());
				playerSlotPresenter.setShowReadyControl(true);

				boolean isMe = player.getId().equals(androidPreferences.getPlayerId());

				playerSlotPresenter.setReadyListener(isMe ? this : null);
			} else {
				playerSlotPresenter.setName("Computer " + i);
				playerSlotPresenter.setShowReadyControl(false);
				playerSlotPresenter.setReadyListener(null);
			}

			playerSlotPresenter.setPlayerType(new PlayerType(remoteSlot.getType()));
			playerSlotPresenter.setCivilisation(new Civilisation(remoteSlot.getCivilisation()));
			playerSlotPresenter.setTeam(new Team(remoteSlot.getTeam()));
			playerSlotPresenter.setStartPosition(new StartPosition(remoteSlot.getPosition()));
		}
	}

	protected abstract boolean amITheHost();
}
