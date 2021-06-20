package com.apps.LarmLarms;

import android.content.Context;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Class managing alarms and their behavior.
 * TODO: implement Parcelable so edit strings become unnecessary?
 */
public final class Alarm implements Listable, Cloneable {
	/* ************************************  Static Fields  *********************************** */

	private final static String TAG = "Alarm";

	// repeat types
	static final int REPEAT_ONCE_ABS = 0;
	static final int REPEAT_ONCE_REL = 1;
	static final int REPEAT_DAY_WEEKLY = 2;
	static final int REPEAT_DATE_MONTHLY = 3;
	static final int REPEAT_DAY_MONTHLY = 4;
	static final int REPEAT_DATE_YEARLY = 5;
	static final int REPEAT_OFFSET = 6;

	private static final int NUM_REPEAT_TYPES = 7;

	/* ***********************************  Instance Fields  ******************************** */

	private Context context;

	/**
	 * Stores the name of the alarm. Only restricted character: tabs. If tabs are present, the
	 * class will log errors.
	 */
	private String name;
	private int repeatType;

	/**
	 * Represents the next repeat time for the Alarm. Decided on a Calendar because AlarmManager asks
	 * for longs (easily convertible) and it helps with fetching different fields. There are certain
	 * guaranteed Calendar fields which are maintained dependent on repeat type.
	 * <br>
	 * Used in all repeatTypes. Check paper pg 2 for a more detailed description.
	 */
	private Calendar ringTime;

	/**
	 * Used for REPEAT_DAY_WEEKLY. Is an array with length 7 whose indices correspond to Calendar
	 * day constants if you have to add 1 to the indices (or subtract 1 from the constants).
	 */
	private boolean[] repeatDays;

	/**
	 * Used for REPEAT_DATE_MONTHLY and REPEAT_DAY_MONTHLY. Is an array with length 12 whose indices
	 * correspond to Calendar month constants.
	 */
	private boolean[] repeatMonths;

	// for REPEAT_DAY_MONTHLY
	private int repeatWeek;

	// for REPEAT_ONCE_REL and REPEAT_OFFSET
	private int offsetDays;
	private int offsetHours;
	private int offsetMins;

	/**
	 * the alarm's personal on/off button (NOT the sum total of folder masks)
	 */
	private boolean alarmIsActive;

	private boolean alarmSnoozed;
	private int numSnoozes;

	private boolean alarmVibrateIsOn;

	private boolean alarmSoundIsOn;
	private Uri ringtoneUri;

	/* **********************************  Constructors  ********************************* */

	// Alarms are always valid, though it can have dummy data in them instead of real data
	public Alarm(Context currContext) {
		// TODO: Dummy data for other fields
		context = currContext;
		name = "default name";

		ringTime = new GregorianCalendar();
		// ringTime.setLenient(false);

		repeatType = REPEAT_ONCE_ABS;

		// should be 7 trues and 12 trues
		repeatDays = new boolean[] {true, true, true, true, true, true, true};
		repeatMonths = new boolean[] {true, true, true, true, true, true, true, true, true, true, true, true};
		repeatWeek = 0;

		offsetDays = 1;
		offsetHours = 0;
		offsetMins = 0;

		alarmVibrateIsOn = true;
		alarmSoundIsOn = true;
		alarmIsActive = true;

		alarmSnoozed = false;
		numSnoozes = 0;

		// TODO: use getActualDefaultRingtoneUri(context, type) or getDefaultRingtoneUri(type)?
		ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
	}

	public Alarm(Context currContext, String title) {
		this(currContext);
		setListableName(title);
	}

	/* ****************************  Methods from Listable  ******************************* */

	@Override @Contract(pure = true)
	public boolean isAlarm() { return true; }

	@Override @Contract(pure = true)
	public String getListableName() { return name; }
	@Override
	public void setListableName(String name) {
		if (name == null || name.equals("") || name.indexOf('\t') != -1) {
			Log.e(TAG, "New name is invalid.");
			return;
		}
		this.name = name;
	}

