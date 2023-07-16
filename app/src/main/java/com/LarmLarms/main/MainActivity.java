package com.larmlarms.main;

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
import android.widget.Toast;

import com.larmlarms.BuildConfig;
import com.larmlarms.R;
import com.larmlarms.data.AlarmDataService;
import com.larmlarms.editor.ListableEditorActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;
import java.util.GregorianCalendar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

/**
 * The main page of the app, showing a list of alarms/folders that the user can scroll through.
 */
public class MainActivity extends AppCompatActivity implements View.OnLongClickListener {
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

	/* ********************************* Lifecycle Methods ********************************* */

	/**
	 * Called when the activity is being created. Caches views to class fields.
	 * @param savedInstanceState the previously saved instance state
	 */
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PrefsActivity.applyPrefsStyle(this);
		setContentView(R.layout.activity_main);
		PrefsActivity.applyPrefsUI(this);

		noAlarmsText = findViewById(R.id.no_alarms_text);
		fragContainer = findViewById(R.id.frag_frame);
		nextAlarmText = findViewById(R.id.next_alarm_text);

		findViewById(R.id.addAlarmButton).setOnLongClickListener(this);
		findViewById(R.id.addFolderButton).setOnLongClickListener(this);
		findViewById(R.id.settingsButton).setOnLongClickListener(this);

		// always need to reinflate the frag
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(R.id.frag_frame, RecyclerViewFrag.class, null, "recycler_frag");
		trans.commitNow();
	}

	/**
	 * Called when the activity is resuming. Checks if we need to restart to apply a new theme.
	 */
	@Override
	protected void onResume() {
		super.onResume();

		MainApplication app = (MainApplication) getApplication();
		if (app.needsRestart) {
			app.needsRestart = false;
			recreate();
		}

		if (app.rootFolder.size() != 1) showFrag();
		else hideFrag();
	}

	/* ************************************  Callbacks  ************************************** */

	/**
	 * Callback for long clicks. Shows descriptions of the buttons that are long clicked
	 * @param view the view that was long clicked
	 * @return whether this consumed the long click (always returns true)
	 */
	@Override
	public boolean onLongClick(@NotNull View view) {
		int id = view.getId();
		if (id == R.id.addAlarmButton)
			Toast.makeText(this, R.string.main_alarm_description, Toast.LENGTH_SHORT).show();
		else if (id == R.id.addFolderButton)
			Toast.makeText(this, R.string.main_folder_description, Toast.LENGTH_SHORT).show();
		else if (id == R.id.settingsButton)
			Toast.makeText(this, R.string.main_settings_description, Toast.LENGTH_SHORT).show();

		return true;
	}

	/**
	 * Onclick callback for adding a new alarm, bound to the + button.
	 * @param view the view that was clicked (the add alarm button)
	 */
	public void addNewAlarm(@NotNull View view) {
		// start AlarmCreator activity
		Intent intent = new Intent(this, ListableEditorActivity.class);
		intent.setAction(ListableEditorActivity.ACTION_CREATE_ALARM);

		startActivity(intent);
	}

	/**
	 * Onclick callback for adding a new folder, bound to the folder button.
	 * @param view the view that was clicked (the folder button)
	 */
	public void addNewFolder(@NotNull View view) {
		// start the listable editor
		Intent intent = new Intent(this, ListableEditorActivity.class);
		intent.setAction(ListableEditorActivity.ACTION_CREATE_FOLDER);

		startActivity(intent);
	}

	/**
	 * Callback for the settings button. Opens the settings.
	 * @param view the view that was clicked (the settings button)
	 */
	public void openSettings(@NotNull View view) {
		// start the preferences
		Intent intent = new Intent(this, PrefsActivity.class);
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
}
