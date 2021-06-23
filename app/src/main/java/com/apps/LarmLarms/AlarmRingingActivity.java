package com.apps.LarmLarms;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AlarmRingingActivity extends AppCompatActivity {
	private static final String TAG = "AlarmRingingActivity";

	private Alarm currAlarm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alarm_ringing);

		currAlarm = Alarm.fromEditString(this, getIntent().getStringExtra(ListableEditorActivity.EXTRA_LISTABLE));
		if (currAlarm == null) {
			Log.e(TAG, "The alarm given was invalid...?");
			finish();
		}

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

		TextView name = findViewById(R.id.alarmName);
		name.setText(currAlarm.getListableName());
	}

	/**
	 * Snoozes the current alarm and exits teh activity. Also serves as the onclick for the snooze
	 * button.
	 * @param v unused view
	 */
	public void snooze(View v) {
		currAlarm.snooze();
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

		exitActivity();
	}

	/**
	 * Sets the next alarm to ring and exits.
	 */
	public void exitActivity() {
		Intent serviceIntent = new Intent(this, NotificationCreatorService.class);
		stopService(serviceIntent);

		RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, RecyclerViewFrag.getAlarmsFromDisk(this));
		adapter.setListableAbs(getIntent().getIntExtra(ListableEditorActivity.EXTRA_LISTABLE_INDEX, -1), currAlarm);

		adapter.setNextAlarmToRing();
		RecyclerViewFrag.writeAlarmsToDisk(this, adapter);

		finish();
	}
}
