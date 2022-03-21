package com.apps.LarmLarms;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;
import java.util.GregorianCalendar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

/**
 * The main page of the app, showing a list of alarms/folders that the user can scroll through.
 */
public class MainActivity extends AppCompatActivity {
	/**
	 * Tag of the class for logging purposes.
	 */
	private final static String TAG = "MainActivity";

	/**
	 * The TextView that is shown when the list is empty.
	 */
	private View noAlarmsText;
	/**
	 * The FrameView that contains the recycler view fragment
	 */
	private View fragContainer;
	/**
	 * The TextView showing the next alarm that will ring.
	 */
	private TextView nextAlarmText;

	/**
	 * Shows whether it is bound to the data service or not.
	 */
	private boolean boundToDataService = false;
	/**
	 * The service connection to the data service.
	 */
	@NotNull
	private DataServiceConnection dataConn;
	/**
	 * The messenger of the data service. Used for sending empty listener or next alarm listener
	 * messages.
	 */
	@Nullable
	private Messenger serviceMessenger;

	/**
	 * Creates a new MainActivity and initializes the data connection. Does not bind to the data
	 * service.
	 */
	public MainActivity() {
		dataConn = new DataServiceConnection();
	}

	/* ********************************* Lifecycle Methods ********************************* */

	/**
	 * Called when the activity is being created. Caches views to class fields.
	 * @param savedInstanceState the previously saved instance state
	 */
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		noAlarmsText = findViewById(R.id.no_alarms_text);
		fragContainer = findViewById(R.id.frag_frame);
		nextAlarmText = findViewById(R.id.next_alarm_text);

