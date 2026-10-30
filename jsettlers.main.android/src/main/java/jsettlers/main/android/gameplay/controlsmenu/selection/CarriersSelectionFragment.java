/*
 * Copyright (c) 2017
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

package jsettlers.main.android.gameplay.controlsmenu.selection;

import java.util.HashSet;
import java.util.Set;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.os.SystemClock;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import jsettlers.common.action.ConvertAction;
import jsettlers.common.action.SelectMovablesAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.map.MapContent;
import jsettlers.graphics.map.draw.ECommonLinkType;
import jsettlers.graphics.map.draw.ImageLinkMap;
import jsettlers.main.android.R;
import jsettlers.main.android.core.controls.ActionControls;
import jsettlers.main.android.core.controls.ControlsResolver;
import jsettlers.main.android.core.resources.OriginalImageProvider;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.ILogicMovable;

/**
 * Created by tompr on 13/01/2017.
 */
@EFragment(R.layout.menu_selection_carriers)
public class CarriersSelectionFragment extends SelectionFragment {
	private static final long DOUBLE_CLICK_TIME_MS = 400;

	public static CarriersSelectionFragment newInstance() {
		return new CarriersSelectionFragment_();
	}

	@ViewById(R.id.image_view_pioneer)
	ImageView pioneerImageView;
	@ViewById(R.id.image_view_geologist)
	ImageView geologistImageView;
	@ViewById(R.id.image_view_thief)
	ImageView thiefImageView;
	@ViewById(R.id.text_view_pioneer)
	TextView pioneerTextView;
	@ViewById(R.id.text_view_geologist)
	TextView geologistTextView;
	@ViewById(R.id.text_view_thief)
	TextView thiefTextView;

	private ActionControls actionControls;
	private MapContent mapContent;
	private IPlayer player;

	private EMovableType lastClickedType;
	private long lastClickTime;


	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		ControlsResolver controlsResolver = new ControlsResolver(getActivity());
		actionControls = controlsResolver.getActionControls();
		mapContent = controlsResolver.getMapContent();
		player = getSelection().get(0).getPlayer();

		ECivilisation civilisation = player.getCivilisation();

		OriginalImageProvider.get(ImageLinkMap.get(civilisation, ECommonLinkType.SETTLER_GUI, EMovableType.PIONEER)).setAsImage(pioneerImageView);
		OriginalImageProvider.get(ImageLinkMap.get(civilisation, ECommonLinkType.SETTLER_GUI, EMovableType.GEOLOGIST)).setAsImage(geologistImageView);
		OriginalImageProvider.get(ImageLinkMap.get(civilisation, ECommonLinkType.SETTLER_GUI, EMovableType.THIEF)).setAsImage(thiefImageView);

		pioneerTextView.setText(Labels.getName(EMovableType.PIONEER));
		geologistTextView.setText(Labels.getName(EMovableType.GEOLOGIST));
		thiefTextView.setText(Labels.getName(EMovableType.THIEF));
	}

	@Click(R.id.button_convert_one_pioneer)
	void convertOnePioneerClicked() {
		fireConvertAction(EMovableType.PIONEER, false);
	}

	@Click(R.id.button_convert_all_pioneer)
	void convertAllPioneerClicked() {
		fireConvertAction(EMovableType.PIONEER, true);
	}

	@Click(R.id.button_convert_one_geologist)
	void convertOneGeologistClicked() {
		fireConvertAction(EMovableType.GEOLOGIST, false);
	}

	@Click(R.id.button_convert_all_geologist)
	void convertAllGeologistClicked() {
		fireConvertAction(EMovableType.GEOLOGIST, true);
	}

	@Click(R.id.button_convert_one_thief)
	void convertOneThiefClicked() {
		fireConvertAction(EMovableType.THIEF, false);
	}

	@Click(R.id.button_convert_all_thief)
	void convertAllThiefClicked() {
		fireConvertAction(EMovableType.THIEF, true);
	}

	@Click(R.id.image_view_pioneer)
	void pioneerIconClicked() {
		typeIconClicked(EMovableType.PIONEER);
	}

	@Click(R.id.image_view_geologist)
	void geologistIconClicked() {
		typeIconClicked(EMovableType.GEOLOGIST);
	}

	@Click(R.id.image_view_thief)
	void thiefIconClicked() {
		typeIconClicked(EMovableType.THIEF);
	}

	private void typeIconClicked(EMovableType type) {
		long now = SystemClock.uptimeMillis();
		boolean doubleClick = type == lastClickedType && now - lastClickTime <= DOUBLE_CLICK_TIME_MS;

		lastClickedType = doubleClick ? null : type;
		lastClickTime = now;

		if (doubleClick) {
			selectVisible(type);
		}
	}

	private void selectVisible(EMovableType type) {
		Set<Integer> visibleIds = new HashSet<>();
		for (ILogicMovable movable : MovableManager.getAllMovables()) {
			ShortPoint2D position = movable.getPosition();
			if (movable.getMovableType() == type && movable.getPlayer().getPlayerId() == player.getPlayerId() && position != null && mapContent.isOnScreen(position)) {
				visibleIds.add(movable.getID());
			}
		}

		if (visibleIds.isEmpty()) {
			Toast.makeText(getContext(), R.string.none_of_type_visible, Toast.LENGTH_SHORT).show();
		} else {
			actionControls.fireAction(new SelectMovablesAction(visibleIds));
		}
	}

	private void fireConvertAction(EMovableType convertToType, boolean convertAll) {
		actionControls.fireAction(new ConvertAction(convertToType, convertAll ? Short.MAX_VALUE : (short) 1));
	}
}
