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

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Gets data for the RecyclerView holding alarms. It is unusable without the Messenger to the data
 * service, so be sure to set it before use.
 */
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder> {
	private static final String TAG = "RecyclerViewAdapter";
	private static final String DIALOG_FRAG_TAG = "RecyclerView dialog";

	/**
	 * Stores all the Listables (Alarms and AlarmGroups) present, using only absolute indices.
	 */
	private ArrayList<ListableInfo> data;
	/**
	 * Stores the number of listables in the dataset. Updated by AlarmDataService when MSG_DATA_CHANGED
	 * messages are sent.
	 */
	private int dataSize;

	/**
	 * Stores the context that this is being run in.
	 */
	private Context context;
	/**
	 * Messenger to send data requests to. Should connect to the AlarmDataService. Can be null, if
	 * the service isn't connected.
	 */
	private Messenger dataService;
	/**
	 * Messenger sent to the data service, registered for data changed events.
	 */
	private final Messenger dataChangedMessenger;
	/**
	 * Any messages that failed to send but need to be sent. Read in the order they were added.
	 * TODO: could implement as a Queue (LinkedList as a specific implementation?) instead
	 */
	private List<Message> unsentMessages;

	RecyclerViewAdapter (Context currContext) {
		context = currContext;

		data = new ArrayList<>();
		dataSize = 0;

		dataService = null;
		dataChangedMessenger = new Messenger(new MsgHandler(this));
		unsentMessages = new ArrayList<>();
	}

	/* ***************************  RecyclerView.Adapter Methods  ***************************** */

	@Override
	public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		// v is the cardView that was just inflated
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item, parent, false);
		RecyclerViewHolder r = new RecyclerViewHolder(v, context);
		r.setAdapter(this);

		return r;
	}

	/**
	 * Binds a specific piece of data (a Listable) to a ViewHolder. Called by the RecyclerView.
	 * @param holder the ViewHolder to bind data to
	 * @param position the absolute position of the Listable to bind to the ViewHolder
	 */
	@Override
	public void onBindViewHolder (RecyclerViewHolder holder, final int position) {
		Log.i(TAG, "onBindViewHolder called for this position: " + position);

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

	@Override
	public int getItemCount() { return dataSize; }

	/* ******************************  Getter and Setter Methods  ***************************** */

	/**
	 * Sets the data service messenger to the one passed in and registers dataChangedMessenger as a
	 * data changed listener. If there are any issues with the new messenger (can't send the
	 * MSG_DATA_CHANGED message), dataService will be set to null.
	 * @param messenger the new messenger to set dataService to
	 */
	void setDataService(Messenger messenger) {
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
					Log.e(TAG, "The new messenger no longer exists. No bind to ");
					e.printStackTrace();
					dataService = null;
					return;
				}
			}
		}

		dataService = messenger;
	}

	// TODO: could just go to the data service directly (this gets the DATA_CHANGED messages so...)

	Listable getListableAbs(int absIndex) { return data.get(absIndex).listable; }

	/**
	 * Sends a message to AlarmDataService to add a new listable to the end of the list, not nested.
	 * @param item the Listable to add to the list
	 */
	void addListable(Listable item) {
		Message msg = Message.obtain(null, AlarmDataService.MSG_ADD_LISTABLE);

		ListableInfo info = new ListableInfo();
		info.listable = item;

		Bundle b = new Bundle();
		b.putParcelable(AlarmDataService.BUNDLE_INFO_KEY, info);
		msg.setData(b);

		try {
			dataService.send(msg);
		}
		catch (NullPointerException | RemoteException e) {
			Log.e(TAG, "Data service is unavailable. Caching the message.");
			unsentMessages.add(msg);
		}
	}

	/**
	 * Sends a message to AlarmDataService to set the absolute index to the specified listable.
	 * @param absIndex the absolute index of the Listable to set
	 * @param item the new Listable to set it to
	 */
	void setListableAbs(int absIndex, Listable item) {
		Message msg = Message.obtain(null, AlarmDataService.MSG_SET_LISTABLE);

		ListableInfo info = new ListableInfo();
		info.listable = item;

		Bundle b = new Bundle();
		b.putParcelable(AlarmDataService.BUNDLE_INFO_KEY, info);
		msg.setData(b);

		msg.arg1 = absIndex;

		try {
			dataService.send(msg);
		}
		catch (NullPointerException | RemoteException e) {
			Log.e(TAG, "Data service is unavailable. Caching the message.");
			unsentMessages.add(msg);
		}
	}

	/**
	 * Sends a message to AlarmDataService to delete the listable at the specified index
	 * @param absIndex the absolute index of the Listable to set
	 */
	private void deleteListableAbs(int absIndex) {
		Message msg = Message.obtain(null, AlarmDataService.MSG_SET_LISTABLE);
		msg.arg1 = absIndex;

		try {
			dataService.send(msg);
		}
		catch (NullPointerException | RemoteException e) {
			Log.e(TAG, "Data service is unavailable. Caching the message.");
			unsentMessages.add(msg);
		}
	}

	/* **********************************  Other Methods  ********************************* */

	/**
	 * Sends the specified listable off to ListableEditor for editing
	 * @param listable the listable to edit
	 * @param index the absolute index of the listable
	 */
	void editExistingListable(final Listable listable, final int index) {
		// start new activity (AlarmCreator)
		Intent intent = new Intent(context, ListableEditorActivity.class);

		// add extras (Listable, index, req id)
		intent.putExtra(ListableEditorActivity.EXTRA_LISTABLE, listable.toEditString());
		intent.putExtra(ListableEditorActivity.EXTRA_LISTABLE_INDEX, index);

		int req;
		if (listable.isAlarm()) { req = ListableEditorActivity.REQ_EDIT_ALARM; }
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

			try {
				dataService.send(m);
			}
			catch (NullPointerException | RemoteException e) {
				Log.e(TAG, "Tried to bind a view holder when the data service is unavailable. Caching message.");
				unsentMessages.add(m);
			}
		}
	}

	/* ***********************************  Inner Classes  ************************************* */

	/**
	 * A required view holder class for the RecyclerView. Caches its child views so findViewById()
	 * isn't called as many times.
	 */
	public static class RecyclerViewHolder extends RecyclerView.ViewHolder
			implements View.OnClickListener, View.OnLongClickListener, DialogInterface.OnClickListener {
		private static String TAG = "RecyclerViewHolder";

		private Listable listable;
		private Context context;
		private RecyclerViewAdapter adapter;
		private Drawable openAnim, closeAnim;

		// handles to views
		private final View view;
		private final TextView titleView;
		private final TextView repeatView;
		private final TextView timeView;
		private final Switch switchView;
		private final ImageView imageView;

		RecyclerViewHolder (View cardView, Context currContext) {
			super(cardView);

			// saving the current context, needed in onClick callback
			context = currContext;

			// caching handles to the holder's views
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

		View getCardView() { return view; }

		TextView getTitleText() { return titleView; }
		TextView getRepeatText() { return repeatView; }
		TextView getTimeText() { return timeView; }
		Switch getOnSwitch() { return switchView; }
		ImageView getImageView() { return imageView; }

		void setAdapter(RecyclerViewAdapter newAdapter) { adapter = newAdapter; }

		/* ***********************************  Callbacks  ********************************* */

		// onClick listener for view holders
		@Override
		public void onClick(View v) {
			switch(v.getId()) {
				case R.id.card_view:
					adapter.editExistingListable(listable, getLayoutPosition());
					return;
				case R.id.on_switch:
					listable.toggleActive();
					// TODO: also toggle active in AlarmDataService
					return;
				case R.id.folder_icon:
					((AlarmGroup) listable).toggleOpen();
					// TODO: also toggle open in AlarmDataService
					return;
				default:
					Log.e(TAG, "Unexpected view using the recycler view holder onClick method.");
			}
		}

		// onClick listener for dialog click
		@Override
		public void onClick(DialogInterface dialog, int which) {
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

		@Override
		public boolean onLongClick(View v) {
			RecyclerDialogFrag diag = new RecyclerDialogFrag(this, listable.isAlarm());
			diag.show(((MainActivity) context).getSupportFragmentManager(), DIALOG_FRAG_TAG);
			return true;
		}

		/* **********************************  Other Methods  ********************************** */

		// method for binding a new Listable to the current ViewHolder
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
		private RecyclerViewAdapter adapter;

		/**
		 * Creates a new handler in the main Looper.
		 * @param a the adapter to modify
		 */
		private MsgHandler(RecyclerViewAdapter a) {
			super(Looper.getMainLooper());
			adapter = a;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case AlarmDataService.MSG_GET_LISTABLE:
					Log.i(TAG, "GET_LISTABLE message received from data service!");
					handleGetListable(msg);
					break;
				case AlarmDataService.MSG_DATA_CHANGED:
					Log.i(TAG, "DATA_CHANGED message received from data service!");
					adapter.dataSize = msg.arg1;
					adapter.refreshListables();
					break;
				default:
					Log.e(TAG, "Delivered message was of an unrecognized type. Sending to Handler.");
					super.handleMessage(msg);
					break;
			}
		}

		private void handleGetListable(Message msg) {
			if (msg.what != AlarmDataService.MSG_GET_LISTABLE) {
				Log.e(TAG, "Delivered message was not a GET_LISTABLE message. Sending to Handler.");
				super.handleMessage(msg);
				return;
			}

			int absIndex = msg.arg1;

			// what used to be onBindViewHolder()
			ListableInfo i = msg.getData().getParcelable(AlarmDataService.BUNDLE_INFO_KEY);
			if (i == null) {
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
