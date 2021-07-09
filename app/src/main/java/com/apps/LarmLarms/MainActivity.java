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

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {
	private final static String TAG = "MainActivity";

	// some important views
	private RecyclerViewFrag myRecyclerFrag;
	private View noAlarmsText;
	private View fragContainer;

	private boolean boundToDataService = false;
	private EmptyServiceConnection dataConn;
	private Messenger dataMessenger;

	/* ********************************* Lifecycle Methods ********************************* */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		noAlarmsText = findViewById(R.id.no_alarms_text);
		fragContainer = findViewById(R.id.frag_frame);

		// always need to reinflate the frag
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(R.id.frag_frame, RecyclerViewFrag.class, null, "recycler_frag");
		trans.commitNow();

		myRecyclerFrag = (RecyclerViewFrag) getSupportFragmentManager().findFragmentByTag("recycler_frag");

		Log.i(TAG, "Activity created successfully.");
	}

	@Override
	protected void onStart() {
		super.onStart();

		dataConn = new EmptyServiceConnection(this);
		bindService(new Intent(this, AlarmDataService.class), dataConn, Context.BIND_AUTO_CREATE);
	}

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

	// onClick callback for the + Alarm button (id: addNewAlarmButton)
	public void addNewAlarm(View view) {
		// start AlarmCreator activity
		Intent intent = new Intent(this, ListableEditorActivity.class);
		intent.putExtra(ListableEditorActivity.EXTRA_REQ_ID, ListableEditorActivity.REQ_NEW_ALARM);

		startActivityForResult(intent, ListableEditorActivity.REQ_NEW_ALARM);
	}

	// onClick callback for the add new folder button
	public void addNewFolder(View view) {
		// start AlarmCreator activity
		Intent intent = new Intent(this, ListableEditorActivity.class);
		intent.putExtra(ListableEditorActivity.EXTRA_REQ_ID, ListableEditorActivity.REQ_NEW_FOLDER);

		startActivityForResult(intent, ListableEditorActivity.REQ_NEW_FOLDER);
	}

	/**
	 * Callback for when Activities finish with a result. Deals with the output of each Activity.
	 *
	 * For AlarmCreators: adds the newly-made or edited alarm into the RecyclerView
	 *
	 * @param requestCode the request code the closed Activity was started with
	 * @param resultCode the result code the closed Activity returned
	 * @param data the intent returned by the closed Activity
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO: if this isn't true for some other activity we come back from, change
		if (resultCode == RESULT_CANCELED) {
			Log.i(TAG, "Action cancelled.");
			return;
		}
		if (resultCode != RESULT_OK || data == null ||
				data.getStringExtra(ListableEditorActivity.EXTRA_LISTABLE) == null) {
			Log.e(TAG, "Data from ListableEditorActivity was invalid.");
			return;
		}

		switch(requestCode) {
			case ListableEditorActivity.REQ_NEW_ALARM:
			case ListableEditorActivity.REQ_EDIT_ALARM:
			case ListableEditorActivity.REQ_NEW_FOLDER:
			case ListableEditorActivity.REQ_EDIT_FOLDER:
				myRecyclerFrag.onListableCreatorResult(requestCode, data);
				break;
			default:
				Log.e(TAG, "Unknown request code returned.");
		}

	}

	/* ************************************  Other Methods  ************************************* */

	private void showFrag() {
		fragContainer.setVisibility(View.VISIBLE);
		noAlarmsText.setVisibility(View.GONE);
		Log.i("RecyclerViewFragment", "Recycler view shown.");
	}

	private void hideFrag() {
		fragContainer.setVisibility(View.GONE);
		noAlarmsText.setVisibility(View.VISIBLE);
		Log.i("RecyclerViewFragment", "Recycler view hidden.");
	}

	/* ***********************************  Inner Classes  ************************************** */

	private class EmptyServiceConnection implements ServiceConnection {
		private MainActivity mainActivity;
		private Messenger emptyMessenger;

		private EmptyServiceConnection(MainActivity act) {
			mainActivity = act;
		}

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
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

		@Override
		public void onServiceDisconnected(ComponentName className) {
			// sad, it crashed...
			boundToDataService = false;
		}

		private void removeEmptyListener() {
			if (!boundToDataService) return;

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

	private static class EmptyHandler extends Handler {
		private MainActivity activity;
		private EmptyHandler(MainActivity activity) {
			super(Looper.getMainLooper());
			this.activity = activity;
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg == null) {
				Log.e(TAG, "Message sent to the main activity was null. Ignoring...");
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
					Log.e(TAG, "Unknown message type. Sending to Handler's handleMessage().");
					super.handleMessage(msg);
					break;
			}
		}
	}
}