	@NotNull @Override
	public String getRepeatString() {
		Resources res = context.getResources();

		// TODO: show that the alarm is snoozed if it is

		String repeatString = "";
		String dateStr = DateFormat.getDateInstance(DateFormat.SHORT).format(ringTime.getTime());
		String[] ordinals = res.getStringArray(R.array.alarm_ordinals);

		switch (repeatType) {
			case REPEAT_ONCE_ABS:
			case REPEAT_ONCE_REL:
				repeatString = String.format(res.getString(R.string.alarm_once_abs_rel), dateStr);
				break;
			case REPEAT_DAY_WEEKLY:
				repeatString = getWeeklyDisplayString();
				break;
			case REPEAT_DATE_MONTHLY:
				int dateOfMonth = ringTime.get(Calendar.DATE);
				repeatString = String.format(res.getString(R.string.alarm_date_monthly),
						ordinals[dateOfMonth - 1], getExceptionMonthsString());
				break;
			case REPEAT_DAY_MONTHLY:
				String[] weekdays = (new DateFormatSymbols()).getWeekdays();
				repeatString = String.format(res.getString(R.string.alarm_day_monthly),
						res.getStringArray(R.array.alarm_week_strings)[repeatWeek],
						weekdays[ringTime.get(Calendar.DAY_OF_WEEK)],
						getExceptionMonthsString());
				break;
			case REPEAT_DATE_YEARLY:
				// TODO: don't show the year
				repeatString = String.format(res.getString(R.string.alarm_date_yearly), dateStr);
				break;
			case REPEAT_OFFSET:
				repeatString = String.format(res.getString(R.string.alarm_offset),
						getOffsetString(), dateStr);
				break;
			default:
				Log.e(TAG, "Unknown repeat type!");
				break;
		}
		return repeatString;
	}

	@Override @Contract(pure = true)
	public boolean isActive() { return alarmIsActive; }
	@Override
	public void turnOn() { alarmIsActive = true; }
	@Override
	public void turnOff() { alarmIsActive = false; }
	@Override
	public void toggleActive() { alarmIsActive = !alarmIsActive; }
	@Override
	public void setActive(boolean active) { alarmIsActive = active; }

	@NotNull @Override
	public String getNextRingTime() {
		// TODO: uppercase/lowercase as a setting? I like lowercase.
		return DateFormat.getTimeInstance(DateFormat.SHORT).format(ringTime.getTime()).toLowerCase();
	}

	@Override @Contract(pure = true)
	public int getNumItems() { return 1; }

	@Override
	public Listable clone() {
		Alarm that = null;
		try {
			that = (Alarm) super.clone();

			// TODO: deep copies for other object fields
			that.ringTime = (Calendar) this.ringTime.clone();
		} catch (CloneNotSupportedException e) { e.printStackTrace(); }

		return that;
	}

	/**
	 * Creates an edit string from the current alarm. For edit string format, see fromEditString(Context,
	 * String)
	 */
	@NotNull @Override
	public String toEditString() {
		StringBuilder alarmString = new StringBuilder(name).append('\t');
		alarmString.append(alarmIsActive).append('\t');

		switch (repeatType) {
			case REPEAT_ONCE_ABS:
				alarmString.append(REPEAT_ONCE_ABS);
				break;
			case REPEAT_ONCE_REL:
				alarmString.append(REPEAT_ONCE_REL);
				alarmString.append(' ').append(offsetDays);
				alarmString.append(' ').append(offsetHours);
				alarmString.append(' ').append(offsetMins);
				break;
			case REPEAT_DAY_WEEKLY:
				alarmString.append(REPEAT_DAY_WEEKLY);
				for (int i = 0; i < 7; i++) { alarmString.append(' ').append(repeatDays[i]); }
				break;
			case REPEAT_DATE_MONTHLY:
				alarmString.append(REPEAT_DATE_MONTHLY);
				for (int i = 0; i < 12; i++) { alarmString.append(' ').append(repeatMonths[i]); }
				break;
			case REPEAT_DAY_MONTHLY:
				alarmString.append(REPEAT_DAY_MONTHLY);
				alarmString.append(' ').append(repeatWeek);
				for (int i = 0; i < 12; i++) { alarmString.append(' ').append(repeatMonths[i]); }
				break;
			case REPEAT_DATE_YEARLY:
				alarmString.append(REPEAT_DATE_YEARLY);
				break;
			case REPEAT_OFFSET:
				alarmString.append(REPEAT_OFFSET);
				alarmString.append(' ').append(offsetDays);
				alarmString.append(' ').append(offsetHours);
				alarmString.append(' ').append(offsetMins);
				break;
			default:
				// TODO: Any better ways to handle the invalid case? Throw an exception?
				Log.e(TAG, "Invalid alarm repeat type.");
				return "";
		}

		alarmString.append('\t').append(ringTime.getTimeInMillis());
		alarmString.append('\t');
		if (ringtoneUri == null)
			alarmString.append(context.getResources().getString(R.string.alarm_editor_silent));
		else
			alarmString.append(ringtoneUri.toString());

		alarmString.append('\t').append(alarmSnoozed);
		alarmString.append('\t').append(numSnoozes);
		// TODO: encode the other parts in here (don't forget to add a tab char before it)

		return alarmString.toString();
	}

