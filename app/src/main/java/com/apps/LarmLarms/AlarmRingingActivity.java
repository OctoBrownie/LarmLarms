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

import androidx.appcompat.app.AppCompatActivity;

public class AlarmRingingActivity extends AppCompatActivity {
	private static final String TAG = "AlarmRingingActivity";

	private Alarm currAlarm;

	private boolean boundToDataService = false;
	private ServiceConnection dataConn;
	private Messenger dataMessenger;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alarm_ringing);

		// setting fields
		currAlarm = Alarm.fromEditString(this, getIntent().getStringExtra(ListableEditorActivity.EXTRA_LISTABLE));
		if (currAlarm == null) {
			Log.e(TAG, "The alarm given was invalid...?");
			finish();
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
		dataConn = new DataServiceConnection();
		bindService(new Intent(this, AlarmDataService.class), dataConn, Context.BIND_AUTO_CREATE);
	}

	/**
	 * Snoozes the current alarm and exits teh activity. Also serves as the onclick for the snooze
	 * button.
	 * @param v unused view
	 */
	public void snooze(View v) {
		currAlarm.snooze();
		// TODO: actually snooze the alarm in place instead of setting it
		exitActivity();
	}

	/**
	 * Dismisses the current alarm, sets the next alarm to ring, and exits the activity. Also serves
	 * as the onclick for the dismiss button.
	 * @param v unused view
	 */
	public void dismiss(View v) {
		currAlarm.unsnooze();

		switch (currAlarm.getRepeatType()) {
			case Alarm.REPEAT_ONCE_ABS:
			case Alarm.REPEAT_ONCE_REL:
				currAlarm.turnOff();
				break;
			case Alarm.REPEAT_DAY_WEEKLY:
			case Alarm.REPEAT_DATE_MONTHLY:
			case Alarm.REPEAT_DAY_MONTHLY:
			case Alarm.REPEAT_DATE_YEARLY:
			case Alarm.REPEAT_OFFSET:
				currAlarm.updateRingTime();
				break;
			default:
				Log.wtf(TAG, "The repeat type of the alarm was invalid...?");
				break;
		}
		// TODO: actually dismiss the alarm in place instead of setting it
		exitActivity();
	}

	/**
	 * Sets the next alarm to ring and exits.
	 */
	public void exitActivity() {
		if (!boundToDataService) {
			Log.e(TAG, "Not currently bound to the data service...");
			// TODO: do something besides log the error
			return;
		}

		int absIndex = getIntent().getIntExtra(ListableEditorActivity.EXTRA_LISTABLE_INDEX, -1);
		Message msg = Message.obtain(null, AlarmDataService.MSG_SET_LISTABLE, absIndex, 0);

		ListableInfo info = new ListableInfo();
		info.listable = currAlarm;

		Bundle b = new Bundle();
		b.putParcelable(AlarmDataService.BUNDLE_INFO_KEY, info);
		msg.setData(b);

		try {
			dataMessenger.send(msg);
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}

		Intent serviceIntent = new Intent(this, NotificationCreatorService.class);
		stopService(serviceIntent);

		dataMessenger = null;
		boundToDataService = false;
		unbindService(dataConn);

		finish();
	}

	/* ************************************  Inner Classes  ********************************** */

	private class DataServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.i(TAG, "Connected to the data service.");
			boundToDataService = true;
			dataMessenger = new Messenger(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.e(TAG, "The data service crashed.");
			boundToDataService = false;
			dataMessenger = null;
		}
	}
}
