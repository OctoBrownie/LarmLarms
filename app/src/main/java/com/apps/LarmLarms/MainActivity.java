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
import android.util.Log;
import android.view.View;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
	 * Shows whether it is bound to the data service or not.
	 */
	private boolean boundToDataService = false;
	/**
	 * The service connection to the data service.
	 */
	@NotNull
	private EmptyServiceConnection dataConn;
	/**
	 * The messenger of the data service. Used for sending empty listener messages.
	 */
	@Nullable
	private Messenger dataMessenger;

	/**
	 * Creates a new MainActivity and initializes the data connection. Does not bind to the data
	 * service.
	 */
	public MainActivity() {
		dataConn = new EmptyServiceConnection(this);
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

			unbindService(dataConn);
			boundToDataService = false;
			dataMessenger = null;
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

	/* ***********************************  Inner Classes  ************************************** */

	/**
	 * The ServiceConnection to connect with the data service. Makes the activity an empty listener.
	 */
	private class EmptyServiceConnection implements ServiceConnection {
		/**
		 * The activity it binds to.
		 */
		@NotNull
		private MainActivity mainActivity;
		/**
		 * The empty messenger that the data service replies to with empty messages.
		 */
		@Nullable
		private Messenger emptyMessenger;

		/**
		 * Creates a new connection and sets the activity.
		 * @param act the activity that is using this connection
		 */
		private EmptyServiceConnection(@NotNull MainActivity act) {
			mainActivity = act;
		}

		/**
		 * Called when the data service connects to the activity. Sets some fields in the outer class
		 * and registers it as an empty listener.
		 * @param className the name of the class that was bound to (unused)
		 * @param service the binder that the service returned
		 */
		@Override
		public void onServiceConnected(@NotNull ComponentName className, @NotNull IBinder service) {
			boundToDataService = true;
			dataMessenger = new Messenger(service);

			Message msg = Message.obtain(null, AlarmDataService.MSG_DATA_EMPTY_LISTENER, 0, 0);
			emptyMessenger = new Messenger(new EmptyHandler(mainActivity));
			msg.replyTo = emptyMessenger;
			try {
				dataMessenger.send(msg);
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
			if (!boundToDataService || dataMessenger == null) return;

			Message msg = Message.obtain(null, AlarmDataService.MSG_DATA_EMPTY_LISTENER);
			msg.replyTo = emptyMessenger;
			try {
				dataMessenger.send(msg);
			}
			catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Inner handler class for handling empty/full messages from the data service.
	 */
	private static class EmptyHandler extends Handler {
		/**
		 * The activity it binds to.
		 */
		@NotNull
		private MainActivity activity;

		/**
		 * Creats a new handler on the main thread. Also sets the main activity.
		 * @param activity the activity that uses this handler
		 */
		private EmptyHandler(@NotNull MainActivity activity) {
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
				default:
					if (BuildConfig.DEBUG) Log.e(TAG, "Unknown message type. Sending to Handler's handleMessage().");
					super.handleMessage(msg);
					break;
			}
		}
	}
}