	@NotNull @Override
	public String toStoreString() { return "a\t" + toEditString(); }

	/* *********************  Getter and Setter Methods  *************************** */

	@Contract(pure = true)
	Calendar getAlarmTimeCalendar() { return ringTime; }
	long getAlarmTimeMillis() { return ringTime.getTimeInMillis(); }
	void setAlarmTimeMillis(long time) {
		if (time < 0) {
			Log.e(TAG, "New calendar time was negative.");
			return;
		}
		ringTime.setTimeInMillis(time);
	}

	@Contract(pure = true)
	int getRepeatType() { return repeatType; }
	void setRepeatType(int type) {
		if (type < 0 || type >= NUM_REPEAT_TYPES) {
			Log.e(TAG, "Repeat type is invalid.");
			return;
		}
		repeatType = type;
		updateRingTime();
	}

	@Contract(pure = true)
	boolean[] getRepeatDays() { return repeatDays; }
	boolean getRepeatDays(int index) {
		if (index < 0 || index >= 7) {
			// TODO: better way to handle?
			Log.e(TAG, "Index given to setRepeatDays is invalid.");
			return false;
		}
		return repeatDays[index];
	}
	void setRepeatDays(int index, boolean active) {
		if (index < 0 || index >= 7) {
			Log.e(TAG, "Index given to setRepeatDays is invalid.");
			return;
		}
		repeatDays[index] = active;
		updateRingTime();
	}

	@Contract(pure = true)
	boolean[] getRepeatMonths() { return repeatMonths; }
	boolean getRepeatMonths(int index) {
		if (index < 0 || index >= 12) {
			// TODO: better way to handle?
			Log.e(TAG, "Index given to setRepeatMonths is invalid.");
			return false;
		}
		return repeatMonths[index];
	}
	void setRepeatMonths(int index, boolean active) {
		if (index < 0 || index >= 12) {
			Log.e(TAG, "Index given to setRepeatMonths is invalid.");
			return;
		}
		repeatMonths[index] = active;
		updateRingTime();
	}

	@Contract(pure = true)
	int getRepeatWeek() { return repeatWeek; }
	void setRepeatWeek(int newWeek) {
		if (newWeek < 0 || newWeek >= 5) {
			Log.e(TAG, "New week to repeat on is invalid.");
			return;
		}
		repeatWeek = newWeek;
		updateRingTime();
	}

	@Contract(pure = true)
	int getOffsetHours() { return offsetHours; }
	void setOffsetHours(int hours) {
		if (hours < 0 || hours > 24) {
			Log.e(TAG, "New number of hours is invalid.");
			return;
		}
		offsetHours = hours;
		updateRingTime();
	}

	@Contract(pure = true)
	int getOffsetDays() { return offsetDays; }
	void setOffsetDays(int days) {
		if (days < 0) {
			Log.e(TAG, "New number of days is invalid.");
			return;
		}
		offsetDays = days;
		updateRingTime();
	}

	@Contract(pure = true)
	int getOffsetMins() { return offsetMins; }
	void setOffsetMins(int min) {
		if (min < 0 || min > 60) {
			Log.e(TAG, "New number of minutes is invalid.");
			return;
		}
		offsetMins = min;
	}

	@Contract(pure = true)
	boolean getIsSnoozed() { return alarmSnoozed; }

	@Contract(pure = true)
	boolean isVibrateOn() { return alarmVibrateIsOn; }
	void setVibrateOn(boolean on) { alarmVibrateIsOn = on; }

	@Contract(pure = true)
	boolean isSoundOn() { return alarmSoundIsOn; }
	void setSoundOn(boolean on) { alarmSoundIsOn = on; }

	@Contract(pure = true)
	Uri getRingtoneUri() { return ringtoneUri; }
	void setRingtoneUri(Uri newRingtone) {
		if (newRingtone == null) {
			Log.i(TAG, "The new ringtone is silent.");
		}
		ringtoneUri = newRingtone;
	}
	String getRingtoneName() {
		Ringtone r = RingtoneManager.getRingtone(context, ringtoneUri);
		if (r == null) {
			return context.getResources().getString(R.string.alarm_editor_silent);
		}
		return r.getTitle(context);
	}

