package com.larmlarms.editor;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.larmlarms.BuildConfig;
import com.larmlarms.Constants;
import com.larmlarms.R;
import com.larmlarms.data.Alarm;
import com.larmlarms.data.AlarmGroup;
import com.larmlarms.data.Item;
import com.larmlarms.data.ItemInfo;
import com.larmlarms.data.RootFolder;
import com.larmlarms.main.MainApplication;
import com.larmlarms.main.PrefsActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

// TODO: set up folder structure via executor service?

/**
 * Activity used for creating new items or editing existing ones.
 *
 * Requirements for calling intents:
 * Must have an action defined in this class
 * For specific actions, see the action documentation for extra requirements
 */
public class EditorActivity extends AppCompatActivity
		implements AdapterView.OnItemSelectedListener, EditorDialogFrag.DialogCloseListener,
		SeekBar.OnSeekBarChangeListener {

	// **************************************  Constants  ***************************************
	
	/**
	 * Tag of the class for logging purposes.
	 */
	private final static String TAG = "Editor";

	/**
	 * Request code to get a ringtone for the app.
	 */
	private final static int REQ_GET_RINGTONE = 4;

	// ***********************************  Instance Fields  ************************************

	// information about item being edited
	/**
	 * The current item being edited/created in this activity. Is not null (after the activity
	 * is created).
	 */
	@NotNull
	private Item workingItem;
	/**
	 * The currently selected path of the current working item. Will be null unless the path was
	 * actually changed.
	 */
	@Nullable
	private String itemPath;

	// information about the original received item
	/**
	 * The original item received from the caller. Can be null if a new item is being made.
	 */
	@Nullable
	private Item originalItem;
	/**
	 * The original path of the current item.
	 */
	@Nullable
	private String originalPath;

	/**
	 * List of all possible paths.
	 */
	@Nullable
	private List<String> paths;

	// current editor mode
	/**
	 * Shows whether the current activity is editing an Alarm (true) or AlarmGroup (false).
	 */
	private boolean isAlarm;
	/**
	 * Shows whether the current activity is editing a item (true) or creating a new one (false).
	 */
	private boolean isEditing;

	// handles to views for repeat types (all are alarms only)
	/**
	 * The time picker (for alarms).
	 */
	@Nullable
	private TimePicker alarmTimePicker;
	/**
	 * The date picker (for alarms only).
	 */
	@Nullable
	private DatePicker alarmDatePicker;
	/**
	 * Layout for the clickable weekdays TextViews (for alarms only).
	 */
	@Nullable
	private ViewGroup alarmDaysLayout;
	/**
	 * Layout for the all offset fields (for alarms only).
	 */
	@Nullable
	private ViewGroup alarmOffsetLayout;
	/**
	 * Layout for the day and week to ring on (label and two spinners for which week and which day,
	 * for alarms only).
	 */
	@Nullable
	private ViewGroup alarmDayMonthlyLayout;
	/**
	 * Layout for the clickable months TextViews (for alarms only).
	 */
	@Nullable
	private ViewGroup alarmMonthsLayout;
	/**
	 * Layout for the day of the month to ring on (for alarms only).
	 */
	@Nullable
	private ViewGroup alarmDateOfMonthLayout;

	// *********************************  Lifecycle Methods  *******************************

	/**
	 * Creates a new editor and initializes some not null fields.
	 */
	public EditorActivity() {
		workingItem = new AlarmGroup();		// used in REQ_NEW_FOLDER, dummy data for all others
	}

	/**
	 * Called when the activity is first being created. Initializes fields and UI based on the type
	 * of item being edited.
	 * @param savedInstanceState the previously saved instance state from previous activity creation
	 */
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String action = intent.getAction();
		if (action == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Action passed to Editor is null.");
			finish();
			return;
		}

		// NOTE: this switch statement is only for setting up activity variables, NOT queuing any UI
		// changes, since the content view hasn't been set up yet.
		switch(action) {
			case Constants.ACTION_CREATE_ALARM:
				isAlarm = true;
				isEditing = false;
				workingItem = new Alarm(this);
				break;
			case Constants.ACTION_EDIT_ALARM:
				isAlarm = true;
				isEditing = true;
				break;
			case Constants.ACTION_CREATE_FOLDER:
				isAlarm = false;
				isEditing = false;
				// new AlarmGroup already created in constructor
				break;
			case Constants.ACTION_EDIT_FOLDER:
				isAlarm = false;
				isEditing = true;
				break;
			default:
				if (BuildConfig.DEBUG) Log.e(TAG, "Unknown request code!");
				finish();
				return;
		}

		if (isEditing) {
			Bundle extras = intent.getExtras();
			if (extras == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Extras passed to Editor are null.");
				finish();
				return;
			}

			ItemInfo info = extras.getParcelable(Constants.EXTRA_ITEM_INFO);

			if (info == null || info.item == null || info.path == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "item info is invalid.");
				finish();
				return;
			}
			workingItem = info.item;
			if (isAlarm) ((Alarm) workingItem).setContext(this);

			originalPath = info.path;
			originalItem = workingItem instanceof Alarm ?
					new Alarm((Alarm)workingItem) : new AlarmGroup((AlarmGroup)workingItem);
		}

		PrefsActivity.applyPrefsStyle(this);

		if (isAlarm) { alarmUISetup(); }
		else { folderUISetup(); }

		PrefsActivity.applyPrefsUI(this);

		// used for any action-specific UI changes
		switch(action) {
			case Constants.ACTION_EDIT_ALARM:
			case Constants.ACTION_EDIT_FOLDER:
				((EditText) findViewById(R.id.nameInput)).setText(workingItem.getName());
				break;
		}

		setupFolderStructure();
	}

	/**
	 * Called when the app is resuming from a paused state (or starting). Checks for 24 hour view
	 * and sets the time picker settings accordingly.
	 */
	@Override
	protected void onResume() {
		super.onResume();

		if (isAlarm && alarmTimePicker != null) {
			// matches the TimePicker's display mode to current system config (12 hr or 24 hr)
			alarmTimePicker.setIs24HourView(DateFormat.is24HourFormat(this));
		}
	}

	// **********************************  Button Callbacks  **********************************

	/**
	 * An onclick callback for the back button. Closes the Editor.
	 * @param view the back button (view that triggered the callback)
	 */
	public void backButtonClicked(@NotNull View view) {
		// TODO: ask user if they REALLY want to exit?
		finish();
	}

	/**
	 * An onclick callback for the save button. Turns currItem on and sends it back to the
	 * DataService to be saved. Exits the activity if the item was saved correctly or if some
	 * fatal error has been encountered.
	 * @param view the save button (view that triggered the callback)
	 */
	public void saveButtonClicked(@NotNull View view) {
		if (!saveItem()) return;

		ItemInfo data = new ItemInfo();
		RootFolder rootFolder = ((MainApplication) getApplication()).rootFolder;

		if (isEditing) {
			workingItem.turnOn();		// in case it was snoozed or something weird

			// check if the path was changed (makes it a move operation, regardless of if the
			// item itself changed)
			if (itemPath != null && !itemPath.equals(originalPath)) {
				data.item = workingItem;
				data.path = originalPath;
				rootFolder.moveItem(data, itemPath);
				return;
			}

			// check if the item itself was changed
			if (!workingItem.equals(originalItem)) {
				// path didn't change but item did, so use MSG_SET_LISTABLE
				data.item = originalItem;
				data.path = originalPath;
				rootFolder.setItemById(data, workingItem);
			}
		}
		else {
			// add the item
			data.item = workingItem;
			data.path = itemPath;
			rootFolder.addItem(data);
		}
		finish();
	}

	/**
	 * An onclick callback to change the days of the week to ring. Creates a dialog
	 * @param view the day of week button (view that triggered the callback)
	 */
	public void dayOfWeekButtonClicked(@NotNull View view) {
		EditorDialogFrag dialog = new EditorDialogFrag(this, true,
				((Alarm) workingItem).getRepeatDays());
		dialog.show(getSupportFragmentManager(), null);
	}

	/**
	 * An onclick callback if a TextView representing a month of the year is clicked.
	 * @param view the month TextView that was clicked
	 */
	public void monthsButtonClicked(@NotNull View view) {
		EditorDialogFrag dialog = new EditorDialogFrag(this, false,
				((Alarm) workingItem).getRepeatMonths());
		dialog.show(getSupportFragmentManager(), null);
	}

	/**
	 * Callback for the set ringtone button. Opens another app to choose the audio file.
	 * TODO: Perhaps make my own sound picker...?
	 * @param view the pick ringtone button (view that triggered the callback)
	 */
	public void chooseSound(@NotNull View view) {
		Intent getSound = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		getSound.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
				((Alarm) workingItem).getRingtoneUri());
		getSound.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
		startActivityForResult(getSound, REQ_GET_RINGTONE);
	}

	// *********************************  Spinner Callbacks  ***********************************

	/**
	 * Based on which repeat type was selected (using the position), will show/hide certain UI
	 * elements and change the repeatType of currItem.
	 *
	 * @param parent Spinner (AdapterView) that just closed
	 * @param view view within the AdapterView that was clicked
	 * @param pos position of the view within the Adapter
	 * @param id row ID of the item selected
	 */
	@Override
	public void onItemSelected(@NotNull AdapterView<?> parent, View view, int pos, long id) {
		int parentId = parent.getId();
		if (parentId == R.id.alarmRepeatTypeInput) {
			String type = (String) parent.getItemAtPosition(pos);
			if (type.equals(getString(R.string.repeat_once_abs)))
				changeRepeatType(Alarm.REPEAT_ONCE_ABS, false);
			else if (type.equals(getString(R.string.repeat_once_rel)))
				changeRepeatType(Alarm.REPEAT_ONCE_REL, false);
			else if (type.equals(getString(R.string.repeat_day_weekly)))
				changeRepeatType(Alarm.REPEAT_DAY_WEEKLY, false);
			else if (type.equals(getString(R.string.repeat_date_monthly)))
				changeRepeatType(Alarm.REPEAT_DATE_MONTHLY, false);
			else if (type.equals(getString(R.string.repeat_day_monthly)))
				changeRepeatType(Alarm.REPEAT_DAY_MONTHLY, false);
			else if (type.equals(getString(R.string.repeat_date_yearly)))
				changeRepeatType(Alarm.REPEAT_DATE_YEARLY, false);
			else if (type.equals(getString(R.string.repeat_offset)))
				changeRepeatType(Alarm.REPEAT_OFFSET, false);
		}
		else if (parentId == R.id.alarmWeekOfMonthInput) {
			((Alarm) workingItem).setRepeatWeek(pos);
		}
		else if (parentId == R.id.alarmDayOfWeekInput) {
			((Alarm) workingItem).getAlarmTimeCalendar().set(Calendar.DAY_OF_WEEK, pos + 1);
		}
		else if (parentId == R.id.parentFolderInput) {
			if (paths != null) itemPath = paths.get(pos);
		}
		else {
			if (BuildConfig.DEBUG) Log.e(TAG, "Unknown AdapterView selected an item.");
			finish();
		}
	}

	/**
	 * Doesn't do anything when nothing has been selected from the repeat type dropdown.
	 * @param parent Spinner (AdapterView) that just closed
	 */
	@Override
	public void onNothingSelected(@NotNull AdapterView<?> parent) {}

	// ***********************************  SeekBar Callbacks  **********************************

	/**
	 * Callback for SeekBar, called whenever the volume has changed. Unused.
	 * @param seekBar the SeekBar whose progress changed
	 * @param progress the current progress level
	 * @param fromUser whether the progress change was user-initiated or not
	 */
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
	/**
	 * Callback for SeekBar, called whenever the user has started changing the bar. Unused.
	 * @param seekBar the SeekBar whose progress changed
	 */
	public void onStartTrackingTouch(SeekBar seekBar) {}

	/**
	 * Callback for SeekBar, called whenever the user has stopped changing the bar. Sets the volume
	 * of the current alarm.
	 * @param seekBar the SeekBar whose progress changed
	 */
	public void onStopTrackingTouch(SeekBar seekBar) {
		((Alarm) workingItem).setVolume(seekBar.getProgress());
	}

	// ***********************************  Other Callbacks  ***********************************

	/**
	 * Callback for EditorDialogFrag dialogs for when they close
	 * @param isDays whether it was a days or months dialog
	 * @param which which button was clicked
	 */
	public void onDialogClose(boolean isDays, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			if (isDays) {
				if (alarmDaysLayout == null) {
					if (BuildConfig.DEBUG) Log.e(TAG, "Alarm days layout was null when the dialog for it closed.");
					return;
				}
				((TextView) alarmDaysLayout.findViewById(R.id.text))
						.setText(((Alarm) workingItem).getWeeklyDisplayString());
			}
			else {
				if (alarmMonthsLayout == null) {
					if (BuildConfig.DEBUG) Log.e(TAG, "Alarm days layout was null when the dialog for it closed.");
					return;
				}
				((TextView) alarmMonthsLayout.findViewById(R.id.text))
						.setText(((Alarm) workingItem).getMonthsString());
			}
		}
	}

	/**
	 * Callback for when a compound button (checkbox or switch) is clicked.
	 * @param view the compound button that was clicked
	 */
	public void onCompoundButtonClicked(@NotNull View view) {
		boolean checked = ((CompoundButton) view).isChecked();

		int id = view.getId();
		if (id == R.id.alarmOffsetFromNowCheckbox) {
			((Alarm) workingItem).setOffsetFromNow(checked);

			if (checked) {
				// hide the time and date pickers
				if (alarmTimePicker != null) alarmTimePicker.setVisibility(View.GONE);
				if (alarmDatePicker != null) alarmDatePicker.setVisibility(View.GONE);
			} else {
				// show the time and date pickers
				if (alarmTimePicker == null) setupTimePicker();
				alarmTimePicker.setVisibility(View.VISIBLE);

				if (alarmDatePicker == null) {
					setupDatePicker();
					alarmDatePicker.setMinDate(0);        // in case the date picker hadn't been set up yet
				}
				alarmDatePicker.setVisibility(View.VISIBLE);
			}
		}
		else if (id == R.id.alarmVibrateSwitch) {
			((Alarm) workingItem).setVibrateOn(checked);
		}
	}

	/**
	 * Callback called after a ringtone is chosen.
	 * @param requestCode the code we sent when creating the activity
	 * @param resultCode the code the activity sent back after finishing
	 * @param data the data sent back by the activity
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQ_GET_RINGTONE && resultCode == RESULT_OK) {
			Uri uri = null;
			if (data != null)
				uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

			((Alarm) workingItem).setRingtoneUri(uri);

			TextView t = findViewById(R.id.soundText);
			t.setText(((Alarm) workingItem).getRingtoneName());
		}
	}

	// ***************************************  UI Setup  **************************************

	/**
	 * Sets up the UI for editing (or creating) an alarm.
	 */
	private void alarmUISetup() {
		setContentView(R.layout.editor_alarm);
		changeRepeatType(((Alarm) workingItem).getRepeatType(), true);

		Spinner spinner;
		ArrayAdapter<CharSequence> adapter;

		// repeatType spinner
		spinner = findViewById(R.id.alarmRepeatTypeInput);
		adapter = ArrayAdapter.createFromResource(this,
				R.array.alarm_editor_repeat_strings, android.R.layout.simple_spinner_dropdown_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		int typeIndex = 0;
		switch (((Alarm) workingItem).getRepeatType()) {
			case Alarm.REPEAT_ONCE_ABS:
				typeIndex = getResources().getInteger(R.integer.repeat_once_abs);
				break;
			case Alarm.REPEAT_ONCE_REL:
				typeIndex = getResources().getInteger(R.integer.repeat_once_rel);
				break;
			case Alarm.REPEAT_DAY_WEEKLY:
				typeIndex = getResources().getInteger(R.integer.repeat_day_weekly);
				break;
			case Alarm.REPEAT_DATE_MONTHLY:
				typeIndex = getResources().getInteger(R.integer.repeat_date_monthly);
				break;
			case Alarm.REPEAT_DAY_MONTHLY:
				typeIndex = getResources().getInteger(R.integer.repeat_day_monthly);
				break;
			case Alarm.REPEAT_DATE_YEARLY:
				typeIndex = getResources().getInteger(R.integer.repeat_date_yearly);
				break;
			case Alarm.REPEAT_OFFSET:
				typeIndex = getResources().getInteger(R.integer.repeat_offset);
				break;
		}
		spinner.setSelection(typeIndex);
		spinner.setOnItemSelectedListener(this);

		// set name of the current ringtone
		TextView alarmSoundLabel = findViewById(R.id.soundText);
		alarmSoundLabel.setText(((Alarm) workingItem).getRingtoneName());

		// set the volume bar to the current volume and register listeners
		SeekBar volumeBar = findViewById(R.id.volumeSeekBar);
		volumeBar.setProgress(((Alarm) workingItem).getVolume());
		volumeBar.setOnSeekBarChangeListener(this);

		// vibrate switch
		SwitchMaterial vibrateSwitch = findViewById(R.id.alarmVibrateSwitch);
		// vibrateSwitch.setOnClickListener(this);
		vibrateSwitch.setChecked(((Alarm) workingItem).isVibrateOn());
	}

	/**
	 * Sets up the UI for editing (or creating) a folder.
	 */
	private void folderUISetup() { setContentView(R.layout.editor_folder); }

	/**
	 * Sets up the time picker for the first time. Sets the field and UI (hours/minutes on picker).
	 * Does not check whether the field is null or not. For alarms only.
	 */
	private void setupTimePicker() {
		int timePickerHour, timePickerMin;
		Calendar sysClock = Calendar.getInstance();

		if (isEditing) {
			// sets it to the current alarm time first
			timePickerHour = ((Alarm) workingItem).getAlarmTimeCalendar().get(Calendar.HOUR_OF_DAY);
			timePickerMin = ((Alarm) workingItem).getAlarmTimeCalendar().get(Calendar.MINUTE);
		}
		else {
			// matches the TimePicker time to current system time, should only be once
			timePickerHour = sysClock.get(Calendar.HOUR_OF_DAY);
			timePickerMin = sysClock.get(Calendar.MINUTE);
		}

		alarmTimePicker = findViewById(R.id.alarmTimeInput);
		if (alarmTimePicker == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Time picker couldn't be found.");
			return;
		}
		alarmTimePicker.setIs24HourView(DateFormat.is24HourFormat(this));

		// editing alarmTimePicker to match hour/min
		alarmTimePicker.setCurrentHour(timePickerHour);
		alarmTimePicker.setCurrentMinute(timePickerMin);
	}

	/**
	 * Sets up the date picker for the first time. Sets up both the field and the UI. Does not check
	 * whether the field is null or not. For alarms only.
	 */
	private void setupDatePicker() {
		// used temp variable because otherwise the IDE says there's a possible null ptr exception
		DatePicker picker = findViewById(R.id.alarmDateInput);
		picker.setMinDate(Calendar.getInstance().getTimeInMillis());
		// picker.updateDate();
		Calendar c = ((Alarm) workingItem).getAlarmTimeCalendar();
		if (isEditing) {
			// sets it to the current alarm time first
			picker.updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
		}

		alarmDatePicker = picker;
	}

	/**
	 * Sets up the days of the week field (alarmDaysLayout) and corresponding UI (text). Does
	 * not check whether the field is null or not. For alarms only.
	 */
	private void setupWeekDays() {
		alarmDaysLayout = findViewById(R.id.alarmDaysInput);

		((TextView) alarmDaysLayout.findViewById(R.id.text))
				.setText(((Alarm) workingItem).getWeeklyDisplayString());
	}

	/**
	 * Sets up the months field (alarmMonthsLayout) and corresponding UI (text). Does not
	 * check whether the field is null or not. For alarms only.
	 */
	private void setupMonths() {
		alarmMonthsLayout = findViewById(R.id.alarmMonthsInput);

		((TextView) alarmMonthsLayout.findViewById(R.id.text))
				.setText(((Alarm) workingItem).getMonthsString());
	}


	/**
	 * Sets up the day monthly layout for the first time. Sets the field and the UI (spinners). Does
	 * not check whether the field is null or not. For alarms only.
	 */
	private void setupDayMonthlyLayout() {
		alarmDayMonthlyLayout = findViewById(R.id.alarmDayMonthly);

		Spinner spinner;
		ArrayAdapter<CharSequence> adapter;

		// week of month spinner
		spinner = alarmDayMonthlyLayout.findViewById(R.id.alarmWeekOfMonthInput);
		adapter = ArrayAdapter.createFromResource(this,
				R.array.alarm_week_strings, android.R.layout.simple_spinner_dropdown_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(((Alarm) workingItem).getRepeatWeek());
		spinner.setOnItemSelectedListener(this);

		// day of week spinner
		spinner = alarmDayMonthlyLayout.findViewById(R.id.alarmDayOfWeekInput);
		adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
		adapter.addAll((new DateFormatSymbols()).getWeekdays());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(((Alarm) workingItem).getAlarmTimeCalendar().get(Calendar.DAY_OF_WEEK) - 1);
		spinner.setOnItemSelectedListener(this);
	}

	/**
	 * Handles setup of the folder spinner. Currently does this synchronously.
	 */
	private void setupFolderStructure() {
		paths = ((MainApplication)getApplication()).rootFolder.toPathList();

		if (isEditing && !isAlarm) {
			if (originalItem == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Can't setup folder structure without the original item being valid.");
				return;
			}
			String folderPath = originalPath + originalItem.getName() + '/';
			for (int i = paths.size() - 1; i > 0; i--) {
				if (paths.get(i).startsWith(folderPath)) paths.remove(i);
			}
		}

		Spinner spinner = findViewById(R.id.parentFolderInput);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
		adapter.addAll(paths);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		int index;
		if (itemPath == null) index = paths.indexOf(originalPath);
		else index = paths.indexOf(itemPath);
		if (index == -1) index = 0;

		spinner.setSelection(index);
		spinner.setOnItemSelectedListener(this);
	}

	// ************************************  Other Methods  *********************************

	/**
	 * Changes repeat type to the new specified type. Changes both the working alarm type and the UI
	 * layouts. For alarms only.
	 * @param type new type to set it to, should be one of the Alarm constants
	 * @param first whether the repeat type is the first repeat type to be initialized or not
	 */
	@SuppressLint("DefaultLocale")
	private void changeRepeatType(int type, boolean first) {
		if (!first) {
			switch(((Alarm) workingItem).getRepeatType()) {
				case Alarm.REPEAT_DATE_YEARLY:
				case Alarm.REPEAT_ONCE_ABS:
					// requires: time picker, date picker
					if (alarmTimePicker == null || alarmDatePicker == null) {
						if (BuildConfig.DEBUG) Log.wtf(TAG, "The alarm date/time pickers are null.");
						finish();
						return;
					}
					alarmTimePicker.setVisibility(View.GONE);
					alarmDatePicker.setVisibility(View.GONE);
					break;
				case Alarm.REPEAT_ONCE_REL:
				case Alarm.REPEAT_OFFSET:
					// requires: offset fields (days, hours, minutes), maybe date/time pickers
					if (alarmOffsetLayout == null) {
						if (BuildConfig.DEBUG) Log.wtf(TAG, "The alarm offsets layouts are null.");
						finish();
						return;
					}
					alarmOffsetLayout.setVisibility(View.GONE);
					break;
				case Alarm.REPEAT_DAY_WEEKLY:
					// requires: time picker, list of days (clickable text)
					if (alarmTimePicker == null || alarmDaysLayout == null) {
						if (BuildConfig.DEBUG) Log.wtf(TAG, "The alarm time picker or days of week layout is null.");
						finish();
						return;
					}
					alarmTimePicker.setVisibility(View.GONE);
					alarmDaysLayout.setVisibility(View.GONE);
					break;
				case Alarm.REPEAT_DATE_MONTHLY:
					// requires: time picker, date of month (number picker), list of months (clickable text)
					if (alarmTimePicker == null || alarmMonthsLayout == null ||
							alarmDateOfMonthLayout == null) {
						if (BuildConfig.DEBUG) Log.wtf(TAG, "The alarm time picker, months layout, or date of month layout is null.");
						finish();
						return;
					}
					alarmTimePicker.setVisibility(View.GONE);
					alarmMonthsLayout.setVisibility(View.GONE);
					alarmDateOfMonthLayout.setVisibility(View.GONE);
					break;
				case Alarm.REPEAT_DAY_MONTHLY:
					// requires: time picker, list of months, date of month (number picker), spinner for the week #
					if (alarmTimePicker == null || alarmDayMonthlyLayout == null ||
							alarmMonthsLayout == null) {
						if (BuildConfig.DEBUG) Log.wtf(TAG, "The alarm time picker, day of the month, or months layout is null.");
						finish();
						return;
					}
					alarmTimePicker.setVisibility(View.GONE);
					alarmDayMonthlyLayout.setVisibility(View.GONE);
					alarmMonthsLayout.setVisibility(View.GONE);
					break;
				default:
					if (BuildConfig.DEBUG) Log.e(TAG, "Invalid previous alarm type selected.");
					finish();
			}
		}

		if (alarmDatePicker != null) {
			// gotta set it back to the right minimum date
			if (type == Alarm.REPEAT_ONCE_REL || type == Alarm.REPEAT_OFFSET)
				alarmDatePicker.setMinDate(0);
			else
				alarmDatePicker.setMinDate(Calendar.getInstance().getTimeInMillis());
		}

		switch(type) {
			case Alarm.REPEAT_DATE_YEARLY:
			case Alarm.REPEAT_ONCE_ABS:
				// requires: time picker, date picker
				if (alarmTimePicker == null) setupTimePicker();
				alarmTimePicker.setVisibility(View.VISIBLE);

				if (alarmDatePicker == null) setupDatePicker();
				alarmDatePicker.setVisibility(View.VISIBLE);
				break;
			case Alarm.REPEAT_OFFSET:
			case Alarm.REPEAT_ONCE_REL:
				// requires: offset fields (days, hours, minutes)
				// only really need to check one offset field, since they essentially come as a set
				if (alarmOffsetLayout == null) {
					EditText curr;
					int temp;

					alarmOffsetLayout = findViewById(R.id.alarmOffsetLayout);
					curr = alarmOffsetLayout.findViewById(R.id.alarmOffsetDaysInput);
					temp = ((Alarm) workingItem).getOffsetDays();
					if (temp != 0) curr.setText(String.format("%d", temp));

					curr = alarmOffsetLayout.findViewById(R.id.alarmOffsetHoursInput);
					temp = ((Alarm) workingItem).getOffsetHours();
					if (temp != 0) curr.setText(String.format("%d", temp));

					curr = alarmOffsetLayout.findViewById(R.id.alarmOffsetMinsInput);
					temp = ((Alarm) workingItem).getOffsetMins();
					if (temp != 0) curr.setText(String.format("%d", temp));

					if (!((Alarm) workingItem).isOffsetFromNow()) {
						CheckBox checkBox = alarmOffsetLayout.findViewById(R.id.alarmOffsetFromNowCheckbox);
						checkBox.setChecked(false);

						// date/time picker setup
						// calculate previous offset from time
						Calendar c = (Calendar) ((Alarm) workingItem).getAlarmTimeCalendar().clone();
						c.add(Calendar.DAY_OF_MONTH, -((Alarm) workingItem).getOffsetDays());
						c.add(Calendar.HOUR_OF_DAY, -((Alarm) workingItem).getOffsetHours());
						c.add(Calendar.MINUTE, -((Alarm) workingItem).getOffsetMins());

						if (alarmTimePicker == null) setupTimePicker();
						alarmTimePicker.setCurrentHour(c.get(Calendar.HOUR_OF_DAY));
						alarmTimePicker.setCurrentMinute(c.get(Calendar.MINUTE));

						if (alarmDatePicker == null) setupDatePicker();
						alarmDatePicker.updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH),
								c.get(Calendar.DAY_OF_MONTH));
						alarmDatePicker.setMinDate(0);
					}
				}

				if (!((Alarm) workingItem).isOffsetFromNow()) {
					if (alarmTimePicker == null) setupTimePicker();
					alarmTimePicker.setVisibility(View.VISIBLE);

					alarmDatePicker.setMinDate(0);
					alarmDatePicker.setVisibility(View.VISIBLE);
				}

				alarmOffsetLayout.setVisibility(View.VISIBLE);
				break;
			case Alarm.REPEAT_DAY_WEEKLY:
				// requires: time picker, list of days (clickable text)
				if (alarmTimePicker == null) setupTimePicker();
				alarmTimePicker.setVisibility(View.VISIBLE);

				if (alarmDaysLayout == null) setupWeekDays();
				alarmDaysLayout.setVisibility(View.VISIBLE);
				break;
			case Alarm.REPEAT_DATE_MONTHLY:
				// requires: time picker, date of month (number picker), list of months (clickable text)
				if (alarmTimePicker == null) setupTimePicker();
				alarmTimePicker.setVisibility(View.VISIBLE);

				if (alarmMonthsLayout == null) setupMonths();
				alarmMonthsLayout.setVisibility(View.VISIBLE);

				if (alarmDateOfMonthLayout == null) {
					alarmDateOfMonthLayout = findViewById(R.id.alarmDateOfMonth);

					// day of month number picker
					NumberPicker dateOfMonth = alarmDateOfMonthLayout.findViewById(R.id.alarmDateOfMonthInput);
					dateOfMonth.setMinValue(1);
					dateOfMonth.setMaxValue(31);
					dateOfMonth.setValue(((Alarm) workingItem).getAlarmTimeCalendar().get(Calendar.DAY_OF_MONTH));
				}
				alarmDateOfMonthLayout.setVisibility(View.VISIBLE);
				break;
			case Alarm.REPEAT_DAY_MONTHLY:
				// requires: time picker, list of months, date of month (number picker), spinner for the week #
				if (alarmTimePicker == null) setupTimePicker();
				alarmTimePicker.setVisibility(View.VISIBLE);

				if (alarmDayMonthlyLayout == null) setupDayMonthlyLayout();
				alarmDayMonthlyLayout.setVisibility(View.VISIBLE);

				if (alarmMonthsLayout == null) setupMonths();
				alarmMonthsLayout.setVisibility(View.VISIBLE);
				break;
			default:
				if (BuildConfig.DEBUG) Log.e(TAG, "Invalid alarm type selected.");
				finish();
		}
		((Alarm) workingItem).setRepeatType(type);
	}

	/**
	 * Saves information from activity fields to currItem. Does NOT turn currItem on
	 * if it was originally off. If an error is encountered, will ither create a Toast to tell the
	 * user how to fix it or close the activity using exitActivity(), based on error severity.
	 * @return whether the item was saved without error
	 */
	public boolean saveItem() {
		// common fields
		String newName = ((EditText) findViewById(R.id.nameInput)).getText().toString();
		int errorCode = workingItem.setName(newName);

		if (newName.equals("")) errorCode = 1;
		if (errorCode != 0) {
			String toastErrorText;

			switch(errorCode) {
				case 1:
					// error: name is null/empty
					if (isAlarm) toastErrorText = getResources().getString(R.string.alarm_editor_toast_empty);
					else toastErrorText = getResources().getString(R.string.folder_editor_toast_empty);
					break;
				case 2:
					// error: name has tabs or slashes
					if (isAlarm) toastErrorText = getResources().getString(R.string.alarm_editor_toast_restricted);
					else toastErrorText = getResources().getString(R.string.folder_editor_toast_restricted);
					break;
				default:
					// error: unknown request code!
					if (BuildConfig.DEBUG) Log.e(TAG, "Unknown setName() error code! Exiting activity...");
					finish();
					return false;
			}

			Toast.makeText(this, toastErrorText, Toast.LENGTH_SHORT).show();
			return false;		// don't exit, let the user fix it
		}

		// checking for folders of the same name
		if (!isAlarm) {
			// building future folder path
			String newPath = itemPath;
			if (itemPath == null) newPath = originalPath;
			newPath += '/' + newName;

			if (paths == null || paths.contains(newPath)) {
				Toast.makeText(this, getResources().getString(R.string.folder_editor_toast_duplicate),
						Toast.LENGTH_SHORT).show();
				return false;		// don't exit, let the user fix it
			}
		}

		// saving alarm time data
		if (isAlarm) {
			((Alarm) workingItem).unsnooze();
			Calendar alarmCalendar = ((Alarm) workingItem).getAlarmTimeCalendar();

			switch(((Alarm) workingItem).getRepeatType()) {
				case Alarm.REPEAT_ONCE_ABS:
				case Alarm.REPEAT_DATE_YEARLY:
					if (!pickerToCalendar(alarmCalendar)) return false;
					break;
				case Alarm.REPEAT_ONCE_REL:
				case Alarm.REPEAT_OFFSET:
					if (alarmOffsetLayout == null) {
						if (BuildConfig.DEBUG) Log.wtf(TAG, "The alarm offset layouts are null.");
						finish();
						return false;
					}

					EditText currEditText;
					int days, hours, mins;
					NumberFormat format = NumberFormat.getInstance();
					format.setParseIntegerOnly(true);
					Number n;

					currEditText = alarmOffsetLayout.findViewById(R.id.alarmOffsetDaysInput);
					try {
						n = format.parse(currEditText.getText().toString());
						if (n != null) days = n.intValue();
						else {
							if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't parse the days to offset. Defaulting to 0.");
							days = 0;
						}
					} catch (ParseException e) {
						if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't parse the days to offset. Defaulting to 0.");
						days = 0;
					}

					currEditText = alarmOffsetLayout.findViewById(R.id.alarmOffsetHoursInput);
					try {
						n = format.parse(currEditText.getText().toString());
						if (n != null) hours = n.intValue();
						else {
							if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't parse the hours to offset. Defaulting to 0.");
							hours = 0;
						}
					} catch (ParseException e) {
						if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't parse the hours to offset. Defaulting to 0.");
						hours = 0;
					}

					currEditText = alarmOffsetLayout.findViewById(R.id.alarmOffsetMinsInput);
					try {
						n = format.parse(currEditText.getText().toString());
						if (n != null) mins = n.intValue();
						else {
							if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't parse the minutes to offset. Defaulting to 0.");
							mins = 0;
						}
					} catch (ParseException e) {
						if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't parse the minutes to offset. Defaulting to 0.");
						mins = 0;
					}

					if (days + hours + mins == 0) {
						Toast.makeText(this, getResources().getString(R.string.alarm_editor_toast_offset),
								Toast.LENGTH_SHORT).show();
						return false;		// don't exit, let the user fix it
					}

					Calendar newCalendar = Calendar.getInstance();

					// offset from the given date/time
					if (!((Alarm) workingItem).isOffsetFromNow()) pickerToCalendar(newCalendar);

					if (((Alarm) workingItem).getRepeatType() == Alarm.REPEAT_ONCE_REL) {
						newCalendar.add(Calendar.DAY_OF_MONTH, days);
						newCalendar.add(Calendar.HOUR_OF_DAY, hours);
						newCalendar.add(Calendar.MINUTE, mins);
					}

					hours = hours + mins/60;
					mins = mins % 60;
					days = days + hours/24;
					hours = hours % 24;

					((Alarm) workingItem).setOffsetDays(days);
					((Alarm) workingItem).setOffsetHours(hours);
					((Alarm) workingItem).setOffsetMins(mins);

					((Alarm) workingItem).setAlarmTimeMillis(newCalendar.getTimeInMillis());
					break;
				case Alarm.REPEAT_DATE_MONTHLY:
					if (alarmDateOfMonthLayout == null) {
						if (BuildConfig.DEBUG) Log.wtf(TAG, "The alarm date of month is null.");
						finish();
						return false;
					}

					NumberPicker picker = alarmDateOfMonthLayout.findViewById(R.id.alarmDateOfMonthInput);
					alarmCalendar.set(Calendar.DAY_OF_MONTH, picker.getValue());
					// continues onto the next case
				case Alarm.REPEAT_DAY_WEEKLY:
				case Alarm.REPEAT_DAY_MONTHLY:
					if (alarmTimePicker == null) {
						if (BuildConfig.DEBUG) Log.wtf(TAG, "The alarm time picker is null.");
						finish();
						return false;
					}

					alarmCalendar.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
					alarmCalendar.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());

					alarmCalendar.set(Calendar.SECOND, 0);
					alarmCalendar.set(Calendar.MILLISECOND, 0);
					break;
				default:
					if (BuildConfig.DEBUG) Log.e(TAG, "The alarm has an invalid repeat type.");
					finish();
					return false;
			}

			((Alarm) workingItem).updateRingTime();
		}

		return true;
	}

	/**
	 * Saves the current contents of the date/time pickers into the specified Calendar object. Will
	 * also set the seconds and milliseconds to zero.
	 * @param c the calendar to save the date/time to
	 * @return whether the method successfully saved picker data to the calendar or not
	 */
	private boolean pickerToCalendar(Calendar c) {
		if (alarmDatePicker == null || alarmTimePicker == null) {
			if (BuildConfig.DEBUG) Log.wtf(TAG, "The alarm date/time pickers are null.");
			finish();
			return false;
		}

		c.set(Calendar.YEAR, alarmDatePicker.getYear());
		c.set(Calendar.MONTH, alarmDatePicker.getMonth());
		c.set(Calendar.DAY_OF_MONTH, alarmDatePicker.getDayOfMonth());

		c.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
		c.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());

		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return true;
	}
}
