package com.apps.LarmLarms;

import android.content.Context;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Class managing alarms and their behavior.
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

	/**
	 * Number of fields in a single edit string (don't have one for store strings because they're the
	 * same as edit strings + an extra field).
	*/
	private static final int NUM_EDIT_FIELDS = 4;

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

	// for REPEAT_DAY_WEEKLY
	private boolean[] repeatDays;

	// for REPEAT_DATE_MONTHLY and REPEAT_DAY_MONTHLY
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

		// TODO: use getActualDefaultRingtoneUri(context, type) or getDefaultRingtoneUri(type)?
		ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
	}

	public Alarm(Context currContext, String title) {
		this(currContext);
		setListableName(title);
	}

	/* ****************************  Methods from Listable  ******************************* */

	@Override
	public boolean isAlarm() { return true; }

	@Override
	public String getListableName() { return name; }
	@Override
	public void setListableName(String name) {
		if (name == null || name.equals("") || name.indexOf('\t') != -1) {
			Log.e(TAG, "New name is invalid.");
			return;
		}
		this.name = name;
	}

	@Override
	public String getRepeatString() {
		Resources res = context.getResources();

		StringBuilder repeatString = new StringBuilder();
		String date = DateFormat.getDateInstance(DateFormat.SHORT).format(ringTime.getTime());

		// TODO: perhaps don't use a StringBuilder to make these strings and use String.format instead?
		switch (repeatType) {
			case REPEAT_ONCE_ABS:
			case REPEAT_ONCE_REL:
				repeatString.append(res.getString(R.string.main_once_abs_rel_prefix));
				repeatString.append(date);
				break;
			case REPEAT_DAY_WEEKLY:
				repeatString.append(getWeeklyDisplayString());
				break;
			case REPEAT_DATE_MONTHLY:
				int dateOfMonth = ringTime.get(Calendar.DATE);
				String[] suffixStrings = res.getStringArray(R.array.alarm_date_suffix_strings);

				repeatString.append(res.getString(R.string.main_generic_repeat_string));
				repeatString.append(dateOfMonth);
				repeatString.append(suffixStrings[Math.min(dateOfMonth, suffixStrings.length) - 1]);
				repeatString.append(res.getString(R.string.main_monthly_suffix));
				repeatString.append(getExceptionMonthsString());
				break;
			case REPEAT_DAY_MONTHLY:
				repeatString.append(res.getString(R.string.main_generic_repeat_string));
				repeatString.append(res.getStringArray(R.array.alarm_week_strings)[repeatWeek]);
				repeatString.append(res.getString(R.string.space));
				repeatString.append(res.getStringArray(R.array.alarm_day_strings_long)[
						ringTime.get(Calendar.DAY_OF_WEEK) - 1]);
				repeatString.append(res.getString(R.string.main_monthly_suffix));
				repeatString.append(getExceptionMonthsString());
				break;
			case REPEAT_DATE_YEARLY:
				repeatString.append(res.getString(R.string.main_date_yearly_prefix));
				repeatString.append(date);
				break;
			case REPEAT_OFFSET:
				repeatString.append(res.getString(R.string.main_generic_repeat_string));
				repeatString.append(getOffsetString());
				repeatString.append(res.getString(R.string.main_offset_text_5));
				repeatString.append(date);
				break;
			default:
				Log.e(TAG, "Unknown repeat type!");
				break;
		}
		return repeatString.toString();
	}

	@Override
	public boolean isActive() { return alarmIsActive; }
	@Override
	public void turnOn() { alarmIsActive = true; }
	@Override
	public void turnOff() { alarmIsActive = false; }
	@Override
	public void toggleActive() { alarmIsActive = !alarmIsActive; }
	@Override
	public void setActive(boolean active) { alarmIsActive = active; }

	@Override
	public String getNextRingTime() {
		// TODO: uppercase/lowercase as a setting? I like lowercase.
		return DateFormat.getTimeInstance(DateFormat.SHORT).format(ringTime.getTime()).toLowerCase();
	}

	@Override
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

	@Override
	public String toEditString() {
		StringBuilder alarmString = new StringBuilder(name).append('\t');
		alarmString.append(Boolean.toString(alarmIsActive)).append('\t');

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

		alarmString.append('\t').append(Long.toString(ringTime.getTimeInMillis()));

		// TODO: encode the other parts in here (don't forget to add a tab char before it)

		return alarmString.toString();
	}

	@Override
	public String toStoreString() { return "a\t" + toEditString(); }

	/* *********************  Getter and Setter Methods  *************************** */

	Calendar getAlarmTimeCalendar() { return ringTime; }
	long getAlarmTimeMillis() { return ringTime.getTimeInMillis(); }
	void setAlarmTimeMillis(long time) {
		if (time < 0) {
			Log.e(TAG, "New calendar time was negative.");
			return;
		}
		ringTime.setTimeInMillis(time);
	}

	int getRepeatType() { return repeatType; }
	void setRepeatType(int type) {
		if (type < 0 || type >= NUM_REPEAT_TYPES) {
			Log.e(TAG, "Repeat type is invalid.");
			return;
		}
		repeatType = type;
		updateRingTime();
	}

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

	int getRepeatWeek() { return repeatWeek; }
	void setRepeatWeek(int newWeek) {
		if (newWeek < 0 || newWeek >= 5) {
			Log.e(TAG, "New week to repeat on is invalid.");
			return;
		}
		repeatWeek = newWeek;
		updateRingTime();
	}

	int getOffsetHours() { return offsetHours; }
	void setOffsetHours(int hours) {
		if (hours < 0 || hours > 24) {
			Log.e(TAG, "New number of hours is invalid.");
			return;
		}
		offsetHours = hours;
		updateRingTime();
	}

	int getOffsetDays() { return offsetDays; }
	void setOffsetDays(int days) {
		if (days < 0) {
			Log.e(TAG, "New number of days is invalid.");
			return;
		}
		offsetDays = days;
		updateRingTime();
	}

	int getOffsetMins() { return offsetMins; }
	void setOffsetMins(int min) {
		if (min < 0 || min > 60) {
			Log.e(TAG, "New number of minutes is invalid.");
			return;
		}
		offsetMins = min;
	}

	boolean isVibrateOn() { return alarmVibrateIsOn; }
	void setVibrateOn(boolean on) { alarmVibrateIsOn = on; }

	boolean isSoundOn() { return alarmSoundIsOn; }
	void setSoundOn(boolean on) { alarmSoundIsOn = on; }

	Uri getRingtone() { return ringtoneUri; }
	void setRingtone(Uri newRingtone) {
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

	static Alarm fromEditString(Context currContext, String src) {
		if (src == null) {
			Log.e(TAG, "Edit string is null.");
			return null;
		} else if (src.length() == 0) {
			Log.e(TAG, "Edit string is empty.");
			return null;
		}

		String[] fields = src.split("\t");
		if (fields.length != NUM_EDIT_FIELDS) {
			Log.e(TAG, "Edit string didn't have a correct number of fields.");
			return null;
		}

		// TODO: what happens if one of these fails?
		Alarm res = new Alarm(currContext, fields[0]);
		res.setActive(Boolean.parseBoolean(fields[1]));		// doesn't throw anything

		// res.setRepeatType(Integer.parseInt(fields[2]));
		String[] repeatTypeInfo = fields[2].split(" ");
		res.setRepeatType(Integer.parseInt(repeatTypeInfo[0]));		// throws NumberFormatException
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
				res.offsetDays = Integer.parseInt(repeatTypeInfo[1]);
				res.offsetHours = Integer.parseInt(repeatTypeInfo[2]);
				res.offsetMins = Integer.parseInt(repeatTypeInfo[3]);
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

		return res;
	}

	// edit strings and store strings are pretty much the same for Alarms
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

		StringBuilder days = new StringBuilder(res.getString(R.string.main_weekly_prefix));
		String separator = res.getString(R.string.separator);
		String[] dayStrings = res.getStringArray(R.array.alarm_day_strings_short);

		boolean everyDayFlag = true, weekdayFlag = true, weekendFlag = true;

		for (int i = 0; i < repeatDays.length; i++) {
			if (repeatDays[i]) {
				days.append(dayStrings[i]).append(separator);
			}
			everyDayFlag &= repeatDays[i];
			if (i == 0 || i == 6) { weekendFlag &= repeatDays[i]; }
			else { weekdayFlag &= repeatDays[i]; }
		}

		// special cases
		if (days.toString().isEmpty()) { return res.getString(R.string.main_no_repeats_string); }
		else if (everyDayFlag) { return res.getString(R.string.main_weekly_everyday); }
		else if (weekdayFlag) { return res.getString(R.string.main_weekly_weekdays); }
		else if (weekendFlag) { return res.getString(R.string.main_weekly_weekends); }

		days.delete(days.length() - separator.length(), days.length());		// delete the last comma
		return days.toString();
	}

	private String getExceptionMonthsString() {
		Resources res = context.getResources();

		StringBuilder months = new StringBuilder();
		String separator = res.getString(R.string.separator);
		String[] monthStrings = res.getStringArray(R.array.alarm_month_strings);

		for (int i = 0; i < repeatMonths.length; i++) {
			if (!repeatMonths[i]) {
				months.append(monthStrings[i]);
				months.append(res.getString(R.string.separator));
			}
		}
		if (months.toString().isEmpty()) { return ""; }

		months.delete(months.length() - separator.length(), months.length());	// delete the last comma
		months.insert(0, res.getString(R.string.main_monthly_exception_text));
		return months.toString();
	}

	/**
	 * Builds a string for offsets.
	 */
	private String getOffsetString() {
		boolean deleteLastComma = false;
		StringBuilder offsetString = new StringBuilder();
		String separator = context.getResources().getString(R.string.separator);

		if (offsetDays != 0) {
			offsetString.append(offsetDays);
			offsetString.append(context.getResources().getString(R.string.main_offset_text_2)).append(separator);
			deleteLastComma = true;
		}
		if (offsetHours != 0) {
			offsetString.append(offsetHours);
			offsetString.append(context.getResources().getString(R.string.main_offset_text_3)).append(separator);
			deleteLastComma = true;
		}
		if (offsetMins != 0) {
			offsetString.append(offsetMins);
			offsetString.append(context.getResources().getString(R.string.main_offset_text_4));
			deleteLastComma = false;
		}
		if (offsetString.toString().isEmpty()) {
			return context.getResources().getString(R.string.main_no_repeats_string);
		}

		if (deleteLastComma) {
			offsetString.delete(offsetString.length() - separator.length(), offsetString.length());
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
		if (!alarmIsActive) { return; }

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
}