	/* ************************************  Static Methods  ********************************** */

	/**
	 * Returns a new Alarm based on the given string. Current edit string format (separated by tabs):
	 * [alarm title]	[active]	[repeat info]	[next ring time]	[ringtone uri]	[is snoozed]
	 * [number of snoozes]
	 * <br>
	 * Repeat type info format (separated by spaces): [type] [type-specific data]
	 * ONCE_ABS and DATE_YEARLY: none
	 * ONCE_REL and OFFSET: [days] [hours] [mins]
	 * DAY_WEEKLY: [true/false for every day]
	 * DAY_MONTHLY: [week to repeat] [true/false for every month]
	 * DATE_MONTHLY: [true/false for every month]
	 * @param context current operating context
	 * @param src edit string to build an Alarm out of
	 * @return new Alarm based on the edit string
	 */
	@Nullable
	static Alarm fromEditString(Context context, String src) {
		if (src == null) {
			Log.e(TAG, "Edit string is null.");
			return null;
		} else if (src.length() == 0) {
			Log.e(TAG, "Edit string is empty.");
			return null;
		}

		String[] fields = src.split("\t");
		if (fields.length != 7) {
			Log.e(TAG, "Edit string didn't have a correct number of fields.");
			return null;
		}

		Alarm res = new Alarm(context, fields[0]);
		res.setActive(Boolean.parseBoolean(fields[1]));		// doesn't throw anything

		String[] repeatTypeInfo = fields[2].split(" ");
		try {
			res.setRepeatType(Integer.parseInt(repeatTypeInfo[0]));
		}
		catch (NumberFormatException e) {
			Log.e(TAG, "Edit string has an incorrectly formatted repeat type.");
			return null;
		}
		switch(res.repeatType) {
			case REPEAT_DAY_WEEKLY:
				if (repeatTypeInfo.length != 8) {
					Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				for (int i = 0; i < 7; i++) {
					res.repeatDays[i] = Boolean.parseBoolean(repeatTypeInfo[i + 1]);
				}
				break;
			case REPEAT_DAY_MONTHLY:
				if (repeatTypeInfo.length != 14) {
					Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				res.repeatWeek = Integer.parseInt(repeatTypeInfo[1]);
				for (int i = 0; i < 12; i++) {
					res.repeatMonths[i] = Boolean.parseBoolean(repeatTypeInfo[i + 2]);
				}
				break;
			case REPEAT_DATE_MONTHLY:
				if (repeatTypeInfo.length != 13) {
					Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				for (int i = 0; i < 12; i++) {
					res.repeatMonths[i] = Boolean.parseBoolean(repeatTypeInfo[i + 1]);
				}
				break;
			case REPEAT_ONCE_REL:
			case REPEAT_OFFSET:
				if (repeatTypeInfo.length != 4) {
					Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				try {
					res.offsetDays = Integer.parseInt(repeatTypeInfo[1]);
					res.offsetHours = Integer.parseInt(repeatTypeInfo[2]);
					res.offsetMins = Integer.parseInt(repeatTypeInfo[3]);
				}
				catch (NumberFormatException e) {
					Log.e(TAG, "Edit string has incorrectly formatted offsets.");
					return null;
				}
				break;
			case REPEAT_ONCE_ABS:
			case REPEAT_DATE_YEARLY:
				if (repeatTypeInfo.length != 1) {
					Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				break;
			default:
				Log.e(TAG, "Edit string had an unknown repeat type.");
				return null;
		}

		res.ringTime.setTimeInMillis(Long.parseLong(fields[3]));
		if (context.getResources().getString(R.string.alarm_editor_silent).equals(fields[4]))
			res.setRingtoneUri(null);
		else
			res.setRingtoneUri(Uri.parse(fields[4]));

		res.alarmSnoozed = Boolean.parseBoolean(fields[5]);

		try {
			res.numSnoozes = Integer.parseInt(fields[6]);
		}
		catch (NumberFormatException e) {
			Log.e(TAG, "Edit string has an incorrectly formatted number of snoozes.");
			return null;
		}

		return res;
	}

	/**
	 * Creates a new alarm from a stored Alarm string. Edit strings and store strings are pretty
	 * much the same for Alarms. Current store string format (separated by tabs):
	 * a	[edit string]
	 *
	 * @param currContext current operating context
	 * @param src store string to build an Alarm out of
	 * @return A new Alarm based on the store string
	 */
	static Alarm fromStoreString(Context currContext, String src) {
		if (src == null) {
			Log.e(TAG, "Store string is null.");
			return null;
		} else if (src.length() == 0) {
			Log.e(TAG, "Store string is empty.");
			return null;
		}

		if (!src.startsWith("a\t")) {
			Log.e(TAG, "Store string has an unknown ID field.");
			return null;
		}
		return fromEditString(currContext, src.substring(2));		// removes the "a\t"
	}

	/* *********************************  Other Methods  ********************************** */

	@Override
	public String toString() { return name; }

	private String getWeeklyDisplayString() {
		Resources res = context.getResources();

		StringBuilder days = new StringBuilder();
		String separator = res.getString(R.string.separator);
		String[] dayStrings = (new DateFormatSymbols()).getShortWeekdays();

		boolean everyDayFlag = true, weekdayFlag = true, weekendFlag = true;

		for (int i = 0; i < repeatDays.length; i++) {
			if (repeatDays[i]) {
				days.append(dayStrings[i + 1]).append(separator);
			}
			everyDayFlag &= repeatDays[i];
			if (i + 1 == Calendar.SATURDAY || i + 1 == Calendar.SUNDAY) { weekendFlag &= repeatDays[i]; }
			else { weekdayFlag &= repeatDays[i]; }
		}

		// special cases
		if (days.toString().isEmpty()) { return res.getString(R.string.alarm_no_repeats_string); }
		else if (everyDayFlag) { return res.getString(R.string.alarm_weekly_everyday); }
		else if (weekdayFlag) { return res.getString(R.string.alarm_weekly_weekdays); }
		else if (weekendFlag) { return res.getString(R.string.alarm_weekly_weekends); }

		days.delete(days.length() - separator.length(), days.length());		// delete the last separator
		return String.format(res.getString(R.string.alarm_weekly), days);
	}

	/**
	 * Returns the exception months string, or empty string if no exceptions.
	 * @return formatted string resource main_monthly_exception or empty
	 */
	@NotNull
	private String getExceptionMonthsString() {
		Resources res = context.getResources();

		StringBuilder months = new StringBuilder();
		String separator = res.getString(R.string.separator);
		String[] monthStrings = (new DateFormatSymbols()).getShortMonths();

		for (int i = 0; i < repeatMonths.length; i++) {
			if (!repeatMonths[i]) {
				months.append(monthStrings[i]);
				months.append(separator);
			}
		}
		if (months.length() == 0) { return ""; }
		// TODO: other month exceptions?

		months.delete(months.length() - separator.length(), months.length());	// delete the last comma
		return String.format(res.getString(R.string.alarm_monthly_exception), months.toString());
	}

	/**
	 * Builds a string for offsets. Returns a new string with days, hours, and minutes.
	 */
	private String getOffsetString() {
		boolean previous = false;
		StringBuilder offsetString = new StringBuilder();
		Resources res = context.getResources();
		String separator = res.getString(R.string.separator);
		String finalSeparator = context.getResources().getString(R.string.final_separator);

		if (offsetDays != 0) {
			offsetString.append(String.format(res.getString(R.string.alarm_offset_days), offsetDays));
			previous = true;
		}
		if (offsetHours != 0) {
			if (previous) offsetString.append(separator);
			offsetString.append(String.format(res.getString(R.string.alarm_offset_hours), offsetHours));
			previous = true;
		}
		if (offsetMins != 0) {
			if (previous) offsetString.append(finalSeparator);
			offsetString.append(String.format(res.getString(R.string.alarm_offset_mins), offsetMins));
		}
		if (offsetString.length() == 0) {
			return res.getString(R.string.alarm_no_repeats_string);
		}

		return offsetString.toString();
	}

	/**
	 * Updates the ring time, setting it to the next time it should ring. Will not update if the alarm
	 * is not active. If the alarm is ONCE_ABS, the method will not do anything. If the alarm is ONCE_REL,
	 * and ringTime has passed, will reset to offset after current time. Tries not to rely on the
	 * previous value of ringTime, but will take values of guaranteed stored constants (except OFFSET,
	 * which will use the entirety of the previous ring time).
	 *
	 * NOTE: if the date doesn't exist (ex. April 31 for DATE_MONTHLY), it will simply skip it (will
	 * not schedule an alarm for May 1)
	 *
	 * TODO: could perhaps do something if there are no repeat results found (when everything is unchecked?)
	 */
	void updateRingTime() {
		if (!alarmIsActive || alarmSnoozed) { return; }

		GregorianCalendar sysClock = new GregorianCalendar(), workingClock = new GregorianCalendar();
		int thisMonth = sysClock.get(Calendar.MONTH), currMonth = thisMonth;

		// common to all the repeat types that change workingClock
		workingClock.set(Calendar.HOUR_OF_DAY, ringTime.get(Calendar.HOUR_OF_DAY));
		workingClock.set(Calendar.MINUTE, ringTime.get(Calendar.MINUTE));

		// use break to set workingClock to ringTime, return to not
		switch(repeatType) {
			case REPEAT_ONCE_ABS:
				return;
			case REPEAT_ONCE_REL:
				// only changes if the alarm is overdue
				if (ringTime.after(sysClock)) { return; }
				workingClock.add(Calendar.DAY_OF_MONTH, offsetDays);
				workingClock.add(Calendar.HOUR_OF_DAY, offsetHours);
				workingClock.add(Calendar.MINUTE, offsetMins);
				break;
			case REPEAT_DAY_WEEKLY:
				// trying to avoid any namespace issues
				{
					int dayOfWeek = sysClock.get(Calendar.DAY_OF_WEEK), todayDayOfWeek = dayOfWeek;

					while (workingClock.before(sysClock) || !repeatDays[dayOfWeek - 1]) {
						dayOfWeek = dayOfWeek % 7 + 1;
						workingClock.set(Calendar.DAY_OF_WEEK, dayOfWeek);
						if (dayOfWeek == todayDayOfWeek) { return; }
					}
				}
				break;
			case REPEAT_DATE_MONTHLY:
				workingClock.set(Calendar.DAY_OF_MONTH, ringTime.get(Calendar.DAY_OF_MONTH));

				while (workingClock.before(sysClock) || !repeatMonths[currMonth] ||
						workingClock.get(Calendar.DAY_OF_MONTH) != ringTime.get(Calendar.DAY_OF_MONTH)) {
					// go to the next month
					currMonth = (currMonth + 1) % 12;
					workingClock.set(Calendar.MONTH, currMonth);
					workingClock.set(Calendar.DAY_OF_MONTH, ringTime.get(Calendar.DAY_OF_MONTH));
					if (currMonth == thisMonth) { return; }
				}
				break;
			case REPEAT_DAY_MONTHLY:
				if (repeatWeek == 4) {
					// the last week of the month
					workingClock.set(Calendar.DAY_OF_WEEK_IN_MONTH, -1);
				}
				else {
					workingClock.set(Calendar.DAY_OF_WEEK_IN_MONTH, repeatWeek + 1);
				}

				workingClock.set(Calendar.DAY_OF_WEEK, ringTime.get(Calendar.DAY_OF_WEEK));

				while (workingClock.before(sysClock) || !repeatMonths[currMonth]) {
					currMonth = (currMonth + 1) % 12;
					workingClock.set(Calendar.MONTH, currMonth);
					if (currMonth == thisMonth) { return; }
				}
				break;
			case REPEAT_DATE_YEARLY:
				{
					int day = ringTime.get(Calendar.DAY_OF_MONTH);
					int month = ringTime.get(Calendar.MONTH);
					workingClock.set(Calendar.DAY_OF_MONTH, day);
					workingClock.set(Calendar.MONTH, month);

					while (workingClock.before(sysClock) || workingClock.get(Calendar.DAY_OF_MONTH) != day ||
							workingClock.get(Calendar.MONTH) != month) {
						workingClock.add(Calendar.YEAR, 1);
						workingClock.set(Calendar.DAY_OF_MONTH, day);
						workingClock.set(Calendar.MONTH, month);
					}
				}
				break;
			case REPEAT_OFFSET:
				ringTime.add(Calendar.DAY_OF_MONTH, offsetDays);
				ringTime.add(Calendar.HOUR_OF_DAY, offsetHours);
				ringTime.add(Calendar.MINUTE, offsetMins);
				return;
			default:
				Log.wtf(TAG, "Somehow the repeat type within the Alarm is wrong.");
				return;
		}
		ringTime.setTimeInMillis(workingClock.getTimeInMillis());
	}

	void snooze() {
		alarmSnoozed = true;
		numSnoozes++;
		// TODO: change number of minutes to snooze?
		ringTime.add(Calendar.MINUTE, 5);
	}

	/**
	 * Unsnoozes the alarm if it was snoozed previously. Won't do anything if it wasn't snoozed.
	 */
	void unsnooze() {
		alarmSnoozed = false;

		ringTime.add(Calendar.MINUTE, -5*numSnoozes);
		numSnoozes = 0;
	}
}
