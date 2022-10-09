package com.larmlarms.main;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.larmlarms.BuildConfig;
import com.larmlarms.R;
import com.larmlarms.data.Alarm;
import com.larmlarms.data.AlarmDataService;
import com.larmlarms.data.AlarmGroup;
import com.larmlarms.data.Listable;
import com.larmlarms.data.ListableInfo;
import com.larmlarms.editor.ListableEditorActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Queue;

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
	 * Stores all the Listables (Alarms and AlarmGroups) present, using only absolute indices.
	 */
	@NotNull
	private AlarmGroup data;

	/**
	 * Stores the context that this is being run in. Shouldn't be null.
	 */
	@NotNull
	private Context context;
	/**
	 * Messenger to send data requests to. Should connect to the AlarmDataService. Can be null, if
	 * the service isn't connected.
	 */
	@Nullable
	private Messenger dataService;
	/**
	 * Messenger sent to the data service, registered for data changed events.
	 */
	@NotNull
	private final Messenger dataChangedMessenger;
	/**
	 * Any messages that failed to send but need to be sent. Read in the order they were added.
	 */
	@NotNull
	private final Queue<Message> unsentMessages;

	/**
	 * Creates a new RecyclerViewAdapter with a specific context. Data starts out empty.
	 * @param currContext the current context, cannot be null
	 */
	RecyclerViewAdapter (@NotNull Context currContext) {
		context = currContext;

		data = new AlarmGroup(context.getResources().getString(R.string.root_folder));

		dataService = null;
		dataChangedMessenger = new Messenger(new MsgHandler(this));
		unsentMessages = new LinkedList<>();

		setHasStableIds(true);
	}

	/* ***************************  RecyclerView.Adapter Methods  ***************************** */

	/**
	 * Called when the recycler view wants to create a view holder
	 * @param parent the parent view group to attach it to
	 * @param viewType the type of view
	 * @return a new RecyclerViewHolder in the recycler view
	 */
	@Override
	public RecyclerViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
		// v is the cardView that was just inflated
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item, parent, false);
		return new RecyclerViewHolder(v, context, this);
	}

	/**
	 * Binds a specific piece of data (a Listable) to a ViewHolder. Called by the RecyclerView.
	 * @param holder the ViewHolder to bind data to
	 * @param position the absolute position of the Listable to bind to the ViewHolder
	 */
	@Override
	public void onBindViewHolder (@NotNull RecyclerViewHolder holder, final int position) {
		ListableInfo i = data.getListableInfo(position, true);
		if (i == null || i.listable == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The listable to display doesn't exist as far as the adapter knows.");
			return;
		}

		holder.changeListable(i.listable);

		// TODO: max indentation based on screen visibleSize?
		float dp = i.numIndents * context.getResources().getDimension(R.dimen.marginIncrement);
		ViewGroup.MarginLayoutParams params =
				new ViewGroup.MarginLayoutParams(holder.getCardView().getLayoutParams());
		// context.getResources().getDisplayMetrics().density gets the density scalar
		params.setMarginStart((int) (context.getResources().getDisplayMetrics().density * dp));
		holder.getCardView().setLayoutParams(params);
		holder.getCardView().requestLayout();
	}

	/**
	 * Gets the number of items within the recycler view
	 * @return number of items
	 */
	@Override
	public int getItemCount() { return data.visibleSize() - 1; }

	/**
	 * Gets the id for the item at the given position.
	 * @return returns the id of the given Listable, or -1 if it couldn't be found
	 */
	@Override
	public long getItemId(int position) {
		Listable l = data.getListableAbs(position, true);

		if (l == null) return -1;
		return l.getId();
	}

	/* ******************************  Getter and Setter Methods  ***************************** */

	/**
	 * Sets the data service messenger to the one passed in and registers dataChangedMessenger as a
	 * data changed listener. If there are any issues with the new messenger (can't send the
	 * MSG_DATA_CHANGED message), dataService will be set to null.
	 * @param messenger the new messenger to set dataService to
	 */
	void setDataService(@Nullable Messenger messenger) {
		Message outMsg;
		if (dataService != null) {
			// unregister data changed listener
			outMsg = Message.obtain(null, AlarmDataService.MSG_DATA_CHANGED);
			outMsg.replyTo = dataChangedMessenger;
			try {
				dataService.send(outMsg);
			}
			catch (RemoteException e) {
				e.printStackTrace();
				dataService = null;
			}
		}
		if (messenger != null) {
			// register data changed listener
			outMsg = Message.obtain(null, AlarmDataService.MSG_DATA_CHANGED);
			outMsg.replyTo = dataChangedMessenger;
			try {
				messenger.send(outMsg);
			}
			catch (RemoteException e) {
				e.printStackTrace();
				dataService = null;
				return;
			}

			// send all unsent messages in the order put there (they are lost if they don't get sent)
			while (!unsentMessages.isEmpty()) {
				try {
					messenger.send(unsentMessages.remove());
				}
				catch (RemoteException e) {
					if (BuildConfig.DEBUG) Log.e(TAG, "The new messenger no longer exists.");
					e.printStackTrace();
					dataService = null;
					return;
				}
			}
		}

		dataService = messenger;
	}

	/* **********************************  Other Methods  ********************************* */

	/**
	 * Sends an explicit intent off to ListableEditor for editing. Sends a ListableInfo describing
	 * the Listable to edit with the key ListableEditorActivity.EXTRA_LISTABLE_INFO, and the request
	 * ID with the key ListableEditorActivity.EXTRA_REQ_ID. If the listable is null, doesn't do
	 * anything.
	 * @param index the absolute index of the listable, assumes within range
	 */
	void editExistingListable(final int index) {
		// start ListableEditor
		Intent intent = new Intent(context, ListableEditorActivity.class);
		ListableInfo info = data.getListableInfo(index, true);
		if (info == null || info.listable == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The listable is null so it cannot be edited.");
			return;
		}

		intent.putExtra(ListableEditorActivity.EXTRA_LISTABLE_INFO, info);

		String action;
		if (info.listable instanceof Alarm) { action = ListableEditorActivity.ACTION_EDIT_ALARM; }
		else { action = ListableEditorActivity.ACTION_EDIT_FOLDER; }

		intent.setAction(action);

		context.startActivity(intent);
	}

	/**
	 * Sends a message to the data service using the adapter's messenger.
	 * @param msg the message to send
	 */
	private void sendMessage(@Nullable Message msg) {
		if (dataService == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Data service is unavailable. Caching the message.");
			unsentMessages.add(msg);
			return;
		}

		try {
			dataService.send(msg);
		}
		catch (RemoteException e) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Data service is unavailable. Caching the message.");
			unsentMessages.add(msg);
		}
	}

	/* ***********************************  Inner Classes  ************************************* */

	/**
	 * A required view holder class for the RecyclerView. Caches its child views so findViewById()
	 * isn't called as many times.
	 */
	public static class RecyclerViewHolder extends RecyclerView.ViewHolder
			implements View.OnClickListener, View.OnLongClickListener, DialogInterface.OnClickListener {
		/**
		 * Tag of the class for logging purposes.
		 */
		private static String TAG = "RecyclerViewHolder";

		/**
		 * Is the listable it currently represents. Don't modify it, since it's actually a pointer
		 * to the listable in the adapter too.
		 */
		@Nullable
		private Listable listable;
		/**
		 * The context of the holder. Shouldn't be null. Is required in an onClick callback.
		 */
		@NotNull
		private Context context;
		/**
		 * The adapter this holder is tied to.
		 */
		@NotNull
		private RecyclerViewAdapter adapter;

		/**
		 * Vector drawable for the open folder animation. Ends in the open state.
		 */
		@NotNull
		private Drawable openAnim;
		/**
		 * Vector drawable for the close folder animation. Ends in the closed state.
		 */
		@NotNull
		private Drawable closeAnim;

		// handles to views
		/**
		 * Main CardView view.
		 */
		private final View view;
		/**
		 * The title view. Shows the name of the listable.
		 */
		private final TextView titleView;
		/**
		 * The view that shows the repeat text of the Listable.
		 */
		private final TextView repeatView;
		/**
		 * The view that shows the time of the alarm. Is replaced by the image view when displaying
		 * a folder.
		 */
		private final TextView timeView;
		/**
		 * The active state switch. Turns the Listable on/off.
		 */
		private final Switch switchView;
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
			view = cardView;
			titleView = cardView.findViewById(R.id.title_text);
			repeatView = cardView.findViewById(R.id.repeat_text);
			timeView = cardView.findViewById(R.id.time_text);
			switchView = cardView.findViewById(R.id.on_switch);
			imageView = cardView.findViewById(R.id.folder_icon);

			// cache vector drawables
			openAnim = context.getResources().getDrawable(R.drawable.folder_open_animation, context.getTheme());
			closeAnim = context.getResources().getDrawable(R.drawable.folder_close_animation, context.getTheme());

			// create onclick callbacks
			view.setOnClickListener(this);
			view.setOnLongClickListener(this);
			switchView.setOnClickListener(this);
			imageView.setOnClickListener(this);
		}

		/* **************************  Getter and Setter Methods  ***************************** */

		/**
		 * Gets the main card view of the holder.
		 */
		View getCardView() { return view; }

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
		Switch getOnSwitch() { return switchView; }
		/**
		 * Gets the folder image view of the holder.
		 */
		ImageView getImageView() { return imageView; }

		/* ***********************************  Callbacks  ********************************* */

		/**
		 * The onclick listener for views within the view holder. If the card itself is clicked,
		 * will edit the listable. If the switch is clicked, toggles the switch. If the folder icon
		 * is clicked (meaning it's a folder), it will toggle the open/closed state.
		 * @param v the view that was clicked
		 */
		@Override
		public void onClick(@NotNull View v) {
			Message msg;
			switch(v.getId()) {
				case R.id.card_view:
					adapter.editExistingListable(getLayoutPosition());
					return;
				case R.id.on_switch:
					msg = Message.obtain(null, AlarmDataService.MSG_TOGGLE_ACTIVE,
							getLayoutPosition(), 0);
					adapter.sendMessage(msg);
					return;
				case R.id.folder_icon:
					msg = Message.obtain(null, AlarmDataService.MSG_TOGGLE_OPEN_FOLDER,
							getLayoutPosition(), 0);
					adapter.sendMessage(msg);
					return;
				default:
					if (BuildConfig.DEBUG) Log.e(TAG, "Unexpected view using the recycler view holder onClick method.");
			}
		}

		/**
		 * The onclick listener for a dialog (accessible by long clicking the listable).
		 * @param dialog dialog that was clicked
		 * @param which the item within the dialog that was clicked
		 */
		@Override
		public void onClick(@NotNull DialogInterface dialog, int which) {
			switch (which) {
				case 0:
					// delete the current listable
					Message msg = Message.obtain(null, AlarmDataService.MSG_DELETE_LISTABLE);
					msg.arg1 = getLayoutPosition();
					adapter.sendMessage(msg);
					break;
				default:
					if (BuildConfig.DEBUG) Log.e(TAG, "There was an invalid choice in the Listable dialog.");
					break;
			}
		}

		/**
		 * Long click for the entire card view (doesn't care where the user clicks, as long as it's
		 * on the card). Creates a recycler view dialog for the currently bound listable and returns
		 * true. If there is no listable bound, will not do anything and returns false.
		 * @param v the view that was clicked
		 * @return whether the long click was handled or not
		 */
		@Override
		public boolean onLongClick(@NotNull View v) {
			if (listable == null) {
				if (BuildConfig.DEBUG) Log.v(TAG, "Long clicked Listable is null.");
				return false;
			}

			RecyclerDialogFrag dialog = new RecyclerDialogFrag(this, listable instanceof Alarm);
			dialog.show(((MainActivity) context).getSupportFragmentManager(), DIALOG_FRAG_TAG);
			return true;
		}

		/* **********************************  Other Methods  ********************************** */

		/**
		 * Binds a new Listable to the current ViewHolder. If the new listable is null, will not
		 * change anything.
		 * @param l the new Listable to bind, can be null
		 */
		private void changeListable(@Nullable Listable l) {
			if (l == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "The new listable to swap into the view holder was null.");
				return;
			}
			getTitleText().setText(l.getListableName());
			getRepeatText().setText(l.getRepeatString());
			getTimeText().setText(l.getNextRingTime());
			getOnSwitch().setChecked(l.isActive());

			listable = l;
			if (l instanceof Alarm) {
				getImageView().setVisibility(View.GONE);
				getTimeText().setVisibility(View.VISIBLE);
			}
			else {
				// is an AlarmGroup
				getImageView().setVisibility(View.VISIBLE);
				getTimeText().setVisibility(View.GONE);

				if (((AlarmGroup) listable).getIsOpen()) {
					// open it
					imageView.setImageDrawable(openAnim);
				}
				else {
					// close it
					imageView.setImageDrawable(closeAnim);
				}
				((Animatable) imageView.getDrawable()).start();
			}
		}
	}

	/**
	 * A handler passed to messages sent to the data service. Used for handling MSG_DATA_CHANGED and
	 * MSG_GET_LISTABLE messages.
	 */
	private static class MsgHandler extends Handler {
		/**
		 * The adapter that owns this handler.
		 */
		@NotNull
		private RecyclerViewAdapter adapter;

		/**
		 * Creates a new handler in the main Looper.
		 * @param a the adapter to modify
		 */
		private MsgHandler(@NotNull RecyclerViewAdapter a) {
			super(Looper.getMainLooper());
			adapter = a;
		}

		/**
		 * Handles message from the data service. Handles almost all of the types of messages from
		 * the data service, except empty listener and folder structure messages.
		 * @param msg the inbound message
		 */
		@Override
		public void handleMessage(@Nullable Message msg) {
			if (msg == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Message sent to the recycler view adapter is null. Ignoring...");
				return;
			}

			ListableInfo info;
			Listable l;
			int index;
			switch (msg.what) {
				case AlarmDataService.MSG_SET_LISTABLE:
					// assumes new listable has nothing in it if it's an AlarmGroup
					info = msg.getData().getParcelable(AlarmDataService.BUNDLE_INFO_KEY);
					if (info == null || info.listable == null) {
						if (BuildConfig.DEBUG) Log.e(TAG, "Info sent back from the data service was null.");
						return;
					}
					l = adapter.data.getListableAbs(info.absIndex, true);	// old listable
					adapter.data.setListableAbs(info.absIndex, info.listable);
					if (l != null && l.visibleSize() != 1) adapter.notifyItemRangeRemoved(info.absIndex + 1, l.visibleSize() - 1);
					adapter.notifyItemChanged(info.absIndex);
					break;
				case AlarmDataService.MSG_ADD_LISTABLE:
					info = msg.getData().getParcelable(AlarmDataService.BUNDLE_INFO_KEY);
					if (info == null || info.listable == null) {
						if (BuildConfig.DEBUG)
							Log.e(TAG, "Info sent back from the data service was null.");
						return;
					}
					index = adapter.data.addListableAbs(info.listable, info.path);
					if (index == -2) {
						if (BuildConfig.DEBUG) Log.e(TAG, "Listable couldn't be found.");
						return;
					}
					else if (index == -1) {
						if (BuildConfig.DEBUG) Log.i(TAG, "Listable isn't visible.");
						break;
					}
					adapter.notifyItemInserted(index);
					break;
				case AlarmDataService.MSG_MOVE_LISTABLE:
					info = msg.getData().getParcelable(AlarmDataService.BUNDLE_INFO_KEY);
					if (info == null) {
						if (BuildConfig.DEBUG)
							Log.e(TAG, "Info sent back from the data service was null.");
						return;
					}

					l = adapter.data.getListableAbs(msg.arg1, true);    // old listable
					if (l == null) {
						if (BuildConfig.DEBUG) Log.e(TAG, "Listable in the list was null.");
						return;
					}

					// move stuff
					index = adapter.data.moveListableAbs(info.listable, info.path, msg.arg1);
					if (index == -1) {
						if (BuildConfig.DEBUG) Log.e(TAG, "Listable couldn't be found.");
						return;
					}
					if (info.listable == null) info.listable = l;

					// notify adapter that things have moved
					if (l.visibleSize() == 1) adapter.notifyItemRemoved(msg.arg1);
					else adapter.notifyItemRangeRemoved(msg.arg1, l.visibleSize());

					if (info.listable.visibleSize() == 1) adapter.notifyItemInserted(index);
					else adapter.notifyItemRangeInserted(index, info.listable.visibleSize());
					break;
				case AlarmDataService.MSG_DELETE_LISTABLE:
					l = adapter.data.deleteListableAbs(msg.arg1);
					if (l == null) {
						if (BuildConfig.DEBUG) Log.e(TAG, "Listable in the list was null.");
						return;
					}
					if (l instanceof Alarm) adapter.notifyItemRemoved(msg.arg1);
					else adapter.notifyItemRangeRemoved(msg.arg1, l.visibleSize());
					break;
				case AlarmDataService.MSG_TOGGLE_ACTIVE:
					l = adapter.data.getListableAbs(msg.arg1, true);
					if (l == null) {
						if (BuildConfig.DEBUG) Log.e(TAG, "Listable in the list was null.");
						return;
					}
					l.toggleActive();
					if (l instanceof Alarm) {
						((Alarm) l).updateRingTime();
						adapter.notifyItemChanged(msg.arg1);
					}
					else {
						// update all alarms inside
						Listable a;
						for (int i = 0; i < l.size() - 1; i++) {
							a = ((AlarmGroup) l).getListableAbs(i, false);
							if (a instanceof Alarm) ((Alarm) a).updateRingTime();
						}

						// notify possible item changes
						if (((AlarmGroup) l).getIsOpen())
							adapter.notifyItemRangeChanged(msg.arg1, l.size());
						else
							adapter.notifyItemChanged(msg.arg1);
					}
					break;
				case AlarmDataService.MSG_SNOOZE_ALARM:
					l = adapter.data.getListableAbs(msg.arg1, false);
					if (l == null || !(l instanceof Alarm)) {
						if (BuildConfig.DEBUG) Log.e(TAG, "Listable in the list was invalid.");
						return;
					}
					((Alarm) l).snooze();
					index = adapter.data.realToVisibleIndex(msg.arg1);
					adapter.notifyItemChanged(index);
					break;
				case AlarmDataService.MSG_UNSNOOZE_ALARM:
					l = adapter.data.getListableAbs(msg.arg1, false);
					if (l == null || !(l instanceof Alarm)) {
						if (BuildConfig.DEBUG) Log.e(TAG, "Listable in the list was invalid.");
						return;
					}
					((Alarm) l).unsnooze();
					index = adapter.data.realToVisibleIndex(msg.arg1);
					adapter.notifyItemChanged(index);
					break;
				case AlarmDataService.MSG_DISMISS_ALARM:
					l = adapter.data.getListableAbs(msg.arg1, false);
					if (l == null || !(l instanceof Alarm)) {
						if (BuildConfig.DEBUG) Log.e(TAG, "Listable in the list was invalid.");
						return;
					}
					((Alarm) l).dismiss();
					index = adapter.data.realToVisibleIndex(msg.arg1);
					adapter.notifyItemChanged(index);
					break;
				case AlarmDataService.MSG_TOGGLE_OPEN_FOLDER: {
					// gotta insert/delete new listables
					l = adapter.data.getListableAbs(msg.arg1, true);
					if (l == null || !(l instanceof AlarmGroup)) {
						if (BuildConfig.DEBUG) Log.e(TAG, "Listable in the list was invalid.");
						return;
					}

					int n = l.visibleSize() - 1;

					((AlarmGroup) l).toggleOpen();
					adapter.data.refreshLookups();

					adapter.notifyItemChanged(msg.arg1);
					if (n == 0) break;
					if (((AlarmGroup) l).getIsOpen()) {
						// was closed before, add new ones
						if (n == 1) adapter.notifyItemInserted(msg.arg1 + 1);
						else adapter.notifyItemRangeInserted(msg.arg1 + 1, n);
					} else {
						// was open before, delete old ones
						if (n == 1) adapter.notifyItemRemoved(msg.arg1 + 1);
						else adapter.notifyItemRangeRemoved(msg.arg1 + 1, n);
					}
					break;
				}
				case AlarmDataService.MSG_DATA_CHANGED:
					adapter.data.setListables(AlarmDataService.getAlarmsFromDisk(adapter.context));
					adapter.notifyDataSetChanged();
					break;
				default:
					if (BuildConfig.DEBUG) Log.e(TAG, "Delivered message was of an unrecognized type. Sending to Handler.");
					super.handleMessage(msg);
					break;
			}
		}
	}
}
