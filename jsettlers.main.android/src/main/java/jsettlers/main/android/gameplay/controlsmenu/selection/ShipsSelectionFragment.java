/*
 * Copyright (c) 2017 - 2018
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

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.ViewsById;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import jsettlers.common.action.EActionType;
import jsettlers.common.images.EImageLinkType;
import jsettlers.common.images.ImageLink;
import jsettlers.common.images.OriginalImageLink;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.IGraphicsCargoShip;
import jsettlers.common.movable.IGraphicsFerry;
import jsettlers.common.movable.IGraphicsMovable;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.selectable.ISelectable;
import jsettlers.graphics.map.draw.ECommonLinkType;
import jsettlers.graphics.map.draw.ImageLinkMap;
import jsettlers.logic.constants.Constants;
import jsettlers.main.android.R;
import jsettlers.main.android.core.controls.ActionControls;
import jsettlers.main.android.core.controls.ControlsResolver;
import jsettlers.main.android.core.controls.DrawControls;
import jsettlers.main.android.core.controls.DrawListener;
import jsettlers.main.android.core.resources.OriginalImageProvider;

/**
 * Created by Rudolf Polzer
 */
@EFragment(R.layout.menu_selection_ships)
public class ShipsSelectionFragment extends SelectionFragment implements DrawListener {
	private static final OriginalImageLink FREE_SLOT = new OriginalImageLink(EImageLinkType.GUI, 3, 45, 0);

	private static final EMovableType[] shipTypes = new EMovableType[] {
			EMovableType.FERRY,
			EMovableType.CARGO_SHIP,
	};

	private static final ImageLink[] shipImageLinks = new ImageLink[] {
			ImageLink.fromName("original_14_GUI_272", 0),
			ImageLink.fromName("original_14_GUI_278", 0)
	};

	@ViewsById({ R.id.layout_ferries, R.id.layout_tradeShips })
	List<LinearLayout> shipLayouts;
	@ViewsById({ R.id.textView_ferriesCount, R.id.textView_tradeShipsCount })
	List<TextView> countTextViews;
	@ViewsById({ R.id.imageView_ferry, R.id.imageView_tradeShip })
	List<ImageView> shipImageViews;

	@ViewById(R.id.layout_passengers)
	LinearLayout passengersLayout;

	private ActionControls actionControls;
	private DrawControls drawControls;
	private List<List<EMovableType>> shownPassengers;
	private List<List<CargoStack>> shownCargo;

