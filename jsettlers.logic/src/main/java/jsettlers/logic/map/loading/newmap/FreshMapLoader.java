/*******************************************************************************
 * Copyright (c) 2015
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.logic.map.loading.newmap;

import java.io.IOException;
import java.io.InputStream;

import jsettlers.common.CommonConstants;
import jsettlers.common.logging.MilliStopWatch;
import jsettlers.common.menu.UIState;
import jsettlers.input.PlayerState;
import jsettlers.logic.map.grid.MainGrid;
import jsettlers.logic.map.loading.EMapStartResources;
import jsettlers.logic.map.loading.data.IMapData;
import jsettlers.logic.map.loading.MapLoadException;
import jsettlers.logic.map.loading.list.IListedMap;
import jsettlers.logic.map.loading.original.OriginalMultiPlayerWinLoseHandler;
import jsettlers.logic.player.PlayerSetting;

/**
 * 
 * @author Andreas Eberle
 * 
 */
public class FreshMapLoader extends RemakeMapLoader {

	private FreshMapData data = null;

	public FreshMapLoader(IListedMap file, MapFileHeader header) {
		super(file, header);
	}

	@Override
	public MainGridWithUiSettings loadMainGrid(PlayerSetting[] playerSettings, EMapStartResources startResources) throws MapLoadException {
		MilliStopWatch watch = new MilliStopWatch();
		IMapData mapData = getMapData();
		watch.stop("Loading map data required");

		byte numberOfPlayers = (byte) getMaxPlayers();

		if (playerSettings == null || CommonConstants.ACTIVATE_ALL_PLAYERS) {
			playerSettings = new PlayerSetting[numberOfPlayers];
			for (int i = 0; i < numberOfPlayers; i++) {
				playerSettings[i] = new PlayerSetting((byte) i);
			}
		}

		MainGrid mainGrid = new MainGrid(getMapId(), getMapName(), mapData, playerSettings);

		new OriginalMultiPlayerWinLoseHandler(mainGrid).schedule();

		PlayerState[] playerStates = new PlayerState[numberOfPlayers];
		for (byte playerId = 0; playerId < numberOfPlayers; playerId++) {
			playerStates[playerId] = new PlayerState(playerId, new UIState(mapData.getStartPoint(playerId)));
		}

		return new MainGridWithUiSettings(mainGrid, playerStates);
	}

	@Override
	public IMapData getMapData() throws MapLoadException {
		if (data != null) {
			return data;
		}

		try (InputStream stream = super.getMapDataStream()) {
			data = new FreshMapData();
			FreshMapSerializer.deserialize(data, stream);
			return data;
		} catch (IOException ex) {
			throw new MapLoadException(ex);
		}
	}

}
