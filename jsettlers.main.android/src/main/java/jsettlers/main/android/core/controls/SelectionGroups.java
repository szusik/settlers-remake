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
package jsettlers.main.android.core.controls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import jsettlers.common.action.SelectMovablesAction;
import jsettlers.common.menu.ISelectionGroupStore;
import jsettlers.common.movable.IIDable;
import jsettlers.common.selectable.ESelectionType;
import jsettlers.common.selectable.ISelectable;
import jsettlers.common.selectable.ISelectionSet;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.ILogicMovable;

/**
 * Numbered sets of settlers the player saved to select them again later. A settler can be part of several groups and stays in a group when it changes its job, because it keeps its id. Selections are
 * not part of the synchronized game state, so this only affects the local player. The groups are stored in the savegame of the local player.
 */
public class SelectionGroups implements SelectionListener, DrawListener, ISelectionGroupStore {
	private final ActionControls actionControls;

	private final TreeMap<Integer, Set<Integer>> groups = new TreeMap<>();
	private final MutableLiveData<List<Integer>> groupNumbers = new MutableLiveData<>();
	private final MutableLiveData<Integer> nextGroupNumber = new MutableLiveData<>();
	private final MutableLiveData<Boolean> canSaveSelection = new MutableLiveData<>();
	private final MutableLiveData<Integer> activeGroupNumber = new MutableLiveData<>();

	private Integer requestedGroup = null;
	private Integer activeGroup = null;
	private ISelectionSet selection;

	SelectionGroups(ActionControls actionControls) {
		this.actionControls = actionControls;
		groupNumbers.postValue(new ArrayList<>());
		nextGroupNumber.postValue(1);
		canSaveSelection.postValue(false);
	}

	public LiveData<List<Integer>> getGroupNumbers() {
		return groupNumbers;
	}

	public LiveData<Integer> getNextGroupNumber() {
		return nextGroupNumber;
	}

	public LiveData<Boolean> getCanSaveSelection() {
		return canSaveSelection;
	}

	/**
	 * @return The number of the group the current selection came from or null if there is none.
	 */
	public LiveData<Integer> getActiveGroupNumber() {
		return activeGroupNumber;
	}

	public synchronized void removeActiveGroup() {
		if (activeGroup != null) {
			groups.remove(activeGroup);
			setActiveGroup(null);
			groupsChanged();
		}
	}

	public synchronized void saveSelection() {
		if (!isSaveable(selection)) {
			return;
		}

		Set<Integer> members = new HashSet<>();
		for (ISelectable selectable : selection) {
			if (selectable instanceof IIDable) {
				members.add(((IIDable) selectable).getID());
			}
		}

		if (!members.isEmpty()) {
			groups.put(findFreeNumber(), members);
			groupsChanged();
		}
	}

	public synchronized void select(int groupNumber) {
		Set<Integer> members = groups.get(groupNumber);
		if (members != null) {
			requestedGroup = groupNumber;
			actionControls.fireAction(new SelectMovablesAction(new HashSet<>(members)));
		}
	}

	@Override
	public synchronized Map<Integer, Set<Integer>> getSelectionGroups() {
		Map<Integer, Set<Integer>> copy = new HashMap<>();
		groups.forEach((number, members) -> copy.put(number, new HashSet<>(members)));
		return copy;
	}

	@Override
	public synchronized void restoreSelectionGroups(Map<Integer, Set<Integer>> savedGroups) {
		groups.clear();
		setActiveGroup(null);
		savedGroups.forEach((number, members) -> groups.put(number, new HashSet<>(members)));
		groupsChanged();
	}

	@Override
	public synchronized void selectionChanged(ISelectionSet selection) {
		this.selection = selection;
		canSaveSelection.postValue(isSaveable(selection));

		if (requestedGroup != null) {
			setActiveGroup(requestedGroup);
			requestedGroup = null;
		} else if (activeGroup != null && !isPartOfGroup(selection, groups.get(activeGroup))) {
			setActiveGroup(null); // a selection shrinks by itself when members die, so a smaller one still belongs to the group
		}
	}

	private static boolean isPartOfGroup(ISelectionSet selection, Set<Integer> members) {
		if (selection == null || selection.getSize() == 0 || members == null) {
			return false;
		}

		for (ISelectable selectable : selection) {
			if (!(selectable instanceof IIDable) || !members.contains(((IIDable) selectable).getID())) {
				return false;
			}
		}
		return true;
	}

	private void setActiveGroup(Integer groupNumber) {
		activeGroup = groupNumber;
		activeGroupNumber.postValue(groupNumber);
	}

	/**
	 * Regularly removes groups that have lost all of their members.
	 */
	@Override
	public synchronized void draw() {
		if (groups.isEmpty()) {
			return;
		}

		Set<Integer> aliveIds = new HashSet<>();
		for (ILogicMovable movable : MovableManager.getAllMovables()) {
			aliveIds.add(movable.getID());
		}

		if (groups.values().removeIf(members -> {
			members.retainAll(aliveIds);
			return members.isEmpty();
		})) {
			if (activeGroup != null && !groups.containsKey(activeGroup)) {
				setActiveGroup(null);
			}
			groupsChanged();
		}
	}

	private static boolean isSaveable(ISelectionSet selection) {
		return selection != null && selection.getSize() > 0 && selection.getSelectionType() != ESelectionType.BUILDING;
	}

	private int findFreeNumber() {
		int number = 1;
		while (groups.containsKey(number)) {
			number++;
		}
		return number;
	}

	private void groupsChanged() {
		groupNumbers.postValue(new ArrayList<>(groups.keySet()));
		nextGroupNumber.postValue(findFreeNumber());
	}
}
