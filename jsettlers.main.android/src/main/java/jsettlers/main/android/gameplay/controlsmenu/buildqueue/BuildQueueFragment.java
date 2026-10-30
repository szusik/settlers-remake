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

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import jsettlers.main.android.R;
import jsettlers.main.android.core.resources.OriginalImageProvider;

@EFragment(R.layout.menu_build_queue)
public class BuildQueueFragment extends Fragment {
	public static BuildQueueFragment newInstance() {
		return new BuildQueueFragment_();
	}

	@ViewById(R.id.recycler_view)
	RecyclerView recyclerView;
	@ViewById(R.id.text_view_empty)
	View emptyView;

	private BuildQueueViewModel viewModel;

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		viewModel = ViewModelProviders.of(this, new BuildQueueViewModel.Factory(getActivity())).get(BuildQueueViewModel.class);

		BuildQueueAdapter adapter = new BuildQueueAdapter();
		recyclerView.setAdapter(adapter);

		viewModel.getBuildQueue().observe(this, items -> {
			adapter.setItems(items);
			emptyView.setVisibility(items.length == 0 ? View.VISIBLE : View.GONE);
		});
	}

	private class BuildQueueAdapter extends RecyclerView.Adapter<BuildQueueHolder> {
		private BuildQueueItem[] items = new BuildQueueItem[0];

		@Override
		public int getItemCount() {
			return items.length;
		}

		@Override
		public BuildQueueHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View itemView = getActivity().getLayoutInflater().inflate(R.layout.item_build_queue, parent, false);
			BuildQueueHolder holder = new BuildQueueHolder(itemView);
			itemView.setOnClickListener(view -> {
				int position = holder.getAdapterPosition();
				if (position != RecyclerView.NO_POSITION) {
					viewModel.showBuilding(items[position]);
				}
			});
			return holder;
		}

		@Override
		public void onBindViewHolder(BuildQueueHolder holder, int position) {
			holder.bind(position + 1, items[position]);
		}

		void setItems(BuildQueueItem[] newItems) {
			BuildQueueItem[] oldItems = items;
			DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
				@Override
				public int getOldListSize() {
					return oldItems.length;
				}

				@Override
				public int getNewListSize() {
					return newItems.length;
				}

				@Override
				public boolean areItemsTheSame(int oldPosition, int newPosition) {
					return oldItems[oldPosition].getPosition().equals(newItems[newPosition].getPosition());
				}

				@Override
				public boolean areContentsTheSame(int oldPosition, int newPosition) {
					return oldPosition == newPosition && oldItems[oldPosition].hasSameContent(newItems[newPosition]);
				}
			});
			items = newItems;
			diff.dispatchUpdatesTo(this);
		}
	}

	private class BuildQueueHolder extends RecyclerView.ViewHolder {
		private final TextView numberTextView;
		private final ImageView imageView;
		private final TextView nameTextView;
		private final TextView stageTextView;

		BuildQueueHolder(View itemView) {
			super(itemView);
			numberTextView = itemView.findViewById(R.id.text_view_number);
			imageView = itemView.findViewById(R.id.image_view);
			nameTextView = itemView.findViewById(R.id.text_view_building_name);
			stageTextView = itemView.findViewById(R.id.text_view_stage);
		}

		void bind(int number, BuildQueueItem item) {
			numberTextView.setText(Integer.toString(number));
			OriginalImageProvider.get(item.getVariant()).setAsImage(imageView);
			nameTextView.setText(item.getName());
			String stage = getString(getStageText(item.getStage()));
			switch (item.getPriority()) {
			case HIGH:
				stageTextView.setText(getString(R.string.build_queue_high_priority, stage));
				break;
			case STOPPED:
				stageTextView.setText(R.string.build_queue_stopped);
				break;
			default:
				stageTextView.setText(stage);
			}
		}

		private int getStageText(BuildQueueItem.Stage stage) {
			switch (stage) {
			case LEVELLING:
				return R.string.build_queue_levelling;
			case CONSTRUCTING:
				return R.string.build_queue_constructing;
			default:
				return R.string.build_queue_waiting_for_materials;
			}
		}
	}
}
