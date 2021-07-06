package com.apps.LarmLarms;

import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import androidx.appcompat.app.AppCompatActivity;

public class AlarmRingingActivity extends AppCompatActivity {
	private static final String TAG = "AlarmRingingActivity";

	static final String ACTION_SNOOZE = "com.apps.LarmLarms.ACTION_SNOOZE";
	static final String ACTION_DISMISS = "com.apps.LarmLarms.ACTION_DISMISS";

	private boolean boundToRingingService = false;
	private ServiceConnection ringingConn;

	private Messenger ringingService = null;
	private Message unsentMessage = null;

	/* ***********************************  Lifecycle Methods  ********************************* */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String action = getIntent().getAction();
		if (ACTION_SNOOZE.equals(action) || ACTION_DISMISS.equals(action)) {
			ringingConn = new RingingServiceConnection();
			bindService(new Intent(this, AlarmDataService.class), ringingConn, Context.BIND_AUTO_CREATE);

			// sets unsentMessage so that when the service is bound, it will send the message and exit
			switch(action) {
				case ACTION_SNOOZE:
					unsentMessage = Message.obtain(null, AlarmDataService.MSG_SNOOZE_ALARM);
					break;
				case ACTION_DISMISS:
					unsentMessage = Message.obtain(null, AlarmDataService.MSG_DISMISS_ALARM);
					break;
			}
			return;
		}

		// open activity normally

		setContentView(R.layout.activity_alarm_ringing);

		// setting fields
		Alarm currAlarm = Alarm.fromEditString(this,
				getIntent().getStringExtra(ListableEditorActivity.EXTRA_LISTABLE));
		if (currAlarm == null) {
			Log.e(TAG, "The alarm given was invalid...?");
			finish();
			return;
		}

		// show on lock screen
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
			setShowWhenLocked(true);
			setTurnScreenOn(true);
			KeyguardManager key = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
			if (key == null) {
				exitActivity();
				return;
			}
			key.requestDismissKeyguard(this, null);
		}
		else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
			KeyguardManager key = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
			if (key == null) {
				exitActivity();
				return;
			}
			key.requestDismissKeyguard(this, null);
			this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
					WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		}
		else {
			this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
					WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
					WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		}

		// setting UI things
		TextView name = findViewById(R.id.alarmName);
		name.setText(currAlarm.getListableName());

		// binding to AlarmDataService
		ringingConn = new RingingServiceConnection();
		bindService(new Intent(this, AlarmRingingService.class), ringingConn, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (boundToRingingService) {
			ringingService = null;
			boundToRingingService = false;
			unbindService(ringingConn);
		}

		Intent serviceIntent = new Intent(this, AlarmRingingService.class);
		stopService(serviceIntent);
	}

	/* **************************************  Callbacks  ************************************** */

	/**
	 * Snoozes the current alarm and exits teh activity. Also serves as the onclick for the snooze
	 * button.
	 * @param v unused view
	 */
	public void snooze(View v) {
		Message msg = Message.obtain(null, AlarmDataService.MSG_SNOOZE_ALARM, 0, 0);
		sendMessage(msg);
		exitActivity();
	}

	/**
	 * Dismisses the current alarm, sets the next alarm to ring, and exits the activity. Also serves
	 * as the onclick for the dismiss button.
	 * @param v unused view
	 */
	public void dismiss(View v) {
		Message msg = Message.obtain(null, AlarmDataService.MSG_DISMISS_ALARM, 0, 0);
		sendMessage(msg);
		exitActivity();
	}

	/* *************************************  Other Methods  ************************************ */

	/**
	 * Sends any unsent messages and exits. If the message still isn't sent, doesn't exit.
	 */
	public void exitActivity() {
		if (!sendMessage(unsentMessage)) {
			// message is not null and didn't get through
			return;
		}

		unsentMessage = null;
		finish();
	}

	/**
	 * Sends a message to the ringing service using the activity's messenger.
	 * @param msg the message to send, can be null
	 * @return returns whether the message went through or not, true if message is null
	 */
	@Contract("null -> true")
	private boolean sendMessage(@Nullable Message msg) {
		if (msg == null) return true;

		try {
			ringingService.send(msg);
			return true;
		}
		catch (NullPointerException | RemoteException e) {
			Log.e(TAG, "Ringing service is unavailable. Caching message.");
			unsentMessage = msg;
			return false;
		}
	}

	/* ************************************  Inner Classes  ********************************** */

	private class RingingServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			boundToRingingService = true;
			ringingService = new Messenger(service);

			if (unsentMessage != null) {
				sendMessage(unsentMessage);
				unsentMessage = null;
				finish();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.e(TAG, "The ringing service crashed.");
			boundToRingingService = false;
			ringingService = null;
		}
	}
}