	public static ShipsSelectionFragment newInstance() {
		return ShipsSelectionFragment_.builder().build();
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		ControlsResolver controlsResolver = new ControlsResolver(getActivity());
		actionControls = controlsResolver.getActionControls();
		drawControls = controlsResolver.getDrawControls();

		for (int i = 0; i < shipTypes.length; i++) {
			EMovableType shipType = shipTypes[i];
			int count = getSelection().getMovableCount(shipType, null);

			if (count > 0) {
				OriginalImageProvider.get(shipImageLinks[i]).setAsImage(shipImageViews.get(i));
				countTextViews.get(i).setText(count + "");
			} else {
				shipLayouts.get(i).setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		updateLoads();
		drawControls.addInfrequentDrawListener(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		drawControls.removeInfrequentDrawListener(this);
	}

	@Override
	public void draw() {
		passengersLayout.post(this::updateLoads);
	}

	private void updateLoads() {
		List<List<EMovableType>> passengers = new ArrayList<>();
		List<List<CargoStack>> cargo = new ArrayList<>();
		for (ISelectable selectable : getSelection()) {
			if (selectable instanceof IGraphicsFerry) {
				passengers.add(getPassengerTypes((IGraphicsFerry) selectable));
			} else if (selectable instanceof IGraphicsCargoShip) {
				cargo.add(getCargoStacks((IGraphicsCargoShip) selectable));
			}
		}

		if (passengers.equals(shownPassengers) && cargo.equals(shownCargo)) {
			return;
		}
		shownPassengers = passengers;
		shownCargo = cargo;

		passengersLayout.removeAllViews();
		if (!passengers.isEmpty()) { // otherwise there might be no selected ship left to ask for the civilisation
			ECivilisation civilisation = getSelection().get(0).getPlayer().getCivilisation();
			for (List<EMovableType> shipPassengers : passengers) {
				passengersLayout.addView(createShipRow(civilisation, shipPassengers));
			}
		}
		for (List<CargoStack> shipCargo : cargo) {
			passengersLayout.addView(createCargoRow(shipCargo));
		}
	}

	private static List<CargoStack> getCargoStacks(IGraphicsCargoShip ship) {
		List<CargoStack> stacks = new ArrayList<>();
		for (int stack = 0; stack < ship.getNumberOfCargoStacks(); stack++) {
			int count = ship.getCargoCount(stack);
			stacks.add(new CargoStack(count > 0 ? ship.getCargoType(stack) : null, count)); // an unloaded stack keeps its old type
		}
		return stacks;
	}

	private View createCargoRow(List<CargoStack> stacks) {
		int height = getResources().getDimensionPixelSize(R.dimen.menu_tile_occupied_height);
		int freeHeight = getResources().getDimensionPixelSize(R.dimen.menu_tile_waiting_height);

		LinearLayout row = new LinearLayout(getActivity());
		row.setGravity(Gravity.CENTER_VERTICAL);

		for (CargoStack stack : stacks) {
			boolean loaded = stack.material != null;

			ImageView imageView = new ImageView(getActivity());
			OriginalImageProvider.get(loaded ? stack.material.getIcon() : FREE_SLOT).setAsImage(imageView);
			row.addView(imageView, new LinearLayout.LayoutParams(0, loaded ? height : freeHeight, 1));

			TextView countTextView = new TextView(getActivity());
			countTextView.setText(stack.count + "/" + Constants.STACK_SIZE);
			row.addView(countTextView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
		}

		return row;
	}

	private static List<EMovableType> getPassengerTypes(IGraphicsFerry ferry) {
		List<EMovableType> types = new ArrayList<>();
		for (Object passenger : ferry.getPassengers().toArray()) { // the game changes this list on another thread
			if (passenger != null) {
				types.add(((IGraphicsMovable) passenger).getMovableType());
			}
		}
		return types;
	}

	private View createShipRow(ECivilisation civilisation, List<EMovableType> passengers) {
		int occupiedHeight = getResources().getDimensionPixelSize(R.dimen.menu_tile_occupied_height);
		int freeHeight = getResources().getDimensionPixelSize(R.dimen.menu_tile_waiting_height);

		LinearLayout row = new LinearLayout(getActivity());
		row.setGravity(Gravity.CENTER_VERTICAL);

		for (int slot = 0; slot < IGraphicsFerry.MAX_NUMBER_OF_PASSENGERS; slot++) {
			ImageView slotImageView = new ImageView(getActivity());
			boolean occupied = slot < passengers.size();
			ImageLink image = occupied ? ImageLinkMap.get(civilisation, ECommonLinkType.SETTLER_GUI, passengers.get(slot)) : FREE_SLOT;
			if (image != null) {
				OriginalImageProvider.get(image).setAsImage(slotImageView);
			}
			row.addView(slotImageView, new LinearLayout.LayoutParams(0, occupied ? occupiedHeight : freeHeight, 1));
		}

		TextView countTextView = new TextView(getActivity());
		countTextView.setText(passengers.size() + "/" + IGraphicsFerry.MAX_NUMBER_OF_PASSENGERS);
		countTextView.setPadding(occupiedHeight / 4, 0, 0, 0);
		row.addView(countTextView);

		return row;
	}

	private static final class CargoStack {
		private final EMaterialType material;
		private final int count;

		private CargoStack(EMaterialType material, int count) {
			this.material = material;
			this.count = count;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof CargoStack && ((CargoStack) obj).material == material && ((CargoStack) obj).count == count;
		}

		@Override
		public int hashCode() {
			return 31 * count + (material == null ? 0 : material.ordinal());
		}
	}

	@Click(R.id.button_unload)
	void unloadClicked() {
		actionControls.fireAction(EActionType.UNLOAD_FERRIES);
	}

	@Click(R.id.button_kill)
	void killClicked() {
		Snackbar.make(getView(), R.string.confirm_destory_ships, Snackbar.LENGTH_SHORT)
				.setAction(R.string.yes, view1 -> actionControls.fireAction(EActionType.DESTROY))
				.show();
	}
}
