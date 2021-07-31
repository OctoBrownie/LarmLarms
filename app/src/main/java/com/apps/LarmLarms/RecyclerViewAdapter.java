package com.apps.LarmLarms;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Gets data for the RecyclerView holding alarms. It is unusable without the Messenger to the data
 * service, so be sure to set it before use.
 */
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder> {
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
	private ArrayList<ListableInfo> data;
	/**
	 * Stores the number of listables in the dataset. Updated by AlarmDataService when MSG_DATA_CHANGED
	 * messages are sent.
	 */
	private int dataSize;

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
	 * TODO: could implement as a Queue (LinkedList as a specific implementation?) instead
	 */
	@NotNull
	private final List<Message> unsentMessages;

	/**
	 * Creates a new RecyclerViewAdapter with a specific context. Data starts out empty.
	 * @param currContext the current context, cannot be null
	 */
	RecyclerViewAdapter (@NotNull Context currContext) {
		context = currContext;

		data = new ArrayList<>();
		dataSize = 0;

		dataService = null;
		dataChangedMessenger = new Messenger(new MsgHandler(this));
		unsentMessages = new ArrayList<>();
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
		ListableInfo i = data.get(position);

		holder.changeListable(i.listable);

		// TODO: max indentation based on screen size?
		float dp = i.numIndents * context.getResources().getDimension(R.dimen.marginIncrement);
		ViewGroup.MarginLayoutParams params =
				new ViewGroup.MarginLayoutParams(holder.getCardView().getLayoutParams());
		// context.getResources().getDisplayMetrics().density gets the density scalar
		params.setMarginStart((int) (context.getResources().getDisplayMetrics().density * dp));
		holder.getCardView().setLayoutParams(params);
		holder.getCardView().requestLayout();

		// TODO: if this layout passing ends up being a bottleneck, can cache the current indent of
		// a ViewHolder to reduce the # of layout passes necessary (if same indent)
	}

	/**
	 * Gets the number of items within the recycler view
	 * @return number of items
	 */
	@Override
	public int getItemCount() { return dataSize; }

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

			// send all unsent messages in the order put there (unless they still don't get sent...)
			int numMessages = unsentMessages.size();
			for (int i = 0; i < numMessages; i++) {
				try {
					messenger.send(unsentMessages.get(0));
					unsentMessages.remove(0);
				}
				catch (RemoteException e) {
					Log.e(TAG, "The new messenger no longer exists.");
					e.printStackTrace();
					dataService = null;
					return;
				}
			}
		}

		dataService = messenger;
	}

	// used mostly because it's synchronous and (should be) up to date

	/**
	 * Gets the listable at the specified absolute index
	 * @param absIndex absolute index of the Listable
	 * @return the Listable that you asked for, or null if the index is out of bounds
	 */
	@Nullable
	Listable getListableAbs(int absIndex) {
		if (absIndex < 0 || absIndex >= data.size()) {
			Log.v(TAG, "Absolute index to get listable for is out of bounds.");
			return null;
		}

		return data.get(absIndex).listable;
	}

	/**
	 * Sends a message to AlarmDataService to add a new listable to the end of the list, not nested.
	 * @param item the Listable to add to the list
	 */
	void addListable(@Nullable Listable item) {
		Message msg = Message.obtain(null, AlarmDataService.MSG_ADD_LISTABLE);

		ListableInfo info = new ListableInfo();
		info.listable = item;

		Bundle b = new Bundle();
		b.putParcelable(AlarmDataService.BUNDLE_INFO_KEY, info);
		msg.setData(b);

		sendMessage(msg);
	}

	/**
	 * Sends a message to AlarmDataService to set the absolute index to the specified listable.
	 * @param absIndex the absolute index of the Listable to set
	 * @param item the new Listable to set it to
	 */
	void setListableAbs(int absIndex, @Nullable Listable item) {
		Message msg = Message.obtain(null, AlarmDataService.MSG_SET_LISTABLE);

		ListableInfo info = new ListableInfo();
		info.listable = item;

		Bundle b = new Bundle();
		b.putParcelable(AlarmDataService.BUNDLE_INFO_KEY, info);
		msg.setData(b);

		msg.arg1 = absIndex;
		sendMessage(msg);
	}

	/**
	 * Sends a message to AlarmDataService to delete the listable at the specified index
	 * @param absIndex the absolute index of the Listable to set
	 */
	private void deleteListableAbs(int absIndex) {
		Message msg = Message.obtain(null, AlarmDataService.MSG_DELETE_LISTABLE);
		msg.arg1 = absIndex;
		sendMessage(msg);
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
		// start new activity (AlarmCreator)
		Intent intent = new Intent(context, ListableEditorActivity.class);
		ListableInfo info = data.get(index);
		if (info == null || info.listable == null) {
			Log.e(TAG, "The listable is null so it cannot be edited.");
			return;
		}

		// add extras (Listable, index, req id)
		intent.putExtra(ListableEditorActivity.EXTRA_LISTABLE_INFO, info);

		int req;
		if (info.listable.isAlarm()) { req = ListableEditorActivity.REQ_EDIT_ALARM; }
		else { req = ListableEditorActivity.REQ_EDIT_FOLDER; }

		intent.putExtra(ListableEditorActivity.EXTRA_REQ_ID, req);

		((AppCompatActivity)context).startActivityForResult(intent, req);
	}

	/**
	 * Refreshes all listables within the cached dataset by sending new queries to AlarmDataService.
	 * Assumes the field dataSize is correct and cleans dataset before retrieval of new data.
	 */
	void refreshListables() {
		// cleaning dataset
		int currSize = data.size();
		if (currSize > dataSize) {
			for (; currSize > dataSize; currSize--) {
				data.remove(currSize - 1);
			}
		}
		else if (currSize < dataSize) {
			for (; dataSize > currSize; currSize++) {
				data.add(null);
			}
		}

		// sending data messages
		Message m;
		for (int pos = 0; pos < dataSize; pos++) {
			m = Message.obtain(null, AlarmDataService.MSG_GET_LISTABLE, pos, 0);
			m.replyTo = dataChangedMessenger;
			sendMessage(m);
		}
	}

	/**
	 * Sends a message to the data service using the adapter's messenger.
	 * @param msg the message to send
	 */
	private void sendMessage(@Nullable Message msg) {
		if (dataService == null) {
			Log.e(TAG, "Data service is unavailable. Caching the message.");
			unsentMessages.add(msg);
			return;
		}

		try {
			dataService.send(msg);
		}
		catch (RemoteException e) {
			Log.e(TAG, "Data service is unavailable. Caching the message.");
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
		 * to the data service listable itself.
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
					Log.e(TAG, "Unexpected view using the recycler view holder onClick method.");
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
					adapter.deleteListableAbs(getLayoutPosition());
					break;
				default:
					Log.e(TAG, "There was an invalid choice in the Listable dialog.");
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
				Log.v(TAG, "Long clicked Listable is null.");
				return false;
			}

			RecyclerDialogFrag diag = new RecyclerDialogFrag(this, listable.isAlarm());
			diag.show(((MainActivity) context).getSupportFragmentManager(), DIALOG_FRAG_TAG);
			return true;
		}

		/* **********************************  Other Methods  ********************************** */

		/**
		 * Binds a new Listable to the current ViewHolder.
		 * @param l the new Listable to bind
		 */
		private void changeListable(Listable l) {
			getTitleText().setText(l.getListableName());
			getRepeatText().setText(l.getRepeatString());
			getTimeText().setText(l.getNextRingTime());
			getOnSwitch().setChecked(l.isActive());

			listable = l;
			if (l.isAlarm()) {
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
		 * Handles message from the data service. Only handles GET_LISTABLE and DATA_CHANGED messages.
		 * @param msg the inbound message
		 */
		@Override
		public void handleMessage(@Nullable Message msg) {
			if (msg == null) {
				Log.e(TAG, "Message sent to the recycler view adapter was null. Ignoring...");
				return;
			}

			switch (msg.what) {
				case AlarmDataService.MSG_GET_LISTABLE:
					handleGetListable(msg);
					break;
				case AlarmDataService.MSG_DATA_CHANGED:
					adapter.dataSize = msg.arg1;
					adapter.refreshListables();
					break;
				default:
					Log.e(TAG, "Delivered message was of an unrecognized type. Sending to Handler.");
					super.handleMessage(msg);
					break;
			}
		}

		/**
		 * Handles a GET_LISTABLE message.
		 * @param msg the inbound MSG_GET_LISTABLE message, shouldn't be null
		 */
		private void handleGetListable(@NotNull Message msg) {
			if (msg.what != AlarmDataService.MSG_GET_LISTABLE) {
				Log.e(TAG, "Delivered message was not a GET_LISTABLE message. Sending to Handler.");
				super.handleMessage(msg);
				return;
			}

			int absIndex = msg.arg1;

			// what used to be onBindViewHolder()
			ListableInfo i = msg.getData().getParcelable(AlarmDataService.BUNDLE_INFO_KEY);
			if (i == null || i.listable == null) {
				Log.e(TAG, "Listable at absolute index " + absIndex + " does not exist!");
				return;
			}

			if (i.listable.isAlarm())
				((Alarm)(i.listable)).setContext(adapter.context);

			// assumes the index absIndex is valid
			adapter.data.set(absIndex, i);

			// we know these messages arrive in order, so if we get the last one, notify data changed
			if (absIndex == adapter.dataSize - 1)
				adapter.notifyDataSetChanged();
		}
	}
}
