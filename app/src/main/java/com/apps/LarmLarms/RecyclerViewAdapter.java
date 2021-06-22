package com.apps.LarmLarms;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Gets data for the RecyclerView holding alarms. It takes a list of Listables (Alarms and AlarmGroups)
 * as its dataset.
 */
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder> {
	private static final String TAG = "RecyclerViewAdapter";

	// TODO: perhaps abstract this data to a ContentProvider?
	/**
	 * Stores all the Listables (Alarms and AlarmGroups) present
	 */
	private AlarmGroup dataset;

	private Context context;

	// required view holder class for the RecyclerView
	public static class RecyclerViewHolder extends RecyclerView.ViewHolder
		implements View.OnClickListener {
		private static String TAG = "RecyclerViewHolder";

		private Listable curr_listable;
		private Context context;
		private RecyclerViewAdapter adapter;
		private Drawable openAnim, closeAnim;

		// handles to views
		private final View view;
		private final TextView title_view;
		private final TextView repeat_view;
		private final TextView time_view;
		private final Switch switch_view;
		private final ImageView image_view;

		RecyclerViewHolder (View card_view, Context curr_context) {
			super(card_view);

			// saving the current context, needed in onClick callback
			context = curr_context;

			// caching handles to the holder's views
			view = card_view;
			title_view = card_view.findViewById(R.id.title_text);
			repeat_view = card_view.findViewById(R.id.repeat_text);
			time_view = card_view.findViewById(R.id.time_text);
			switch_view = card_view.findViewById(R.id.on_switch);
			image_view = card_view.findViewById(R.id.folder_icon);

			// cache vector drawables
			openAnim = context.getResources().getDrawable(R.drawable.folder_open_animation, context.getTheme());
			closeAnim = context.getResources().getDrawable(R.drawable.folder_close_animation, context.getTheme());

			// create onclick callbacks
			view.setOnClickListener(this);
			switch_view.setOnClickListener(this);
			image_view.setOnClickListener(this);
		}

		/* **************************  Getter and Setter Methods  ***************************** */

		View getCardView() { return view; }

		TextView getTitleText() { return title_view; }
		TextView getRepeatText() { return repeat_view; }
		TextView getTimeText() { return time_view; }
		Switch getOnSwitch() { return switch_view; }
		ImageView getImageView() { return image_view; }

		void setAdapter(RecyclerViewAdapter newAdapter) { adapter = newAdapter; }

		/* ***********************************  Callbacks  ********************************* */

		// onClick listener for view holders
		@Override
		public void onClick(View v) {
			switch(v.getId()) {
				case R.id.card_view:
					((MainActivity) context).editExistingListable(curr_listable, getLayoutPosition());
					return;
				case R.id.on_switch:
					curr_listable.toggleActive();
					adapter.setNextAlarmToRing();
					return;
				case R.id.folder_icon:
					((AlarmGroup) curr_listable).toggleOpen();
					adapter.refreshLookup();
					return;
				default:
					Log.e(TAG, "Unexpected view using the recycler view holder onClick method.");
			}
		}

		/* **********************************  Other Methods  ********************************** */

		// method for binding a new Listable to the current ViewHolder
		private void changeListable(Listable l) {
			getTitleText().setText(l.getListableName());
			getRepeatText().setText(l.getRepeatString());
			getTimeText().setText(l.getNextRingTime());
			getOnSwitch().setChecked(l.isActive());

			curr_listable = l;
			if (l.isAlarm()) {
				getImageView().setVisibility(View.GONE);
				getTimeText().setVisibility(View.VISIBLE);
			}
			else {
				// is an AlarmGroup
				getImageView().setVisibility(View.VISIBLE);
				getTimeText().setVisibility(View.GONE);

				if (((AlarmGroup) curr_listable).getIsOpen()) {
					// open it
					image_view.setImageDrawable(openAnim);
				}
				else {
					// close it
					image_view.setImageDrawable(closeAnim);
				}
				((Animatable) image_view.getDrawable()).start();
			}
		}
	}

	RecyclerViewAdapter (Context curr_context, ArrayList<Listable> data) {
		context = curr_context;
		dataset = new AlarmGroup();
		dataset.setListables(data);

		createNotificationChannel();
		setNextAlarmToRing();
	}

	/* ***************************  RecyclerViewAdapter Methods  ***************************** */

	@Override
	public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		// v is the cardView that was just inflated
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item, parent, false);
		RecyclerViewHolder r = new RecyclerViewHolder(v, context);
		r.setAdapter(this);
		((MainActivity) context).registerForContextMenu(v);

		return r;
	}

	/**
	 * Binds a specific piece of data (a Listable) to a ViewHolder. Called by the RecyclerView.
	 * @param view the ViewHolder to bind data to
	 * @param position the absolute position of the Listable to bind to the ViewHolder
	 */
	@Override
	public void onBindViewHolder (RecyclerViewHolder view, final int position) {
		ListableInfo i = dataset.getListableInfo(position);
		if (i == null) {
			Log.e(TAG, "Listable at absolute index " + position + " does not exist!");
			return;
		}

		view.changeListable(i.listable);

		// TODO: max indentation based on screen size?
		float dp = i.numIndents * context.getResources().getDimension(R.dimen.marginIncrement);
		ViewGroup.MarginLayoutParams params =
				new ViewGroup.MarginLayoutParams(view.getCardView().getLayoutParams());
		// context.getResources().getDisplayMetrics().density gets the density scalar
		params.setMarginStart((int) (context.getResources().getDisplayMetrics().density * dp));
		view.getCardView().setLayoutParams(params);
		view.getCardView().requestLayout();

		// TODO: if this layout passing ends up being a bottleneck, can cache the current indent of
		// a ViewHolder to reduce the # of layout passes necessary (if same indent)
	}

	@Override
	public int getItemCount() { return dataset.getNumItems() - 1; }

	/* ******************************  Getter and Setter Methods  ***************************** */

	ArrayList<Listable> getListables() { return dataset.getListables(); }

	Listable getListableAbs(int absIndex) {
		return dataset.getListableAbs(absIndex);
	}

	void setListables(ArrayList<Listable> newList) {
		dataset.setListables(newList);
		refreshLookup();
	}

	ArrayList<Integer> getLookup() { return dataset.getLookup(); }
	void refreshLookup() {
		dataset.refreshLookup();
		notifyDataSetChanged();
	}

	/**
	 * Adds the item to the end of the dataset, not nested. Is just like AlarmGroup:addListable()
	 * except totalNumItems doesn't include the parent (since there isn't one).
	 * @param item the Listable to add to the list
	 */
	void addListable(Listable item) {
		dataset.addListable(item);
		ArrayList<Integer> lookup = dataset.getLookup();
		notifyItemRangeInserted(lookup.get(Math.max(lookup.size() - 1, 0)), item.getNumItems());
	}

	/**
	 * Sets item as the new Listable in the dataset at the absolute index. If the index doesn't exist,
	 * doesn't do anything.
	 * @param absIndex the absolute index of the Listable to set
	 * @param item the new Listable to set it to
	 */
	void setListableAbs(int absIndex, Listable item) { dataset.setListableAbs(absIndex, item); }

	/**
	 * Sets item as the new Listable in the dataset at the relative index
	 * @param relIndex the relative index of the Listable to set
	 * @param item the new Listable to set it to
	 */
	private void setListableRel(int relIndex, Listable item) { dataset.setListable(relIndex, item); }

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
		intent.putExtra(MainActivity.EXTRA_LISTABLE, next.listable.toEditString());
		intent.putExtra(MainActivity.EXTRA_LISTABLE_INDEX, next.relIndex);

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
	 * listable within a ListableInfo struct.
	 * @param data the dataset to look through
	 * @return a ListableInfo with alarm and index filled correctly
	 */
	private static ListableInfo getNextRingingAlarm(ArrayList<Listable> data) {
		ListableInfo nextAlarm = new ListableInfo();
		Listable l;
		int currIndex = 0, listablesInNextAlarmFolder = 0;

		for (int i = 0; i < data.size(); i++) {
			l = data.get(i);

			if (!l.isActive()) {
				currIndex += l.getNumItems();
				continue;
			}

			if (l.isAlarm()) {
				((Alarm) l).updateRingTime();

				// check whether it could be the next listable
				if (nextAlarm.listable == null) { nextAlarm.listable = l; }
				else if (((Alarm) l).getAlarmTimeMillis() < ((Alarm) nextAlarm.listable).getAlarmTimeMillis()) {
					nextAlarm.listable = l;
					nextAlarm.relIndex = currIndex;
					currIndex += 1 + listablesInNextAlarmFolder;
					listablesInNextAlarmFolder = 0;
				}
				else { nextAlarm.relIndex++; }
			}
			else {
				ListableInfo possible = getNextRingingAlarm(((AlarmGroup) l).getListables());
				if (possible.listable == null) {
					currIndex += l.getNumItems();
					continue;
				}
				if (nextAlarm.listable == null) {
					nextAlarm.listable = possible.listable;
					listablesInNextAlarmFolder = l.getNumItems();
					nextAlarm.relIndex = currIndex;
					currIndex += possible.relIndex;
				}
				else if (((Alarm) possible.listable).getAlarmTimeMillis() <
						((Alarm) nextAlarm.listable).getAlarmTimeMillis()) {
					nextAlarm.listable = possible.listable;
					currIndex += 1 + listablesInNextAlarmFolder + possible.relIndex;
					listablesInNextAlarmFolder = l.getNumItems();
				}
				else { currIndex += l.getNumItems(); }
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
}
