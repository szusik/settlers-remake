/*
 * Copyright (c) 2026
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
 */
package jsettlers.main.android.gameplay.controlsmenu.buildqueue;

import java.util.Comparator;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import jsettlers.graphics.map.MapContent;
import jsettlers.logic.buildings.Building;
import jsettlers.main.android.core.controls.ControlsResolver;
import jsettlers.main.android.core.controls.DrawControls;
import jsettlers.main.android.core.events.DrawEvents;
import jsettlers.main.android.gameplay.navigation.MenuNavigator;
import jsettlers.main.android.gameplay.navigation.MenuNavigatorProvider;

/**
 * Lists the buildings of the player that are under construction. High priority buildings get their materials first and stopped ones are not worked on, so they are listed first and last. Within a
 * priority, the buildings stay in the order they were placed in, which is also the order the workers for them get requested.
 */
public class BuildQueueViewModel extends ViewModel {
	private final MapContent mapContent;
	private final MenuNavigator menuNavigator;
	private final byte playerId;

	private final LiveData<BuildQueueItem[]> buildQueue;

	BuildQueueViewModel(DrawControls drawControls, MapContent mapContent, MenuNavigator menuNavigator, byte playerId) {
		this.mapContent = mapContent;
		this.menuNavigator = menuNavigator;
		this.playerId = playerId;

		buildQueue = Transformations.map(new DrawEvents(drawControls), x -> currentBuildQueue());
	}

	LiveData<BuildQueueItem[]> getBuildQueue() {
		return buildQueue;
	}

	void showBuilding(BuildQueueItem item) {
		mapContent.scrollTo(item.getPosition(), true);
		menuNavigator.dismissMenu();
	}

	private BuildQueueItem[] currentBuildQueue() {
		return Building.getAllBuildings().stream()
				.filter(building -> building.getPlayer().getPlayerId() == playerId && !building.isConstructionFinished())
				.map(BuildQueueItem::new)
				.sorted(Comparator.comparingInt((BuildQueueItem item) -> item.getPriority().getPriorityIndex()).reversed())
				.toArray(BuildQueueItem[]::new);
	}

	public static class Factory implements ViewModelProvider.Factory {
		private final ControlsResolver controlsResolver;
		private final MenuNavigator menuNavigator;

		public Factory(Activity activity) {
			controlsResolver = new ControlsResolver(activity);
			menuNavigator = ((MenuNavigatorProvider) activity).getMenuNavigator();
		}

		@Override
		public <T extends ViewModel> T create(Class<T> modelClass) {
			if (modelClass == BuildQueueViewModel.class) {
				return (T) new BuildQueueViewModel(controlsResolver.getDrawControls(), controlsResolver.getMapContent(), menuNavigator, controlsResolver.getPlayer().getPlayerId());
			}
			throw new RuntimeException("BuildQueueViewModel.Factory doesn't know how to create a: " + modelClass.toString());
		}
	}
}
