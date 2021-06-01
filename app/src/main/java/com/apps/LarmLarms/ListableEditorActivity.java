package com.apps.LarmLarms;

import android.content.Intent;
import android.content.res.TypedArray;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity used for creating new alarms or editing existing ones.
 */

public class ListableEditorActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
	private final static String TAG = "ListableEditor";

	private final static int REQ_GET_RINGTONE = 0;

	private Listable workingListable;

	// information about Listable being edited
	private int listableIndex;
	/**
	 * Shows whether the current activity is editing an Alarm (true) or AlarmGroup (false).
	 */
	private boolean isEditingAlarm;
	/**
	 * Shows whether the current activity is editing a Listable (true) or creating a new one (false).
	 */
	private boolean isEditing;

	// handles to views
	private TimePicker alarmTimePicker;
	private DatePicker alarmDatePicker;
	private ViewGroup alarmDaysLayout, alarmOffsetDaysLayout, alarmOffsetHoursLayout,
			alarmOffsetMinsLayout, alarmDayMonthlyLayout, alarmMonthsLayout, alarmDateOfMonthLayout;

	/* *********************************  Lifecycle Methods  ******************************* */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int startedState = getIntent().getIntExtra(MainActivity.EXTRA_REQ_ID, -1);

		// NOTE: this switch statement is only for setting up activity variables, NOT queuing any UI
		// changes, since the content view hasn't been set up yet.
		switch(startedState) {
			case MainActivity.REQ_NEW_ALARM:
				newAlarmFieldSetup();
				Log.i(TAG, "Creating a new alarm.");
				break;
				
			case MainActivity.REQ_EDIT_ALARM:
				editAlarmFieldSetup();
				Log.i(TAG, "Editing an existing alarm.");
				break;
				
			case MainActivity.REQ_NEW_FOLDER:
				newFolderFieldSetup();
				Log.i(TAG, "Creating a new folder.");
				break;
			case MainActivity.REQ_EDIT_FOLDER:
				editFolderFieldSetup();
				Log.i(TAG, "Editing an existing folder.");
				break;
		}

		if (workingListable == null) {
			Log.e(TAG, "Couldn't open the editor.");
			exitActivity();
		}

		if (isEditingAlarm) { alarmUISetup(); }
		else { folderUISetup(); }

		// this switch statement can be used for any REQ-specific UI changes
		switch(startedState) {
			case MainActivity.REQ_NEW_ALARM:
			case MainActivity.REQ_NEW_FOLDER:
				break;
			case MainActivity.REQ_EDIT_ALARM:
			case MainActivity.REQ_EDIT_FOLDER:
				((EditText) findViewById(R.id.nameInput)).setText(workingListable.getListableName());
				break;
			default:
				Log.e(TAG, "Unknown request code!");
				exitActivity();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (isEditingAlarm) {
			// matches the TimePicker's display mode to current system config (12 hr or 24 hr)
			alarmTimePicker.setIs24HourView(DateFormat.is24HourFormat(this));
		}
	}

	/* ************************************  Callbacks  ************************************* */

	/**
	 * An onclick callback for the back button. Closes the AlarmCreator and return
	 * RESULT_CANCELLED and no intent.
	*/
	public void backButtonClicked(View view) {
		Log.i(TAG, "Back button pressed. No changes made to any alarms. Exiting.");

		// TODO: ask user if they REALLY want to exit?
		exitActivity();
	}

	/**
	 * An onclick callback for the save button. Closes the AlarmCreator, returns RESULT_OK and
	 * the Listable (in string form) in the intent.
	*/
	public void saveAlarm(View view) {
		// TODO: validate any field inputs?
		Log.i(TAG, "Sending new alarm back to the caller.");

		// saving data
		if (isEditingAlarm) {
			Calendar alarmCalendar = ((Alarm) workingListable).getAlarmTimeCalendar();

			switch(((Alarm) workingListable).getRepeatType()) {
				case Alarm.REPEAT_ONCE_ABS:
				case Alarm.REPEAT_DATE_YEARLY:
					alarmCalendar.set(Calendar.YEAR, alarmDatePicker.getYear());
					alarmCalendar.set(Calendar.MONTH, alarmDatePicker.getMonth());
					alarmCalendar.set(Calendar.DAY_OF_MONTH, alarmDatePicker.getDayOfMonth());

					alarmCalendar.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
					alarmCalendar.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());
					break;
				case Alarm.REPEAT_ONCE_REL:
				case Alarm.REPEAT_OFFSET:
					EditText currEditText;
					int days, hours, mins;

					currEditText = alarmOffsetDaysLayout.findViewById(R.id.alarmOffsetDaysInput);
					try {
						days = Integer.parseInt(currEditText.getText().toString());
					} catch (NumberFormatException e) {
						days = 0;
					}

					currEditText = alarmOffsetHoursLayout.findViewById(R.id.alarmOffsetHoursInput);
					try {
						hours = Integer.parseInt(currEditText.getText().toString());
					} catch (NumberFormatException e) {
						hours = 0;
					}

					currEditText = alarmOffsetMinsLayout.findViewById(R.id.alarmOffsetMinsInput);
					try {
						mins = Integer.parseInt(currEditText.getText().toString());
					} catch (NumberFormatException e) {
						mins = 0;
					}

					if (((Alarm) workingListable).getRepeatType() == Alarm.REPEAT_ONCE_REL) {
						// starts with curr time/date
						GregorianCalendar newCalendar = new GregorianCalendar();
						newCalendar.add(Calendar.DAY_OF_MONTH, days);
						newCalendar.add(Calendar.HOUR_OF_DAY, hours);
						newCalendar.add(Calendar.MINUTE, mins);
						((Alarm) workingListable).setAlarmTimeMillis(newCalendar.getTimeInMillis());
					}
					else {
						hours = hours + mins/60;
						mins = mins % 60;
						days = days + hours/24;
						hours = hours % 24;

						((Alarm) workingListable).setOffsetDays(days);
						((Alarm) workingListable).setOffsetHours(hours);
						((Alarm) workingListable).setOffsetMins(mins);
					}
					break;
				case Alarm.REPEAT_DATE_MONTHLY:
					NumberPicker picker = alarmDateOfMonthLayout.findViewById(R.id.alarmDateOfMonthInput);
					alarmCalendar.set(Calendar.DAY_OF_MONTH, picker.getValue());
				case Alarm.REPEAT_DAY_WEEKLY:
				case Alarm.REPEAT_DAY_MONTHLY:
					alarmCalendar.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
					alarmCalendar.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());
					break;
				default:
					Log.e(TAG, "The alarm has an invalid repeat type.");
					break;
			}

			((Alarm) workingListable).updateRingTime();
		}

		// common fields
		EditText nameInput = findViewById(R.id.nameInput);
		workingListable.setListableName(nameInput.getText().toString());
		workingListable.turnOn();

		// encoding into an intent
		String editString = workingListable.toEditString();
		Intent result_intent = new Intent();

		result_intent.putExtra(MainActivity.EXTRA_LISTABLE, editString);
		if (isEditing) {
			result_intent.putExtra(MainActivity.EXTRA_LISTABLE_INDEX, listableIndex);
		}

		// exit with result
		setResult(RESULT_OK, result_intent);
		finish();
	}

	/**
	 * An onclick callback if a TextView representing a day of the week is clicked.
	 * @param view the TextView that was clicked
	 */
	public void dayOfWeekClicked(View view) {
		int index = Integer.parseInt(view.getTag().toString());
		boolean newState = !((Alarm) workingListable).getRepeatDays(index);

		((Alarm) workingListable).setRepeatDays(index, newState);
		changeColors((TextView) view, newState);
	}

	/**
	 * An onclick callback if a TextView representing a month of the year is clicked.
	 * @param view the TextView that was clicked
	 */
	public void monthClicked(View view) {
		int index = Integer.parseInt(view.getTag().toString());
		boolean newState = !((Alarm) workingListable).getRepeatMonths(index);

		((Alarm) workingListable).setRepeatMonths(index, newState);
		changeColors((TextView) view, newState);
	}

	/**
	 * Callback for the set ringtone button. Opens another app to choose the audio file.
	 * TODO: Perhaps make my own sound picker...?
	 */
	public void chooseSound(View view) {
		Intent getSound = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		startActivityForResult(getSound, REQ_GET_RINGTONE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_GET_RINGTONE && resultCode == RESULT_OK) {
			Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			((Alarm) workingListable).setRingtone(uri);

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
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		switch (parent.getId()) {
			case R.id.alarmRepeatTypeInput:
				((Alarm) workingListable).setRepeatType(pos);
				changeRepeatType(pos);
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

	@Override
	public void onNothingSelected(AdapterView<?> parent) {}

	/* ************************************  Other Methods  ********************************* */

	/**
	 * Exits the current activity gracefully. Sets the result as RESULT_CANCELLED to ensure that
	 * nothing is done to any data as a result of this exit.
	 */
	private void exitActivity() {
		setResult(RESULT_CANCELED);
		finish();
	}

	private void newAlarmFieldSetup() {
		isEditingAlarm = true;
		isEditing = false;
		workingListable = new Alarm(this);
	}
	private void editAlarmFieldSetup() {
		Intent callingIntent = getIntent();

		isEditingAlarm = true;
		isEditing = true;
		listableIndex = callingIntent.getIntExtra(MainActivity.EXTRA_LISTABLE_INDEX, -1);

		workingListable = Alarm.fromEditString(this, callingIntent.getStringExtra(MainActivity.EXTRA_LISTABLE));
		if (workingListable == null) {
			Log.e(TAG, "fromEditString returned null.");
			exitActivity();
		}
	}
	private void newFolderFieldSetup() {
		isEditingAlarm = false;
		isEditing = false;
		workingListable = new AlarmGroup();
	}
	private void editFolderFieldSetup() {
		Intent callingIntent = getIntent();

		isEditingAlarm = false;
		isEditing = true;
		listableIndex = callingIntent.getIntExtra(MainActivity.EXTRA_LISTABLE_INDEX, -1);

		workingListable = AlarmGroup.fromEditString(callingIntent.getStringExtra(MainActivity.EXTRA_LISTABLE));
		if (workingListable == null) {
			Log.e(TAG, "fromEditString returned null.");
			exitActivity();
		}
	}

	private void alarmUISetup() {
		setContentView(R.layout.activity_alarm_editor);

		// getting handles to views
		alarmTimePicker = findViewById(R.id.alarmTimeInput);
		alarmDatePicker = findViewById(R.id.alarmDateInput);
		alarmDaysLayout = findViewById(R.id.alarmDaysInput);
		alarmOffsetDaysLayout = findViewById(R.id.alarmOffsetDays);
		alarmOffsetHoursLayout = findViewById(R.id.alarmOffsetHours);
		alarmOffsetMinsLayout = findViewById(R.id.alarmOffsetMins);
		alarmDayMonthlyLayout = findViewById(R.id.alarmDayMonthly);
		alarmDateOfMonthLayout = findViewById(R.id.alarmDateOfMonth);
		alarmMonthsLayout = findViewById(R.id.alarmMonthsInput);

		int currRepeatType = ((Alarm) workingListable).getRepeatType();
		GregorianCalendar sysClock = new GregorianCalendar();
		int timePickerHour, timePickerMin;
		Alarm alarm = (Alarm) workingListable;

		if (isEditing) {
			// REQ_EDIT_ALARM
			timePickerHour = alarm.getAlarmTimeCalendar().get(Calendar.HOUR_OF_DAY);
			timePickerMin = alarm.getAlarmTimeCalendar().get(Calendar.MINUTE);

			if (currRepeatType == Alarm.REPEAT_ONCE_REL || currRepeatType == Alarm.REPEAT_OFFSET) {
				// TODO: use String.format() to format these numbers?
				EditText curr;

				curr = findViewById(R.id.alarmOffsetDaysInput);
				curr.setText(Integer.toString(alarm.getOffsetDays()));

				curr = findViewById(R.id.alarmOffsetHoursInput);
				curr.setText(Integer.toString(alarm.getOffsetHours()));

				curr = findViewById(R.id.alarmOffsetMinsInput);
				curr.setText(Integer.toString(alarm.getOffsetMins()));
			}

			TextView alarmSoundLabel = findViewById(R.id.soundText);
			alarmSoundLabel.setText(alarm.getRingtoneName());
		}
		else {
			// REQ_NEW_ALARM
			// matches the TimePicker time to current system time format, since we don't want to
			// change this onResume because the user might have a custom time already set
			timePickerHour = sysClock.get(Calendar.HOUR_OF_DAY);
			timePickerMin = sysClock.get(Calendar.MINUTE);
		}

		// editing alarmTimePicker to match hour/min
		alarmTimePicker.setCurrentHour(timePickerHour);
		alarmTimePicker.setCurrentMinute(timePickerMin);

		alarmDatePicker.setMinDate(sysClock.getTimeInMillis());

		Spinner spinner;
		ArrayAdapter<CharSequence> adapter;

		// repeatType spinner
		spinner = findViewById(R.id.alarmRepeatTypeInput);
		adapter = ArrayAdapter.createFromResource(this,
				R.array.alarm_repeat_strings, android.R.layout.simple_spinner_dropdown_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(this);
		spinner.setSelection(((Alarm) workingListable).getRepeatType());
		// changeRepeatType(currRepeatType);

		// DAY_MONTHLY week of month spinner
		spinner = alarmDayMonthlyLayout.findViewById(R.id.alarmWeekOfMonthInput);
		adapter = ArrayAdapter.createFromResource(this,
				R.array.alarm_week_strings, android.R.layout.simple_spinner_dropdown_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(alarm.getRepeatWeek());
		spinner.setOnItemSelectedListener(this);

		// DAY_MONTHLY day of week spinner
		spinner = alarmDayMonthlyLayout.findViewById(R.id.alarmDayOfWeekInput);
		adapter = ArrayAdapter.createFromResource(this,
				R.array.alarm_day_strings_long, android.R.layout.simple_spinner_dropdown_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(alarm.getAlarmTimeCalendar().get(Calendar.DAY_OF_WEEK) - 1);
		spinner.setOnItemSelectedListener(this);

		// DATE_MONTHLY day of month number picker
		NumberPicker dateOfMonth = alarmDateOfMonthLayout.findViewById(R.id.alarmDateOfMonthInput);
		dateOfMonth.setMinValue(1);
		dateOfMonth.setMaxValue(31);
		dateOfMonth.setValue(alarm.getAlarmTimeCalendar().get(Calendar.DAY_OF_MONTH));

		// change colors of days and months layouts
		changeColors(alarmDaysLayout, alarm.getRepeatDays());
		changeColors(alarmMonthsLayout, alarm.getRepeatMonths());
	}
	private void folderUISetup() { setContentView(R.layout.activity_folder_editor); }

	private void changeRepeatType(int type) {
		switch (type) {
			case Alarm.REPEAT_DATE_YEARLY:
			case Alarm.REPEAT_ONCE_ABS:
				// require: time picker, date picker
				alarmTimePicker.setVisibility(View.VISIBLE);
				alarmDatePicker.setVisibility(View.VISIBLE);
				alarmDaysLayout.setVisibility(View.GONE);
				alarmOffsetDaysLayout.setVisibility(View.GONE);
				alarmOffsetHoursLayout.setVisibility(View.GONE);
				alarmOffsetMinsLayout.setVisibility(View.GONE);
				alarmDayMonthlyLayout.setVisibility(View.GONE);
				alarmMonthsLayout.setVisibility(View.GONE);
				alarmDateOfMonthLayout.setVisibility(View.GONE);
				break;
			case Alarm.REPEAT_OFFSET:
			case Alarm.REPEAT_ONCE_REL:
				// requires: time fields or number pickers (days, hours, minutes)
				alarmTimePicker.setVisibility(View.GONE);
				alarmDatePicker.setVisibility(View.GONE);
				alarmDaysLayout.setVisibility(View.GONE);
				alarmOffsetDaysLayout.setVisibility(View.VISIBLE);
				alarmOffsetHoursLayout.setVisibility(View.VISIBLE);
				alarmOffsetMinsLayout.setVisibility(View.VISIBLE);
				alarmDayMonthlyLayout.setVisibility(View.GONE);
				alarmMonthsLayout.setVisibility(View.GONE);
				alarmDateOfMonthLayout.setVisibility(View.GONE);
				break;
			case Alarm.REPEAT_DAY_WEEKLY:
				// requires: time picker, list of days (clickable text)
				alarmTimePicker.setVisibility(View.VISIBLE);
				alarmDatePicker.setVisibility(View.GONE);
				alarmDaysLayout.setVisibility(View.VISIBLE);
				alarmOffsetDaysLayout.setVisibility(View.GONE);
				alarmOffsetHoursLayout.setVisibility(View.GONE);
				alarmOffsetMinsLayout.setVisibility(View.GONE);
				alarmDayMonthlyLayout.setVisibility(View.GONE);
				alarmMonthsLayout.setVisibility(View.GONE);
				alarmDateOfMonthLayout.setVisibility(View.GONE);
				break;
			case Alarm.REPEAT_DATE_MONTHLY:
				// requires: time picker, date of month (number picker), list of months (clickable text)
				alarmTimePicker.setVisibility(View.VISIBLE);
				alarmDatePicker.setVisibility(View.GONE);
				alarmDaysLayout.setVisibility(View.GONE);
				alarmOffsetDaysLayout.setVisibility(View.GONE);
				alarmOffsetHoursLayout.setVisibility(View.GONE);
				alarmOffsetMinsLayout.setVisibility(View.GONE);
				alarmDayMonthlyLayout.setVisibility(View.GONE);
				alarmMonthsLayout.setVisibility(View.VISIBLE);
				alarmDateOfMonthLayout.setVisibility(View.VISIBLE);
				break;
			case Alarm.REPEAT_DAY_MONTHLY:
				// requires: time picker, list of months, date of month (number picker), spinner for the week #
				alarmTimePicker.setVisibility(View.VISIBLE);
				alarmDatePicker.setVisibility(View.GONE);
				alarmDaysLayout.setVisibility(View.GONE);
				alarmOffsetDaysLayout.setVisibility(View.GONE);
				alarmOffsetHoursLayout.setVisibility(View.GONE);
				alarmOffsetMinsLayout.setVisibility(View.GONE);
				alarmDayMonthlyLayout.setVisibility(View.VISIBLE);
				alarmMonthsLayout.setVisibility(View.VISIBLE);
				alarmDateOfMonthLayout.setVisibility(View.GONE);
				break;
			default:
				Log.e(TAG, "Invalid alarm type selected.");
				exitActivity();
		}
	}

	/**
	 * Gets all children in the selected view hierarchy, except for ViewGroups.
	 * @param view the View or ViewGroup to generate a list for
	 * @return the list of children, in a 1D ArrayList and not including view groups
	 */
	private static ArrayList<View> getAllChildren(View view) {
		ArrayList<View> children = new ArrayList<>();

		if (!(view instanceof ViewGroup)) {
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
	 * Changes the colors of the views in group (not just direct children) according to the mask.
	 * Changes up to the length of the mask or all of the views in group, whichever comes first.
	 * @param group the ViewGroup to change the colors of views in
	 * @param mask the boolean mask to use, where true is highlighted, false is not
	 */
	private void changeColors(ViewGroup group, boolean[] mask) {
		ArrayList<View> children = getAllChildren(group);
		int max = Math.min(children.size(), mask.length);
		TextView t;

		TypedArray textColors = getTheme().obtainStyledAttributes(
				new int[] {R.attr.activeTextColor, R.attr.inactiveTextColor});

		for (int i = 0; i < max; i++) {
			// change colors of each child
			t = (TextView) children.get(i);
			if (mask[i]) { t.setTextColor(textColors.getColor(0, 0)); }
			else { t.setTextColor(textColors.getColor(1, 0)); }
		}

		textColors.recycle();
	}

	private void changeColors(TextView view, boolean mask) {
		TypedArray textColors = getTheme().obtainStyledAttributes(
				new int[] {R.attr.activeTextColor, R.attr.inactiveTextColor});

		if (mask) { view.setTextColor(textColors.getColor(0, 0)); }
		else { view.setTextColor(textColors.getColor(1, 0)); }

		textColors.recycle();
	}
}
