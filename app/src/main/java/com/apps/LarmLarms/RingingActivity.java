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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import androidx.appcompat.app.AppCompatActivity;

public class RingingActivity extends AppCompatActivity {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "AlarmRingingActivity";

	/**
	 * Absolute index of the alarm that's ringing.
	 */
	private int alarmAbsIndex;

	/* ***********************************  Lifecycle Methods  ********************************* */

	/**
	 * Called when the Activity is being created. Checks the calling intent for snooze/dismiss
	 * messages and binds to AlarmRingingService. Also dismisses keyguard manager so the activity
	 * can show on the lock screen.
	 * @param savedInstanceState the previous instance state
	 */
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alarm_ringing);

		// setting fields
		Alarm currAlarm = Alarm.fromEditString(this,
				getIntent().getStringExtra(RingingService.EXTRA_LISTABLE));
		if (currAlarm == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The alarm given was invalid.");
			finish();
			return;
		}
		
		alarmAbsIndex = getIntent().getIntExtra(RingingService.EXTRA_LISTABLE_INDEX, -1);
		if (alarmAbsIndex == -1) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The alarm's index was invalid.");
			finish();
			return;
		}

		// show on lock screen
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
			setShowWhenLocked(true);
			setTurnScreenOn(true);
			KeyguardManager key = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
			if (key == null) {
				finish();
				return;
			}
			key.requestDismissKeyguard(this, null);
		}
		else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
			KeyguardManager key = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
			if (key == null) {
				finish();
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
	}

	/* **************************************  Callbacks  ************************************** */

	/**
	 * Snoozes the current alarm and exits the activity. Also serves as the onclick for the snooze
	 * button.
	 * @param v view that was clicked (unused)
	 */
	public void snooze(@NotNull View v) {
		startService(new Intent(this, AfterRingingService.class)
				.putExtra(RingingService.EXTRA_LISTABLE_INDEX, alarmAbsIndex)
				.setAction(AfterRingingService.ACTION_SNOOZE));
		finish();
	}

	/**
	 * Dismisses the current alarm and exits the activity. Also serves as the onclick for the
	 * dismiss button.
	 * @param v view that was clicked (unused)
	 */
	public void dismiss(@NotNull View v) {
		startService(new Intent(this, AfterRingingService.class)
				.putExtra(RingingService.EXTRA_LISTABLE_INDEX, alarmAbsIndex)
				.setAction(AfterRingingService.ACTION_DISMISS));
		finish();
	}
}