		// always need to reinflate the frag
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(R.id.frag_frame, RecyclerViewFrag.class, null, "recycler_frag");
		trans.commitNow();
	}

	/**
	 * Called when the activity is started. Binds to the data service.
	 */
	@Override
	protected void onStart() {
		super.onStart();

		bindService(new Intent(this, AlarmDataService.class), dataConn, Context.BIND_AUTO_CREATE);
	}

	/**
	 * Called when the activity is stopped. Unbinds from the data service.
	 */
	@Override
	protected void onStop() {
		super.onStop();

		// unbind from service
		if (boundToDataService) {
			dataConn.removeEmptyListener();
			dataConn.removeNextAlarmListener();

			unbindService(dataConn);
			boundToDataService = false;
			serviceMessenger = null;
		}
	}

	/* ************************************  Callbacks  ************************************** */

	/**
	 * Onclick callback for adding a new alarm, bound to the + button.
	 * @param view the view that was clicked
	 */
	public void addNewAlarm(@NotNull View view) {
		// start AlarmCreator activity
		Intent intent = new Intent(this, ListableEditorActivity.class);
		intent.setAction(ListableEditorActivity.ACTION_CREATE_ALARM);

		startActivity(intent);
	}

	/**
	 * Onclick callback for adding a new folder, bound to the folder button.
	 * @param view the view that was clicked
	 */
	public void addNewFolder(@NotNull View view) {
		// start AlarmCreator activity
		Intent intent = new Intent(this, ListableEditorActivity.class);
		intent.setAction(ListableEditorActivity.ACTION_CREATE_FOLDER);

		startActivity(intent);
	}

	/* ************************************  Other Methods  ************************************* */

	/**
	 * Shows the recycler view fragment and hides the noAlarmsText.
	 */
	private void showFrag() {
		fragContainer.setVisibility(View.VISIBLE);
		noAlarmsText.setVisibility(View.GONE);
	}

	/**
	 * Hides the recycler view fragment and shows the noAlarmsText.
	 */
	private void hideFrag() {
		fragContainer.setVisibility(View.GONE);
		noAlarmsText.setVisibility(View.VISIBLE);
	}

	/**
	 * Changes the next alarm to ring text based on what was returned from the data service.
	 * @param b the data bundle returned from the data service
	 */
	private void changeNextAlarm(@NotNull Bundle b) {
		String text;

		if (b.getString(AlarmDataService.BUNDLE_NAME_KEY) == null)
			text = getResources().getString(R.string.main_no_next_alarm);
		else {
			Log.i("DataService", "Did the thing, boss.");
			GregorianCalendar time = new GregorianCalendar(), rightNow = new GregorianCalendar();
			time.setTimeInMillis(b.getLong(AlarmDataService.BUNDLE_TIME_KEY));

			String dateString = null;
			if (time.get(Calendar.DAY_OF_MONTH) == rightNow.get(Calendar.DAY_OF_MONTH)) {
				dateString = getResources().getString(R.string.main_date_string_today);
			}

			rightNow.add(Calendar.DAY_OF_MONTH, 1);
			if (time.get(Calendar.DAY_OF_MONTH) == rightNow.get(Calendar.DAY_OF_MONTH)) {
				dateString = getResources().getString(R.string.main_date_string_tomorrow);
			}

			if (dateString == null) {
				dateString = String.format(getResources().getString(R.string.main_date_string),
						DateFormat.getMediumDateFormat(this).format(time.getTimeInMillis()));
			}
			String timeString = DateFormat.getTimeFormat(this).format(time.getTimeInMillis());

			text = String.format(getResources().getString(R.string.main_next_alarm),
					b.getString(AlarmDataService.BUNDLE_NAME_KEY), dateString, timeString);
		}
		nextAlarmText.setText(text);
	}

	/* ***********************************  Inner Classes  ************************************** */

	/**
	 * The ServiceConnection to connect with the data service. Makes the activity an empty listener
	 * and a next alarm listener.
	 */
	private class DataServiceConnection implements ServiceConnection {
		/**
		 * The messenger that the data service replies to with empty or next alarm messages.
		 */
		@Nullable
		private Messenger replyMessenger;

		/**
		 * Called when the data service connects to the activity. Sets some fields in the outer class
		 * and registers it as an empty listener and next alarm listener.
		 * @param className the name of the class that was bound to (unused)
		 * @param service the binder that the service returned
		 */
		@Override
		public void onServiceConnected(@NotNull ComponentName className, @NotNull IBinder service) {
			boundToDataService = true;
			serviceMessenger = new Messenger(service);
			replyMessenger = new Messenger(new DataServiceHandler(MainActivity.this));

			Message emptyMsg = Message.obtain(null, AlarmDataService.MSG_DATA_EMPTY_LISTENER, 0, 0);
			emptyMsg.replyTo = replyMessenger;
			Message nextAlarmMsg = Message.obtain(null, AlarmDataService.MSG_NEXT_ALARM, 0, 0);
			nextAlarmMsg.replyTo = replyMessenger;
			try {
				serviceMessenger.send(emptyMsg);
				serviceMessenger.send(nextAlarmMsg);
			}
			catch (RemoteException e) {
				// handler doesn't exist, messenger is unusable
				e.printStackTrace();
				boundToDataService = false;
			}
		}

		/**
		 * Called when the data service crashes. Hides the RecyclerView so that the screen isn't blank.
		 * @param className the name of the class that was bound to (unused)
		 */
		@Override
		public void onServiceDisconnected(@NotNull ComponentName className) {
			// sad, it crashed...
			boundToDataService = false;
			hideFrag();
		}

		/**
		 * Removes the activity from the data service as an empty listener.
		 */
		private void removeEmptyListener() {
			if (!boundToDataService || MainActivity.this.serviceMessenger == null) return;

			Message msg = Message.obtain(null, AlarmDataService.MSG_DATA_EMPTY_LISTENER);
			msg.replyTo = replyMessenger;
			try {
				MainActivity.this.serviceMessenger.send(msg);
			}
			catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Removes the activity from the data service as an next alarm listener.
		 */
		private void removeNextAlarmListener() {
			if (!boundToDataService || MainActivity.this.serviceMessenger == null) return;

			Message msg = Message.obtain(null, AlarmDataService.MSG_NEXT_ALARM);
			msg.replyTo = replyMessenger;
			try {
				MainActivity.this.serviceMessenger.send(msg);
			}
			catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Inner handler class for handling empty/full messages and next alarm messages from the data
	 * service.
	 */
	private static class DataServiceHandler extends Handler {
		/**
		 * The activity it binds to.
		 */
		@NotNull
		private MainActivity activity;

		/**
		 * Creats a new handler on the main thread. Also sets the main activity.
		 * @param activity the activity that uses this handler
		 */
		private DataServiceHandler(@NotNull MainActivity activity) {
			super(Looper.getMainLooper());
			this.activity = activity;
		}

		/**
		 * Handles messages from the data service.
		 * @param msg the inbound message
		 */
		@Override
		public void handleMessage(@Nullable Message msg) {
			if (msg == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Message sent to the main activity was null. Ignoring...");
				return;
			}

			switch(msg.what) {
				case AlarmDataService.MSG_DATA_EMPTIED:
					activity.hideFrag();
					break;
				case AlarmDataService.MSG_DATA_FILLED:
					activity.showFrag();
					break;
				case AlarmDataService.MSG_NEXT_ALARM:
					activity.changeNextAlarm(msg.getData());
					break;
				default:
					if (BuildConfig.DEBUG) Log.e(TAG, "Unknown message type. Sending to Handler's handleMessage().");
					super.handleMessage(msg);
					break;
			}
		}
	}
}
