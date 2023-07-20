package com.larmlarms.ringing;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.larmlarms.BuildConfig;
import com.larmlarms.Constants;
import com.larmlarms.R;
import com.larmlarms.data.Alarm;
import com.larmlarms.data.ItemInfo;
import com.larmlarms.main.PrefsActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import androidx.appcompat.app.AppCompatActivity;

public class RingingActivity extends AppCompatActivity {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "AlarmRingingActivity";

	/**
	 * The alarm that's ringing right now.
	 */
	private Alarm alarm;

	/**
	 * Path of the current alarm.
	 */
	private String path;

	// ***********************************  Lifecycle Methods  *********************************

	/**
	 * Called when the Activity is being created. Checks the calling intent for snooze/dismiss
	 * messages and binds to AlarmRingingService. Also dismisses keyguard manager so the activity
	 * can show on the lock screen.
	 * @param savedInstanceState the previous instance state
	 */
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PrefsActivity.applyPrefsStyle(this);
		setContentView(R.layout.activity_ringing);
		PrefsActivity.applyPrefsUI(this);

		// setting fields
		alarm = Alarm.fromEditString(this, getIntent().getStringExtra(Constants.EXTRA_ITEM));
		if (alarm == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The alarm given was invalid.");
			finish();
			return;
		}
		path = getIntent().getStringExtra(Constants.EXTRA_PATH);

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
		name.setText(alarm.getName());
	}

	// **************************************  Callbacks  **************************************

	/**
	 * Snoozes the current alarm and exits the activity. Also serves as the onclick for the snooze
	 * button.
	 * @param v view that was clicked (unused)
	 */
	public void snooze(@NotNull View v) {
		startService(new Intent(this, AfterRingingService.class)
				.putExtra(Constants.EXTRA_ITEM, alarm.toEditString())
				.putExtra(Constants.EXTRA_PATH, path)
				.setAction(Constants.ACTION_SNOOZE));
		finish();
	}

	/**
	 * Dismisses the current alarm and exits the activity. Also serves as the onclick for the
	 * dismiss button.
	 * @param v view that was clicked (unused)
	 */
	public void dismiss(@NotNull View v) {
		startService(new Intent(this, AfterRingingService.class)
				.putExtra(Constants.EXTRA_ITEM, alarm.toEditString())
				.putExtra(Constants.EXTRA_PATH, path)
				.setAction(Constants.ACTION_DISMISS));
		finish();
	}
}
