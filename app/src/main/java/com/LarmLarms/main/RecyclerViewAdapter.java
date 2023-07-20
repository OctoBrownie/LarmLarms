package com.larmlarms.main;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.larmlarms.BuildConfig;
import com.larmlarms.Constants;
import com.larmlarms.R;
import com.larmlarms.data.Alarm;
import com.larmlarms.data.AlarmGroup;
import com.larmlarms.data.Item;
import com.larmlarms.editor.EditorActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Gets data for the RecyclerView holding alarms. It is unusable without the Messenger to the data
 * service, so be sure to set it before use.
 */
class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder> {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "RecyclerViewAdapter";
	/**
	 * Tag string for the dialogs made by an alarm/folder item.
	 */
	private static final String DIALOG_FRAG_TAG = "RecyclerView dialog";

	/**
	 * Handle to the current folder.
	 */
	@NotNull
	private final AlarmGroup data;

	/**
	 * Stores the context that this is being run in. Shouldn't be null.
	 */
	@NotNull
	private final Context context;

	/**
	 * Creates a new RecyclerViewAdapter with a specific context. Data starts out empty.
	 * @param context handle to the application, cannot be null
	 * @param folder the folder to display with this adapter, cannot be null
	 */
	RecyclerViewAdapter (@NotNull Context context, @NotNull AlarmGroup folder) {
		this.context = context;
		data = folder;

		setHasStableIds(true);
	}

	// ***************************  RecyclerView.Adapter Methods  *****************************

