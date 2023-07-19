package com.larmlarms.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.larmlarms.Constants;
import com.larmlarms.R;
import com.larmlarms.data.Alarm;
import com.larmlarms.editor.EditorActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;
import java.util.GregorianCalendar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

/**
 * The main page of the app, showing a list of alarms/folders that the user can scroll through.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {
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

	// ********************************* Lifecycle Methods *********************************

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

		noAlarmsText = findViewById(R.id.noAlarmsText);
		fragContainer = findViewById(R.id.fragFrame);
		nextAlarmText = findViewById(R.id.next_alarm_text);

		int[] buttons = {R.id.addAlarmButton, R.id.addFolderButton, R.id.settingsButton};
		for (int i : buttons) {
			ImageButton b = findViewById(i);
			b.setOnClickListener(this);
			b.setOnLongClickListener(this);
		}

		// always need to reinflate the frag
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(R.id.fragFrame, RecyclerViewFrag.class, null, "recycler_frag");
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

		changeNextAlarm();
	}

	// ************************************  Callbacks  **************************************

	/**
	 * Callback for clicks. Starts a new activity for all current buttons.
	 * @param view the view that was clicked
	 */
	@Override
	public void onClick(@NotNull View view) {
		int id = view.getId();
		if (id == R.id.addAlarmButton) FolderViewActivity.addNewAlarm(this);
		else if (id == R.id.addFolderButton) FolderViewActivity.addNewFolder(this);
		else if (id == R.id.settingsButton) FolderViewActivity.openSettings(this);
	}

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

	// ************************************  Other Methods  *************************************

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
	 * Builds and displays the next alarm to ring text, regardless of whether it changed or not.
	 */
	private void changeNextAlarm() {
		Alarm nextAlarm = ((MainApplication)getApplication()).rootFolder.getCurrNextAlarm();

		String text;

		if (nextAlarm == null)
			text = getResources().getString(R.string.main_no_next_alarm);
		else {
			Calendar rightNow = new GregorianCalendar();

			String dateString = null;
			if (nextAlarm.getAlarmTimeCalendar().get(Calendar.DAY_OF_MONTH) == rightNow.get(Calendar.DAY_OF_MONTH)) {
				dateString = getResources().getString(R.string.main_date_string_today);
			}

			rightNow.add(Calendar.DAY_OF_MONTH, 1);
			if (nextAlarm.getAlarmTimeCalendar().get(Calendar.DAY_OF_MONTH) == rightNow.get(Calendar.DAY_OF_MONTH)) {
				dateString = getResources().getString(R.string.main_date_string_tomorrow);
			}

			if (dateString == null) {
				dateString = String.format(getResources().getString(R.string.main_date_string),
						DateFormat.getMediumDateFormat(this).format(nextAlarm.getAlarmTimeMillis()));
			}
			String timeString = DateFormat.getTimeFormat(this).format(nextAlarm.getAlarmTimeMillis());

			text = String.format(getResources().getString(R.string.main_next_alarm),
					nextAlarm.getName(), dateString, timeString);
		}
		nextAlarmText.setText(text);
	}
}
