package com.apps.LarmLarms;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
	 * Stores all the Listables (Alarms and AlarmGroups) present
	 */
	private AlarmGroup dataset;
	/**
	 * Stores the context that this is being run in.
	 */
	private Context context;
	/**
	 * Messenger to send data requests to. Should connect to the AlarmDataService. Can be null, if
	 * the service isn't connected.
	 */
	private Messenger dataService;
	private final Messenger dataChangedMessenger;

	private List<Message> unsentMessages;

	RecyclerViewAdapter (Context currContext, ArrayList<Listable> data) {
		context = currContext;
		dataset = new AlarmGroup(context.getResources().getString(R.string.root_folder), data);
		dataService = null;
		dataChangedMessenger = new Messenger(new MsgHandler(this));
		unsentMessages = new ArrayList<>();

		createNotificationChannel();
		setNextAlarmToRing();
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
	 * @param view the ViewHolder to bind data to
	 * @param position the absolute position of the Listable to bind to the ViewHolder
	 */
	@Override
	public void onBindViewHolder (RecyclerViewHolder view, final int position) {
		BindHolderHandler h = new BindHolderHandler(this, view, position);
		Message m = Message.obtain(h, AlarmDataService.MSG_GET_LISTABLE, position, 0);
		try {
			dataService.send(m);
		}
		catch (NullPointerException | RemoteException e) {
			Log.e(TAG, "Tried to bind a view holder when the data service is null. Caching message.");
			unsentMessages.add(m);
		}
	}

	@Override
	public int getItemCount() { return dataset.getNumItems() - 1; }

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

	ArrayList<Listable> getListables() { return dataset.getListables(); }

	Listable getListableAbs(int absIndex) { return dataset.getListableAbs(absIndex); }

	void setListables(ArrayList<Listable> newList) {
		dataset.setListables(newList);
		notifyDataSetChanged();
		setNextAlarmToRing();
	}

	/**
	 * Adds the item to the end of the dataset, not nested. Is just like AlarmGroup:addListable()
	 * except totalNumItems doesn't include the parent (since there isn't one).
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
			Log.e(TAG, "Data service is null. Caching the message.");
			unsentMessages.add(msg);
		}
		setNextAlarmToRing();
	}

	/**
	 * Sets item as the new Listable in the dataset at the absolute index. If the index doesn't exist,
	 * doesn't do anything.
	 * @param absIndex the absolute index of the Listable to set
	 * @param item the new Listable to set it to
	 */
	void setListableAbs(int absIndex, Listable item) {
		dataset.setListableAbs(absIndex, item);
		notifyDataSetChanged();
		setNextAlarmToRing();
	}

	/**
	 * Deletes the item in the dataset at the absolute index
	 * @param absIndex the absolute index of the Listable to set
	 */
	private void deleteListableAbs(int absIndex) {
		dataset.deleteListableAbs(absIndex);
		notifyDataSetChanged();
		setNextAlarmToRing();
	}

	/* **********************************  Other Methods  ********************************* */

	/**
	 * Sets the next alarm to ring. Does not create a new pending intent, rather updates the current
	 * one. Tells AlarmManager to wake up and call NotificationCreatorService.
	 */
	void setNextAlarmToRing() {
		ListableInfo next = getNextRingingAlarm(dataset.getListables());
		if (next.listable == null) {
			Log.i(TAG, "No next listable to register to ring.");
			return;
		}

		Intent intent = new Intent(context, NotificationCreatorService.class);
		intent.putExtra(ListableEditorActivity.EXTRA_LISTABLE, next.listable.toEditString());
		intent.putExtra(ListableEditorActivity.EXTRA_LISTABLE_INDEX, next.absIndex);

		AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		if (manager != null && pendingIntent != null) {
			manager.setExact(AlarmManager.RTC_WAKEUP, ((Alarm) next.listable).getAlarmTimeMillis(), pendingIntent);
			Log.i(TAG, "Sent an intent to AlarmManager.");
		}
	}

	/**
	 * Searches for the next Alarm that will ring. Returns the listable and absolute index of the
	 * listable (within the current dataset) within a ListableInfo struct.
	 * @param data the dataset to look through
	 * @return a ListableInfo with alarm and absolute index filled correctly
	 */
	private static ListableInfo getNextRingingAlarm(ArrayList<Listable> data) {
		ListableInfo nextAlarm = new ListableInfo();
		Listable l;
		int absIndex = 0;

		for (int i = 0; i < data.size(); i++) {
			l = data.get(i);

			if (!l.isActive()) {
				absIndex += l.getNumItems();
				continue;
			}

			if (l.isAlarm()) {
				((Alarm) l).updateRingTime();

				// check whether it could be the next listable
				if (nextAlarm.listable == null || ((Alarm) l).getAlarmTimeMillis() <
						((Alarm) nextAlarm.listable).getAlarmTimeMillis()) {
					nextAlarm.listable = l;
					nextAlarm.absIndex = absIndex;
				}
				absIndex++;
			}
			else {
				ListableInfo possible = getNextRingingAlarm(((AlarmGroup) l).getListables());
				// there is no candidate in this folder
				if (possible.listable == null) {
					absIndex += l.getNumItems();
					continue;
				}
				// we had no candidate before or this candidate is better
				if (nextAlarm.listable == null || ((Alarm) possible.listable).getAlarmTimeMillis() <
						((Alarm) nextAlarm.listable).getAlarmTimeMillis()) {
					nextAlarm.listable = possible.listable;
					nextAlarm.absIndex = absIndex + possible.absIndex;
				}
				absIndex += l.getNumItems();
			}
		}
		return nextAlarm;
	}

	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = context.getString(R.string.notif_channel_name);
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel channel = new NotificationChannel(NotificationCreatorService.CHANNEL_ID, name, importance);
			channel.setShowBadge(false);
			channel.setBypassDnd(true);
			channel.enableLights(true);
			channel.enableVibration(true);
			channel.setSound(null, null);

			// Register the channel with the system; can't change the importance or behaviors after this
			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
			if (notificationManager == null) {
				Log.e(TAG, "System returned a null notification manager.");
				return;
			}
			notificationManager.createNotificationChannel(channel);
		}
	}

	void editExistingListable(final Listable listable, final int index) {
		// start new activity (AlarmCreator)
		Intent intent = new Intent(context, ListableEditorActivity.class);

		// add extras (Listable, index, req id)
		intent.putExtra(ListableEditorActivity.EXTRA_LISTABLE, listable.toEditString());
		intent.putExtra(ListableEditorActivity.EXTRA_LISTABLE_INDEX, index);
		// intent.putExtra(ListableEditorActivity.EXTRA_FOLDERS, dataset.toReducedString());
		Log.i(TAG, "Dataset's reduced string: " + dataset.toReducedString());

		int req;
		if (listable.isAlarm()) { req = ListableEditorActivity.REQ_EDIT_ALARM; }
		else { req = ListableEditorActivity.REQ_EDIT_FOLDER; }

		intent.putExtra(ListableEditorActivity.EXTRA_REQ_ID, req);

		((AppCompatActivity)context).startActivityForResult(intent, req);
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
					adapter.setNextAlarmToRing();
					return;
				case R.id.folder_icon:
					((AlarmGroup) listable).toggleOpen();
					adapter.notifyDataSetChanged();
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
	 * A handler passed to messages sent to the data service. Used for handling MSG_DATA_CHANGED
	 * messages mostly.
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
				case AlarmDataService.MSG_DATA_CHANGED:
					Log.i(TAG, "DATA_CHANGED message received from data service!");
					adapter.notifyDataSetChanged();
					break;
				default:
					Log.e(TAG, "Delivered message was of an unrecognized type. Sending to Handler.");
					super.handleMessage(msg);
					break;
			}
		}
	}

	/**
	 * A handler passed to messages sent to the data service. Used for binding listables (that need
	 * to be queried from the data service) to view holders.
	 */
	private static class BindHolderHandler extends Handler {
		private RecyclerViewAdapter adapter;
		private RecyclerViewHolder holder;
		private int absIndex;

		/**
		 * Creates a new handler in the main Looper.
		 * @param a the adapter to modify
		 * @param h the holder that needs to be bound
		 * @param pos the position of the holder (becomes absolute index)
		 */
		private BindHolderHandler(RecyclerViewAdapter a, RecyclerViewHolder h, final int pos) {
			super(Looper.getMainLooper());
			adapter = a;
			holder = h;
			absIndex = pos;
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.what != AlarmDataService.MSG_GET_LISTABLE) {
				Log.e(TAG, "Delivered message was not a GET_LISTABLE message. Sending to Handler.");
				super.handleMessage(msg);
				return;
			}

			// what used to be onBindViewHolder()
			ListableInfo i = msg.getData().getParcelable(AlarmDataService.BUNDLE_INFO_KEY);
			if (i == null) {
				Log.e(TAG, "Listable at absolute index " + absIndex + " does not exist!");
				return;
			}

			holder.changeListable(i.listable);

			// TODO: max indentation based on screen size?
			float dp = i.numIndents * adapter.context.getResources().getDimension(R.dimen.marginIncrement);
			ViewGroup.MarginLayoutParams params =
					new ViewGroup.MarginLayoutParams(holder.getCardView().getLayoutParams());
			// context.getResources().getDisplayMetrics().density gets the density scalar
			params.setMarginStart((int) (adapter.context.getResources().getDisplayMetrics().density * dp));
			holder.getCardView().setLayoutParams(params);
			holder.getCardView().requestLayout();

			// TODO: if this layout passing ends up being a bottleneck, can cache the current indent of
			// a ViewHolder to reduce the # of layout passes necessary (if same indent)
		}
	}
}
