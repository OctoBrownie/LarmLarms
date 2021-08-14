package com.apps.LarmLarms;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.media.RingtoneManager;
import android.net.Uri;
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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity used for creating new Listables or editing existing ones.
 */
public class ListableEditorActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

	/* **************************************  Constants  *************************************** */

	/**
	 * Tag of the class for logging purposes.
	 */
	private final static String TAG = "ListableEditor";

	// extras for intents carrying alarms
	/**
	 * An extra used for carrying a Listable in edit string form. 
	 */
	final static String EXTRA_LISTABLE = "com.apps.AlarmsButBetter.LISTABLE";
	/**
	 * An extra used for carrying a Listable's absolute index within the data. 
	 */
	final static String EXTRA_LISTABLE_INDEX = "com.apps.AlarmsButBetter.ABS_INDEX";
	/**
	 * An extra used for carrying a ListableInfo.
	 */
	final static String EXTRA_LISTABLE_INFO = "com.apps.AlarmsButBetter.INFO";
	/**
	 * An extra with the request ID the activity is being called for. The editor does something
	 * different based on what ID is received.
	 */
	final static String EXTRA_REQ_ID = "com.apps.AlarmsButBetter.REQ_ID";

	// for inbound intents
	/**
	 * Request code to create a new alarm. 
	 * TODO: could possibly make into an intent action instead 
	 */
	final static int REQ_NEW_ALARM = 0;
	/**
	 * Request code to edit an existing Alarm. Assumes that EXTRA_LISTABLE, EXTRA_LISTABLE_INDEX, 
	 * and EXTRA_FOLDERS are filled out. 
	 */
	final static int REQ_EDIT_ALARM = 1;
	/**
	 * Request code to create a new folder.
	 */
	final static int REQ_NEW_FOLDER = 2;
	/**
	 * Request code to edit an existing AlarmGroup. Assumes that EXTRA_LISTABLE, EXTRA_LISTABLE_INDEX, 
	 * and EXTRA_FOLDERS are filled out. 
	 */
	final static int REQ_EDIT_FOLDER = 3;

	// for outbound intents
	/**
	 * Request code to get a ringtone for the app. 
	 */
	private final static int REQ_GET_RINGTONE = 4;

	/* ***********************************  Instance Fields  ************************************ */

	/**
	 * The current Listable being edited/created in this activity. Is not null (after the activity
	 * is created).
	 */
	@NotNull
	private Listable workingListable;

	// information about Listable being edited
	/**
	 * The path of the current working Listable.
	 */
	@Nullable
	private String listablePath;
	/**
	 * The absolute index of the Listable being edited.
	 */
	private int listableIndex;

	// current editor mode
	/**
	 * Shows whether the current activity is editing an Alarm (true) or AlarmGroup (false).
	 */
	private boolean isEditingAlarm;
	/**
	 * Shows whether the current activity is editing a Listable (true) or creating a new one (false).
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
	 * Layout for the offset days label and text field (for alarms only).
	 */
	@Nullable
	private ViewGroup alarmOffsetDaysLayout;
	/**
	 * Layout for the offset hours label and text field (for alarms only).
	 */
	@Nullable
	private ViewGroup alarmOffsetHoursLayout;
	/**
	 * Layout for the offset minutes label and text field (for alarms only).
	 */
	@Nullable
	private ViewGroup alarmOffsetMinsLayout;
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

	// data service fields
	/**
	 * Shows whether it is bound to the data service or not.
	 */
	private boolean boundToDataService = false;
	/**
	 * The service connection to the data service.
	 */
	@NotNull
	private DataServiceConnection dataConn;

	/* *********************************  Lifecycle Methods  ******************************* */

	/**
	 * Creates a new ListableEditor and initializes some fields.
	 */
	public ListableEditorActivity() {
		dataConn = new DataServiceConnection();
		workingListable = new AlarmGroup();		// used in REQ_NEW_FOLDER, dummy data for all others
	}

	/**
	 * Called when the activity is first being created. Initializes fields and UI based on the type
	 * of Listable being edited.
	 * @param savedInstanceState the previously saved instance state from previous activity creation
	 */
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			Log.e(TAG, "Extras passed to ListableEditor are null.");
			exitActivity();
			return;
		}
		int startedState = extras.getInt(EXTRA_REQ_ID, -1);

		// NOTE: this switch statement is only for setting up activity variables, NOT queuing any UI
		// changes, since the content view hasn't been set up yet.
		switch(startedState) {
			case REQ_NEW_ALARM:
				isEditingAlarm = true;
				isEditing = false;
				workingListable = new Alarm(this);
				Log.v(TAG, "Creating a new alarm.");
				break;
			case REQ_EDIT_ALARM:
				isEditingAlarm = true;
				isEditing = true;
				Log.v(TAG, "Editing an existing alarm.");
				break;
			case REQ_NEW_FOLDER:
				isEditingAlarm = false;
				isEditing = false;
				// already created in constructor
				Log.v(TAG, "Creating a new folder.");
				break;
			case REQ_EDIT_FOLDER:
				isEditingAlarm = false;
				isEditing = true;
				Log.v(TAG, "Editing an existing folder.");
				break;
			default:
				Log.e(TAG, "Unknown request code!");
				exitActivity();
				return;
		}

		if (isEditing) {
			ListableInfo info = extras.getParcelable(EXTRA_LISTABLE_INFO);

			if (info == null || info.listable == null || info.path == null) {
				Log.e(TAG, "Listable info is invalid.");
				exitActivity();
				return;
			}
			listableIndex = info.absIndex;
			workingListable = info.listable;
			listablePath = info.path;
		}

		if (isEditingAlarm) { alarmUISetup(); }
		else { folderUISetup(); }

		// switch statement is used for any REQ-specific UI changes
		switch(startedState) {
			case REQ_EDIT_ALARM:
			case REQ_EDIT_FOLDER:
				((EditText) findViewById(R.id.nameInput)).setText(workingListable.getListableName());
				break;
		}
	}

	/**
	 * Called when the fragment is being started. Binds to the data service.
	 */
	@Override
	protected void onStart() {
		super.onStart();

		bindService(new Intent(this, AlarmDataService.class), dataConn, Context.BIND_AUTO_CREATE);
	}

	/**
	 * Called when the app is resuming from a paused state (or starting). Checks for 24 hour view
	 * and sets the time picker settings accordingly.
	 */
	@Override
	protected void onResume() {
		super.onResume();

		if (isEditingAlarm && alarmTimePicker != null) {
			// matches the TimePicker's display mode to current system config (12 hr or 24 hr)
			alarmTimePicker.setIs24HourView(DateFormat.is24HourFormat(this));
		}
	}


	/**
	 * Called when the app is stopping. Unbinds from the data service if it hasn't already.
	 */
	@Override
	protected void onStop() {
		super.onStop();

		if (boundToDataService) {
			unbindService(dataConn);
			boundToDataService = false;
		}
	}

	/* ************************************  Callbacks  ************************************* */

	/**
	 * An onclick callback for the back button. Closes the AlarmCreator and return
	 * RESULT_CANCELLED and no intent.
	 * @param view the back button (view that triggered the callback)
	*/
	public void backButtonClicked(@NotNull View view) {
		Log.i(TAG, "Back button pressed. No changes made to any alarms. Exiting.");

		// TODO: ask user if they REALLY want to exit?
		exitActivity();
	}

	/**
	 * An onclick callback for the save button. If an error is encountered, will close the activity
	 * using exitActivity(). If no error, will return RESULT_OK and the Listable (in string form) in
	 * the intent.
	 * @param view the save button (view that triggered the callback)
	*/
	public void saveListable(@NotNull View view) {
		// TODO: validate any field inputs?
		Log.i(TAG, "Sending new listable back to the caller.");

		// common fields
		EditText nameInput = findViewById(R.id.nameInput);
		workingListable.setListableName(nameInput.getText().toString());
		workingListable.turnOn();

		// saving alarm time data
		if (isEditingAlarm) {
			((Alarm) workingListable).unsnooze();
			Calendar alarmCalendar = ((Alarm) workingListable).getAlarmTimeCalendar();

			switch(((Alarm) workingListable).getRepeatType()) {
				case Alarm.REPEAT_ONCE_ABS:
				case Alarm.REPEAT_DATE_YEARLY:
					if (alarmDatePicker == null || alarmTimePicker == null) {
						Log.wtf(TAG, "The alarm date/time pickers are null.");
						exitActivity();
						return;
					}

					alarmCalendar.set(Calendar.YEAR, alarmDatePicker.getYear());
					alarmCalendar.set(Calendar.MONTH, alarmDatePicker.getMonth());
					alarmCalendar.set(Calendar.DAY_OF_MONTH, alarmDatePicker.getDayOfMonth());

					alarmCalendar.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
					alarmCalendar.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());
					break;
				case Alarm.REPEAT_ONCE_REL:
				case Alarm.REPEAT_OFFSET:
					if (alarmOffsetDaysLayout == null || alarmOffsetHoursLayout == null ||
							alarmOffsetMinsLayout == null) {
						Log.wtf(TAG, "The alarm offset layouts are null.");
						exitActivity();
						return;
					}

					EditText currEditText;
					int days, hours, mins;
					NumberFormat format = NumberFormat.getInstance();
					format.setParseIntegerOnly(true);

					currEditText = alarmOffsetDaysLayout.findViewById(R.id.alarmOffsetDaysInput);
					try {
						days = format.parse(currEditText.getText().toString()).intValue();
					} catch (ParseException e) {
						Log.e(TAG, "Couldn't parse the days to offset.");
						days = 0;
					}

					currEditText = alarmOffsetHoursLayout.findViewById(R.id.alarmOffsetHoursInput);
					try {
						hours = format.parse(currEditText.getText().toString()).intValue();
					} catch (ParseException e) {
						Log.e(TAG, "Couldn't parse the hours to offset.");
						hours = 0;
					}

					currEditText = alarmOffsetMinsLayout.findViewById(R.id.alarmOffsetMinsInput);
					try {
						mins = format.parse(currEditText.getText().toString()).intValue();
					} catch (ParseException e) {
						Log.e(TAG, "Couldn't parse the minutes to offset.");
						mins = 0;
					}

					// starts with curr time/date
					GregorianCalendar newCalendar = new GregorianCalendar();
					if (((Alarm) workingListable).getRepeatType() == Alarm.REPEAT_ONCE_REL) {
						newCalendar.add(Calendar.DAY_OF_MONTH, days);
						newCalendar.add(Calendar.HOUR_OF_DAY, hours);
						newCalendar.add(Calendar.MINUTE, mins);
					}

					hours = hours + mins/60;
					mins = mins % 60;
					days = days + hours/24;
					hours = hours % 24;

					((Alarm) workingListable).setOffsetDays(days);
					((Alarm) workingListable).setOffsetHours(hours);
					((Alarm) workingListable).setOffsetMins(mins);

					((Alarm) workingListable).setAlarmTimeMillis(newCalendar.getTimeInMillis());
					break;
				case Alarm.REPEAT_DATE_MONTHLY:
					if (alarmDateOfMonthLayout == null) {
						Log.wtf(TAG, "The alarm date of month is null.");
						exitActivity();
						return;
					}

					NumberPicker picker = alarmDateOfMonthLayout.findViewById(R.id.alarmDateOfMonthInput);
					alarmCalendar.set(Calendar.DAY_OF_MONTH, picker.getValue());
					// continues onto the next case
				case Alarm.REPEAT_DAY_WEEKLY:
				case Alarm.REPEAT_DAY_MONTHLY:
					if (alarmTimePicker == null) {
						Log.wtf(TAG, "The alarm time picker is null.");
						exitActivity();
					}

					alarmCalendar.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
					alarmCalendar.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());
					break;
				default:
					Log.e(TAG, "The alarm has an invalid repeat type.");
					exitActivity();
			}

			((Alarm) workingListable).updateRingTime();
		}

		// encoding into an intent
		String editString = workingListable.toEditString();
		Intent result_intent = new Intent();

		result_intent.putExtra(EXTRA_LISTABLE, editString);
		if (isEditing) {
			result_intent.putExtra(EXTRA_LISTABLE_INDEX, listableIndex);
		}

		// exit with result
		setResult(RESULT_OK, result_intent);
		finish();
	}

	/**
	 * An onclick callback if a TextView representing a day of the week is clicked.
	 * @param view the day of week TextView that was clicked
	 */
	public void dayOfWeekClicked(@NotNull View view) {
		int index = Integer.parseInt(view.getTag().toString());
		boolean newState = !((Alarm) workingListable).getRepeatDays(index);

		((Alarm) workingListable).setRepeatDays(index, newState);
		changeColors((TextView) view, newState);
	}

	/**
	 * An onclick callback if a TextView representing a month of the year is clicked.
	 * @param view the month TextView that was clicked
	 */
	public void monthClicked(@NotNull View view) {
		int index = Integer.parseInt(view.getTag().toString());
		boolean newState = !((Alarm) workingListable).getRepeatMonths(index);

		((Alarm) workingListable).setRepeatMonths(index, newState);
		changeColors((TextView) view, newState);
	}

	/**
	 * Callback for the set ringtone button. Opens another app to choose the audio file.
	 * TODO: Perhaps make my own sound picker...?
	 * @param view the pick ringtone button (view that triggered the callback)
	 */
	public void chooseSound(@NotNull View view) {
		Intent getSound = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		getSound.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
				((Alarm) workingListable).getRingtoneUri());
		getSound.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
		startActivityForResult(getSound, REQ_GET_RINGTONE);
	}

	/**
	 * Callback called after a ringtone is chosen.
	 * @param requestCode the code we sent when creating the activity
	 * @param resultCode the code the activity sent back after finishing
	 * @param data the data sent back by the activity
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == REQ_GET_RINGTONE && resultCode == RESULT_OK) {
			Uri uri = null;
			if (data != null)
				uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

			((Alarm) workingListable).setRingtoneUri(uri);

			TextView t = findViewById(R.id.soundText);
			t.setText(((Alarm) workingListable).getRingtoneName());
		}
	}

	/* *************************  Callbacks from OnItemSelectedListener  ************************ */

	/**
	 * Based on which repeat type was selected (using the position), will show/hide certain UI
	 * elements and change the repeatType of workingAlarm.
	 *
	 * @param parent Spinner (AdapterView) that just closed
	 * @param view view within the AdapterView that was clicked
	 * @param pos position of the view within the Adapter
	 * @param id row ID of the item selected
	 */
	@Override
	public void onItemSelected(@NotNull AdapterView<?> parent, View view, int pos, long id) {
		switch (parent.getId()) {
			case R.id.alarmRepeatTypeInput:
				changeRepeatType(pos, false);
				break;
			case R.id.alarmWeekOfMonthInput:
				((Alarm) workingListable).setRepeatWeek(pos);
				break;
			case R.id.alarmDayOfWeekInput:
				((Alarm) workingListable).getAlarmTimeCalendar().set(Calendar.DAY_OF_WEEK, pos + 1);
				break;
			default:
				Log.e(TAG, "Unknown AdapterView selected an item.");
				exitActivity();
		}
	}

	/**
	 * Doesn't do anything when nothing has been selected from the repeat type dropdown.
	 * @param parent Spinner (AdapterView) that just closed
	 */
	@Override
	public void onNothingSelected(@NotNull AdapterView<?> parent) {}

	/* ***************************************  UI Setup  ************************************** */

	/**
	 * Sets up the UI for editing (or creating) an alarm.
	 */
	private void alarmUISetup() {
		setContentView(R.layout.editor_alarm);
		changeRepeatType(((Alarm) workingListable).getRepeatType(), true);

		Spinner spinner;
		ArrayAdapter<CharSequence> adapter;

		// repeatType spinner
		spinner = findViewById(R.id.alarmRepeatTypeInput);
		adapter = ArrayAdapter.createFromResource(this,
				R.array.alarm_editor_repeat_strings, android.R.layout.simple_spinner_dropdown_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(this);
		spinner.setSelection(((Alarm) workingListable).getRepeatType());

		// set name of the current ringtone
		TextView alarmSoundLabel = findViewById(R.id.soundText);
		alarmSoundLabel.setText(((Alarm) workingListable).getRingtoneName());
	}

	/**
	 * Sets up the UI for editing (or creating) a folder.
	 */
	private void folderUISetup() { setContentView(R.layout.editor_folder); }

	/**
	 * Sets up the days of the week field (alarmDaysLayout) and corresponding UI (text/colors). Does
	 * not check whether the field is null or not. For alarms only.
	 */
	private void setupWeekDays() {
		alarmDaysLayout = findViewById(R.id.alarmDaysInput);

		Alarm alarm = (Alarm) workingListable;
		ArrayList<View> children = getAllChildren(alarmDaysLayout);
		String[] dayStrings = (new DateFormatSymbols()).getShortWeekdays();

		changeColors(children, alarm.getRepeatDays());

		// loops through Calendar constants for days, starts at 1 and ends at 7
		for (int i = 1; i < dayStrings.length; i++) {
			((TextView) children.get(i - 1)).setText(dayStrings[i]);
		}
	}

	/**
	 * Sets up the months field (alarmMonthsLayout) and corresponding UI (text/colors). Does not
	 * check whether the field is null or not. For alarms only.
	 */
	private void setupMonths() {
		alarmMonthsLayout = findViewById(R.id.alarmMonthsInput);

		Alarm alarm = (Alarm) workingListable;
		ArrayList<View> children = getAllChildren(alarmMonthsLayout);
		String[] monthStrings = (new DateFormatSymbols()).getShortMonths();

		changeColors(children, alarm.getRepeatMonths());

		// loops through Calendar constants for months, starts at 0 and ends at 11
		for (int i = 0; i < monthStrings.length; i++) {
			((TextView) children.get(i)).setText(monthStrings[i]);
		}
	}

	/**
	 * Sets up the time picker for the first time. Sets the field and UI (hours/minutes on picker).
	 * Does not check whether the field is null or not. For alarms only.
	 */
	private void setupTimePicker() {
		int timePickerHour, timePickerMin;
		GregorianCalendar sysClock = new GregorianCalendar();

		if (isEditing) {
			// sets it to the current alarm time first
			timePickerHour = ((Alarm) workingListable).getAlarmTimeCalendar().get(Calendar.HOUR_OF_DAY);
			timePickerMin = ((Alarm) workingListable).getAlarmTimeCalendar().get(Calendar.MINUTE);
		}
		else {
			// matches the TimePicker time to current system time, should only be once
			timePickerHour = sysClock.get(Calendar.HOUR_OF_DAY);
			timePickerMin = sysClock.get(Calendar.MINUTE);
		}

		alarmTimePicker = findViewById(R.id.alarmTimeInput);

		// editing alarmTimePicker to match hour/min
		alarmTimePicker.setCurrentHour(timePickerHour);
		alarmTimePicker.setCurrentMinute(timePickerMin);
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
		spinner.setSelection(((Alarm) workingListable).getRepeatWeek());
		spinner.setOnItemSelectedListener(this);

		// day of week spinner
		spinner = alarmDayMonthlyLayout.findViewById(R.id.alarmDayOfWeekInput);
		adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
		adapter.addAll((new DateFormatSymbols()).getWeekdays());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(((Alarm) workingListable).getAlarmTimeCalendar().get(Calendar.DAY_OF_WEEK) - 1);
		spinner.setOnItemSelectedListener(this);
	}

	/**
	 * Handles setup of folder spinner. Sets it up after the data service returns with the actual
	 * data structure.
	 * @param msg the inbound MSG_GET_FOLDERS message
	 */
	private void setupFolderStructure(@NotNull Message msg) {
		Bundle data = msg.getData();
		if (data == null) {
			Log.e(TAG, "Message from alarm service had a null bundle.");
			return;
		}

		ArrayList<String> array = data.getStringArrayList(AlarmDataService.BUNDLE_LIST_KEY);
		if (array == null) {
			Log.e(TAG, "Message from alarm service had a null folder structure.");
			return;
		}

		Spinner spinner = findViewById(R.id.parentFolderInput);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
		adapter.addAll(array);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		if (isEditing) {
			int index = array.indexOf(listablePath);
			if (index == -1) index = 0;

			spinner.setSelection(index);
		}
		else {
			spinner.setSelection(0);
		}

		unbindService(dataConn);
		boundToDataService = false;
	}

	/* ************************************  Other Methods  ********************************* */

	/**
	 * Exits the current activity gracefully. Sets the result as RESULT_CANCELLED to ensure that
	 * nothing is done to any data as a result of this exit.
	 */
	private void exitActivity() {
		setResult(RESULT_CANCELED);
		finish();
	}

	/**
	 * Changes repeat type to the new specified type. Changes both the working alarm type and the UI
	 * layouts. For alarms only.
	 * @param type new type to set it to, should be one of the Alarm constants
	 * @param first whether the repeat type is the first repeat type to be initialized or not
	 */
	@SuppressLint("DefaultLocale")
	private void changeRepeatType(int type, boolean first) {
		if (!first) {
			switch (((Alarm) workingListable).getRepeatType()) {
				case Alarm.REPEAT_DATE_YEARLY:
				case Alarm.REPEAT_ONCE_ABS:
					// requires: time picker, date picker
					if (alarmTimePicker == null || alarmDatePicker == null) {
						Log.wtf(TAG, "The alarm date/time pickers are null.");
						exitActivity();
						return;
					}
					alarmTimePicker.setVisibility(View.GONE);
					alarmDatePicker.setVisibility(View.GONE);
					break;
				case Alarm.REPEAT_OFFSET:
				case Alarm.REPEAT_ONCE_REL:
					// requires: offset fields (days, hours, minutes)
					if (alarmOffsetDaysLayout == null || alarmOffsetHoursLayout == null ||
							alarmOffsetMinsLayout == null) {
						Log.wtf(TAG, "The alarm offsets layouts are null.");
						exitActivity();
						return;
					}
					alarmOffsetDaysLayout.setVisibility(View.GONE);
					alarmOffsetHoursLayout.setVisibility(View.GONE);
					alarmOffsetMinsLayout.setVisibility(View.GONE);
					break;
				case Alarm.REPEAT_DAY_WEEKLY:
					// requires: time picker, list of days (clickable text)
					if (alarmTimePicker == null || alarmDaysLayout == null) {
						Log.wtf(TAG, "The alarm time picker or days of week layout is null.");
						exitActivity();
						return;
					}
					alarmTimePicker.setVisibility(View.GONE);
					alarmDaysLayout.setVisibility(View.GONE);
					break;
				case Alarm.REPEAT_DATE_MONTHLY:
					// requires: time picker, date of month (number picker), list of months (clickable text)
					if (alarmTimePicker == null || alarmMonthsLayout == null ||
							alarmDateOfMonthLayout == null) {
						Log.wtf(TAG, "The alarm time picker, months layout, or date of month layout is null.");
						exitActivity();
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
						Log.wtf(TAG, "The alarm time picker, day of the month, or months layout is null.");
						exitActivity();
						return;
					}
					alarmTimePicker.setVisibility(View.GONE);
					alarmDayMonthlyLayout.setVisibility(View.GONE);
					alarmMonthsLayout.setVisibility(View.GONE);
					break;
				default:
					Log.e(TAG, "Invalid previous alarm type selected.");
					exitActivity();
			}
		}

		switch (type) {
			case Alarm.REPEAT_DATE_YEARLY:
			case Alarm.REPEAT_ONCE_ABS:
				// requires: time picker, date picker
				if (alarmTimePicker == null) setupTimePicker();
				alarmTimePicker.setVisibility(View.VISIBLE);

				if (alarmDatePicker == null) {
					alarmDatePicker = findViewById(R.id.alarmDateInput);
					alarmDatePicker.setMinDate(new GregorianCalendar().getTimeInMillis());
				}
				alarmDatePicker.setVisibility(View.VISIBLE);
				break;
			case Alarm.REPEAT_OFFSET:
			case Alarm.REPEAT_ONCE_REL:
				// requires: offset fields (days, hours, minutes)
				// only really need to check one offset field, since they essentially come as a set
				if (alarmOffsetDaysLayout == null) {
					EditText curr;

					alarmOffsetDaysLayout = findViewById(R.id.alarmOffsetDays);
					curr = alarmOffsetDaysLayout.findViewById(R.id.alarmOffsetDaysInput);
					curr.setText(String.format("%d", ((Alarm) workingListable).getOffsetDays()));

					alarmOffsetHoursLayout = findViewById(R.id.alarmOffsetHours);
					curr = alarmOffsetHoursLayout.findViewById(R.id.alarmOffsetHoursInput);
					curr.setText(String.format("%d", ((Alarm) workingListable).getOffsetHours()));

					alarmOffsetMinsLayout = findViewById(R.id.alarmOffsetMins);
					curr = alarmOffsetMinsLayout.findViewById(R.id.alarmOffsetMinsInput);
					curr.setText(String.format("%d", ((Alarm) workingListable).getOffsetMins()));
				}

				alarmOffsetDaysLayout.setVisibility(View.VISIBLE);
				alarmOffsetHoursLayout.setVisibility(View.VISIBLE);
				alarmOffsetMinsLayout.setVisibility(View.VISIBLE);
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
					dateOfMonth.setValue(((Alarm) workingListable).getAlarmTimeCalendar().get(Calendar.DAY_OF_MONTH));
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
				Log.e(TAG, "Invalid alarm type selected.");
				exitActivity();
		}
		((Alarm) workingListable).setRepeatType(type);
	}

	/**
	 * Gets all children in the selected view hierarchy, except for ViewGroups.
	 * @param view the View or ViewGroup to generate a list for, can be null
	 * @return the list of children, in a 1D ArrayList and not including view groups, cannot be null
	 */
	@NotNull
	private static ArrayList<View> getAllChildren(@Nullable View view) {
		ArrayList<View> children = new ArrayList<>();

		if (view == null) {
			return children;
		}
		else if (!(view instanceof ViewGroup)) {
			children.add(view);
			return children;
		}

		ViewGroup group = (ViewGroup) view;
		for (int i = 0, n = group.getChildCount(); i < n; i++) {
			children.addAll(getAllChildren(group.getChildAt(i)));
		}
		return children;
	}

	/**
	 * Changes the colors of the children specified according to the mask. Changes up to the length
	 * of the mask or all of the views in group, whichever comes first.
	 * @param children the children to change the colors of views for (TextViews), shouldn't be null
	 * @param mask the boolean mask to use, where true is highlighted, false is not
	 */
	private void changeColors(@NotNull ArrayList<View> children, boolean[] mask) {
		int max = Math.min(children.size(), mask.length);
		TextView t;

		TypedArray textColors = getTheme().obtainStyledAttributes(
				new int[] {R.attr.activeTextColor, R.attr.inactiveTextColor});

		for (int i = 0; i < max; i++) {
			// change colors of each child
			t = (TextView) children.get(i);
			if (t == null) continue;

			if (mask[i]) { t.setTextColor(textColors.getColor(0, 0)); }
			else { t.setTextColor(textColors.getColor(1, 0)); }
		}

		textColors.recycle();
	}

	/**
	 * Changes the colors of a single text view.
	 * TODO: could possibly use a color selector for these views instead
	 * @param view the TextView to change the color of view for, shouldn't be null
	 * @param mask the boolean mask to use, where true is highlighted, false is not
	 */
	private void changeColors(@NotNull TextView view, boolean mask) {
		TypedArray textColors = getTheme().obtainStyledAttributes(
				new int[] {R.attr.activeTextColor, R.attr.inactiveTextColor});

		if (mask) { view.setTextColor(textColors.getColor(0, 0)); }
		else { view.setTextColor(textColors.getColor(1, 0)); }

		textColors.recycle();
	}

	/* ***********************************  Inner Classes  ************************************* */

	/**
	 * The service connection used for connecting to the data service. Used for passing the messenger
	 * on to the recycler adapter.
	 */
	private class DataServiceConnection implements ServiceConnection {
		/**
		 * Called when the service is connected. Sends the messenger to the adapter.
		 * @param className the name of the class that was bound to (unused)
		 * @param service the binder that the service returned
		 */
		@Override
		public void onServiceConnected(@NotNull ComponentName className, @NotNull IBinder service) {
			boundToDataService = true;

			Messenger messenger = new Messenger(service);
			Message msg = Message.obtain(null, AlarmDataService.MSG_GET_FOLDERS);
			msg.replyTo = new Messenger(new MsgHandler(ListableEditorActivity.this));
			try {
				messenger.send(msg);
			}
			catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Called when the data service crashes.
		 * @param className the name of the class that was bound to (unused)
		 */
		@Override
		public void onServiceDisconnected(@NotNull ComponentName className) {
			Log.e(TAG, "The data service crashed.");
			boundToDataService = false;
		}
	}

	/**
	 * Inner Handler class for handling messages from the data service. Only really handles
	 * MSG_GET_FOLDERS messages.
	 */
	private static class MsgHandler extends Handler {
		/**
		 * The activity that owns this handler.
		 */
		@NotNull
		private ListableEditorActivity activity;

		/**
		 * Creates a new handler on the main thread.
		 */
		private MsgHandler(@NotNull ListableEditorActivity currActivity) {
			super(Looper.getMainLooper());

			activity = currActivity;
		}

		/**
		 * Handles messages from the data service. If it's not a MSG_GET_FOLDERS message, doesn't
		 * do anything. Otherwise, sends it to the activity.
		 * @param msg the incoming Message to handle
		 */
		@Override
		public void handleMessage(@Nullable Message msg) {
			if (msg == null || msg.what != AlarmDataService.MSG_GET_FOLDERS) {
				Log.e(TAG, "Message to handle is invalid. Ignoring...");
				return;
			}

			activity.setupFolderStructure(msg);
		}
	}
}