	/**
	 * Called when the recycler view wants to create a view holder
	 * @param parent the parent view group to attach it to
	 * @param viewType the type of view
	 * @return a new RecyclerViewHolder in the recycler view
	 */
	@NotNull @Override
	public RecyclerViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
		// v is the cardView that was just inflated
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item, parent, false);
		return new RecyclerViewHolder(v, context, this);
	}

	/**
	 * Binds a specific piece of data (a item) to a ViewHolder. Called by the RecyclerView.
	 * @param holder the ViewHolder to bind data to
	 * @param position the absolute position of the item to bind to the ViewHolder
	 */
	@Override
	public void onBindViewHolder (@NotNull RecyclerViewHolder holder, final int position) {
		Item item = data.getItem(position);
		if (item == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The item to display doesn't exist as far as the adapter knows.");
			return;
		}

		holder.changeItem(item);
	}

	/**
	 * Gets the number of items within the recycler view
	 * @return number of items
	 */
	@Override
	public int getItemCount() { return data.size() - 1; }

	/**
	 * Gets the id for the item at the given position.
	 * @return returns the id of the given item, or -1 if it couldn't be found
	 */
	@Override
	public long getItemId(int position) {
		Item l = data.getItem(position);

		if (l == null) return -1;
		return l.getId();
	}

	// **********************************  Other Methods  *********************************

	/**
	 * Sends an explicit intent off to editor for editing. Sends an ItemInfo describing
	 * the alarm to edit with the key EditorActivity.EXTRA_ITEM_INFO, and the request
	 * ID with the key EditorActivity.EXTRA_REQ_ID. If the alarm is null, doesn't do
	 * anything.
	 * @param alarm the alarm to edit, can be null
	 */
	private void editItem(@Nullable final Alarm alarm) {
		if (alarm == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The alarm is null so it cannot be edited.");
			return;
		}

		Intent intent = new Intent(context, EditorActivity.class);
		intent.putExtra(Constants.EXTRA_ITEM, alarm.toEditString());
		intent.putExtra(Constants.EXTRA_PATH, alarm.getPath());
		intent.setAction(Constants.ACTION_EDIT_ALARM);

		context.startActivity(intent);
	}

	/**
	 * Open the folder that just got clicked. Sends the path without a trailing slash.
	 * @param folder the folder to open
	 */
	private void openFolder(AlarmGroup folder) {
		if (folder == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The folder is null so it can't be shown.");
			return;
		}

		Intent intent = new Intent(context, FolderViewActivity.class);
		intent.putExtra(Constants.EXTRA_PATH, folder.getPath() + folder.getName());
		intent.setAction(Intent.ACTION_VIEW);
		context.startActivity(intent);
	}

	// ***********************************  Inner Classes  *************************************

	/**
	 * A required view holder class for the RecyclerView. Caches its child views so findViewById()
	 * isn't called as many times.
	 */
	public static class RecyclerViewHolder extends RecyclerView.ViewHolder
			implements View.OnClickListener, View.OnLongClickListener, DialogInterface.OnClickListener {
		/**
		 * Tag of the class for logging purposes.
		 */
		private final static String TAG = "RecyclerViewHolder";

		/**
		 * The item the holder currently represents.
		 */
		@Nullable
		private Item item;
		/**
		 * The context of the holder. Shouldn't be null. Is required in an onClick callback.
		 */
		@NotNull
		private final Context context;
		/**
		 * The adapter this holder is tied to.
		 */
		@NotNull
		private final RecyclerViewAdapter adapter;

		// handles to views
		/**
		 * The title view. Shows the name of the item.
		 */
		private final TextView titleView;
		/**
		 * The view that shows the repeat text of the item.
		 */
		private final TextView repeatView;
		/**
		 * The view that shows the time of the alarm. Is replaced by the image view when displaying
		 * a folder.
		 */
		private final TextView timeView;
		/**
		 * The active state switch. Turns the item on/off.
		 */
		private final SwitchMaterial switchView;
		/**
		 * The image view for the folder, showing the open/closing animation.
		 */
		private final ImageView imageView;

		/**
		 * Creates a new RecyclerViewHolder. Caches views to fields and creates onclick callbacks.
		 * @param cardView the main card view of the holder
		 * @param currContext the current context
		 * @param currAdapter the adapter that owns this holder
		 */
		RecyclerViewHolder (@NotNull View cardView, @NotNull Context currContext,
							@NotNull RecyclerViewAdapter currAdapter) {
			super(cardView);

			// saving stuff from the parameters
			adapter = currAdapter;
			context = currContext;

			// caching views
			titleView = cardView.findViewById(R.id.titleText);
			repeatView = cardView.findViewById(R.id.repeat_text);
			timeView = cardView.findViewById(R.id.time_text);
			switchView = cardView.findViewById(R.id.on_switch);
			imageView = cardView.findViewById(R.id.folder_icon);

			// create onclick callbacks
			cardView.setOnClickListener(this);
			cardView.setOnLongClickListener(this);
			switchView.setOnClickListener(this);
		}

		// **************************  Getter and Setter Methods  *****************************

		/**
		 * Gets the title text view of the holder.
		 */
		TextView getTitleText() { return titleView; }
		/**
		 * Gets the repeat string text view of the holder.
		 */
		TextView getRepeatText() { return repeatView; }
		/**
		 * Gets the time text view of the holder.
		 */
		TextView getTimeText() { return timeView; }
		/**
		 * Gets the active state switch of the holder.
		 */
		SwitchMaterial getOnSwitch() { return switchView; }
		/**
		 * Gets the folder image view of the holder.
		 */
		ImageView getImageView() { return imageView; }

		// ***********************************  Callbacks  *********************************

		/**
		 * The onclick listener for views within the view holder. If the card itself is clicked,
		 * will edit the item. If the switch is clicked, toggles the switch. If the folder icon
		 * is clicked (meaning it's a folder), it will toggle the open/closed state.
		 * @param v the view that was clicked
		 */
		@Override
		public void onClick(@NotNull View v) {
			int id = v.getId();
			if (id == R.id.card_view) {
				if (item instanceof Alarm) adapter.editItem((Alarm)item);
				else adapter.openFolder((AlarmGroup)item);
			}
			else if (id == R.id.on_switch) {
				if (item != null) item.toggleActive();
			}
			else {
				if (BuildConfig.DEBUG)
					Log.e(TAG, "Unexpected view using the recycler view holder onClick method.");
			}
		}

		/**
		 * The onclick listener for a dialog (accessible by long clicking the item).
		 * @param dialog dialog that was clicked
		 * @param which the item within the dialog that was clicked
		 */
		@Override
		public void onClick(@NotNull DialogInterface dialog, int which) {
			if (which == 0) {
				// delete the current item
				adapter.data.deleteItem(getLayoutPosition());
				adapter.notifyItemRemoved(getLayoutPosition());
			}
			else {
				if (BuildConfig.DEBUG)
					Log.e(TAG, "There was an invalid choice in the item dialog.");
			}
		}

		/**
		 * Long click for the entire card view (doesn't care where the user clicks, as long as it's
		 * on the card). Creates a recycler view dialog for the currently bound item and returns
		 * true. If there is no item bound, will not do anything and returns false.
		 * @param v the view that was clicked
		 * @return whether the long click was handled or not
		 */
		@Override
		public boolean onLongClick(@NotNull View v) {
			if (item == null) {
				if (BuildConfig.DEBUG) Log.v(TAG, "Long clicked item is null.");
				return false;
			}

			RecyclerDialogFrag dialog = new RecyclerDialogFrag(this, item instanceof Alarm);
			dialog.show(((MainActivity) context).getSupportFragmentManager(), DIALOG_FRAG_TAG);
			return true;
		}

		// **********************************  Other Methods  **********************************

		/**
		 * Binds a new item to the current ViewHolder. If the new item is null, will not
		 * change anything.
		 * @param l the new item to bind, can be null
		 */
		private void changeItem(@Nullable Item l) {
			if (l == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "The new item to swap into the view holder was null.");
				return;
			}
			getTitleText().setText(l.getName());
			getRepeatText().setText(l.getRepeatString());
			getTimeText().setText(l.getNextRingTime());
			getOnSwitch().setChecked(l.isActive());

			item = l;
			if (l instanceof Alarm) {
				getImageView().setVisibility(View.GONE);
				getTimeText().setVisibility(View.VISIBLE);
			}
			else {
				// is an AlarmGroup
				getImageView().setVisibility(View.VISIBLE);
				getTimeText().setVisibility(View.GONE);
			}
		}
	}
}
