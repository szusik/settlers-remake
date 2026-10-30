package jsettlers.main.android.mainmenu.mappicker;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import jsettlers.common.menu.EProgressState;
import jsettlers.common.menu.IJoinPhaseMultiplayerGameConnector;
import jsettlers.common.menu.IJoiningGame;
import jsettlers.common.menu.IJoiningGameListener;
import jsettlers.common.menu.IMapDefinition;
import jsettlers.common.menu.IOpenMultiplayerGameInfo;
import jsettlers.common.utils.collections.ChangingList;
import jsettlers.graphics.localization.Labels;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.main.android.core.AndroidPreferences;
import jsettlers.main.android.core.GameStarter;
import jsettlers.main.android.core.events.SingleLiveEvent;

/**
 * Created by Tom Pratt on 06/10/2017.
 */

public class NewMultiPlayerPickerViewModel extends MapPickerViewModel implements IJoiningGameListener {

	private final GameStarter gameStarter;
	private final AndroidPreferences androidPreferences;

	private final SingleLiveEvent<String> mapSelectedEvent = new SingleLiveEvent<>();
	private final MutableLiveData<JoiningViewState> joiningState = new MutableLiveData<>();
	private final SingleLiveEvent<Void> connectionFailedEvent = new SingleLiveEvent<>();

	private IJoiningGame joiningGame;
	private IMapDefinition tempMapDefinition;

	public NewMultiPlayerPickerViewModel(GameStarter gameStarter, AndroidPreferences androidPreferences, ChangingList<? extends MapLoader> changingMaps) {
		super(gameStarter, changingMaps);
		this.gameStarter = gameStarter;
		this.androidPreferences = androidPreferences;
	}

	@Override
	public void selectMap(MapLoader map) {
		cancelJoining();
		tempMapDefinition = map;

		joiningGame = gameStarter.getMultiPlayerConnector().openNewMultiplayerGame(new IOpenMultiplayerGameInfo() {
			@Override
			public String getMatchName() {
				return androidPreferences.getPlayerName();
			}

			@Override
			public IMapDefinition getMapDefinition() {
				return map;
			}

			@Override
			public int getMaxPlayers() {
				return map.getMaxPlayers();
			}
		});

		joiningState.postValue(new JoiningViewState("Connecting to server...", 0));
		gameStarter.setJoiningGame(joiningGame);
		joiningGame.setListener(this);
	}

	@Override
	protected void abort() {
		super.abort();
		cancelJoining();
	}

	public LiveData<String> getMapSelectedEvent() {
		return mapSelectedEvent;
	}

	public LiveData<Void> getConnectionFailedEvent() {
		return connectionFailedEvent;
	}

	public LiveData<JoiningViewState> getJoiningState() {
		return joiningState;
	}

	private void cancelJoining() {
		if (joiningGame != null) {
			joiningGame.abort();
		}

		gameStarter.setJoiningGame(null);
		gameStarter.closeMultiPlayerConnector();
	}

	/**
	 * IJoiningGameListener imeplementation
	 */
	@Override
	public void joinProgressChanged(EProgressState state, float progress) {
		String stateString = Labels.getProgress(state);
		int progressPercentage = (int) (progress * 100);

		joiningState.postValue(new JoiningViewState(stateString, progressPercentage));
	}

	@Override
	public void joinFailed() {
		joiningGame = null; // this game has no connection it could leave
		gameStarter.setJoiningGame(null);
		gameStarter.closeMultiPlayerConnector();
		joiningState.postValue(null);
		connectionFailedEvent.postValue(null);
	}

	@Override
	public void gameJoined(IJoinPhaseMultiplayerGameConnector connector) {
		joiningGame.setListener(null);
		gameStarter.setJoiningGame(null);
		joiningState.postValue(null);

		gameStarter.setJoinPhaseMultiPlayerConnector(connector);
		mapSelectedEvent.postValue(tempMapDefinition.getMapId());
	}

	/**
	 * ViewModel factory
	 */
	public static class Factory implements ViewModelProvider.Factory {

		private final Activity activity;
		private final GameStarter gameStarter;
		private final boolean loadingSavegame;

		public Factory(Activity activity, boolean loadingSavegame) {
			this.activity = activity;
			this.loadingSavegame = loadingSavegame;
			gameStarter = (GameStarter) activity.getApplication();
		}

		@Override
		public <T extends ViewModel> T create(Class<T> modelClass) {
			if (modelClass == NewMultiPlayerPickerViewModel.class) {
				ChangingList<MapLoader> maps = loadingSavegame ? gameStarter.getMapList().getSavedMultiplayerMaps() : gameStarter.getMapList().getFreshMaps();
				return (T) new NewMultiPlayerPickerViewModel(gameStarter, new AndroidPreferences(activity), maps);
			}
			throw new RuntimeException("NewSinglePlayerPickerViewModel.Factory doesn't know how to create a: " + modelClass.toString());
		}
	}
}
