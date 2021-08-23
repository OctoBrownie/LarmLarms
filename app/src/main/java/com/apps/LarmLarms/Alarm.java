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

	/* ************************************  Constants  *********************************** */

	/**
	 * Debug constant, enables/disables log messages.
	 */
	private final static boolean DEBUG = false;

	/**
	 * Tag of the class for logging purposes.
	 */
	private final static String TAG = "Alarm";

	/**
	 * Represents an alarm that doesn't repeat (rings once) and is specified by an absolute date/time.
	 * Only uses field ringTime.
	 */
	static final int REPEAT_ONCE_ABS = 0;
	/**
	 * Represents an alarm that doesn't repeat (rings once) and is specified by an offset from the
	 * current time (relative date/time). Uses field ringTime for storing date/time, and offset
	 * variables for storing the offset from creation/modification time.
	 */
	static final int REPEAT_ONCE_REL = 1;
	/**
	 * Represents an alarm that repeats at a given time on specific days every week. Uses field
	 * ringTime to specify the time to ring and repeatDays for the days of the week to ring.
	 */
	static final int REPEAT_DAY_WEEKLY = 2;
	/**
	 * Represents an alarm that repeats on a specific date (a number) every month. Uses field
	 * ringTime for the date (not month) and time of the alarm, repeatMonths for the months to ring
	 * on (can skip months).
	 */
	static final int REPEAT_DATE_MONTHLY = 3;
	/**
	 * Represents an alarm that repeats on a specific day (day of a week) every month. Uses field
	 * ringTime for the time of the alarm and the day of the week, repeatMonths for the months to
	 * ring on (can skip months), and repeatWeek for the week to ring on (first week, second week,
	 * last week, etc).
	 */
	static final int REPEAT_DAY_MONTHLY = 4;
	/**
	 * Represents an alarm that rings at a certain date/time every year. Only uses field ringTime.
	 */
	static final int REPEAT_DATE_YEARLY = 5;
	/**
	 * Represents an alarm that repeats every offset from the previous time. Uses field ringTime to
	 * store date/time of the next ring time, and offset variables to store the offset to generate
	 * the next ring time (add them to ringTime).
	 */
	static final int REPEAT_OFFSET = 6;

	/**
	 * Total number of repeat types, used for validating data (repeat type number has to be under
	 * NUM_REPEAT_TYPES but greater than or equal to 0).
	 */
	private static final int NUM_REPEAT_TYPES = 7;

	/* ***********************************  Instance Fields  ******************************** */

	/**
	 * Used for getting string resources (for display strings). Can be null.
	 */
	@Nullable
	private Context context;

	/**
	 * Stores the name of the alarm, cannot be null. Only restricted character: tabs.
	 */
	@NotNull
	private String name;

	/**
	 * Stores the repeat type of the current alarm. Should be one of the repeat types in the
	 * "Constants" section.
	 */
	private int repeatType;

	/**
	 * Represents the next repeat time for the Alarm, cannot be null. There are certain Calendar
	 * fields guaranteed to be correct which are maintained based on repeat type.
	 * <br/>
	 * Used in all repeatTypes. Check paper pg 2 or the specific repeat type documentation for a
	 * more detailed description.
	 */
	@NotNull
	private Calendar ringTime;

	/**
	 * Used for REPEAT_DAY_WEEKLY. Is an array with length 7 (cannot be null, shouldn't be any larger
	 * or smaller) whose indices correspond to Calendar day constants if you add 1 to the indices
	 * (or subtract 1 from the constants).
	 */
	@NotNull
	private boolean[] repeatDays;

	/**
	 * Used for REPEAT_DATE_MONTHLY and REPEAT_DAY_MONTHLY. Is an array with length 12 (cannot be
	 * null, shouldn't be any larger or smaller) whose indices correspond to Calendar month constants.
	 */
	@NotNull
	private boolean[] repeatMonths;

	/**
	 * Used for REPEAT_DAY_MONTHLY. Stores the week to repeat on every month (first week, second
	 * week, last week, etc.) based on the string array alarm_week_strings.
	 */
	private int repeatWeek;

	/**
	 * Used for REPEAT_ONCE_REL and REPEAT_OFFSET. Stores the days to offset by, always 0 or above.
	 */
	private int offsetDays;
	/**
	 * Used for REPEAT_ONCE_REL and REPEAT_OFFSET. Stores the hours to offset by, always between 0
	 * and 23.
	 */
	private int offsetHours;
	/**
	 * Used for REPEAT_ONCE_REL and REPEAT_OFFSET. Stores the minutes to offset by, always between 0
	 * and 59.
	 */
	private int offsetMins;

	/**
	 * The alarm's personal on/off button (NOT the sum total of folder masks).
	 */
	private boolean alarmIsActive;

	/**
	 * Stores whether the alarm is currently snoozed or not. If so, should have a nonzero number of
	 * snoozes in numSnoozes. Otherwise, numSnoozes should be 0.
	 */
	private boolean alarmSnoozed;
	/**
	 * The number of times this alarm has been snoozed. Only should be nonzero when alarmSnoozed is
	 * true.
	 */
	private int numSnoozes;

	/**
	 * Represents whether vibrate is active when the alarm rings.
	 */
	private boolean alarmVibrateIsOn;

	/**
	 * Represents whether sound should be on when the alarm rings.
	 */
	private boolean alarmSoundIsOn;
	/**
	 * The URI of the ringtone of the alarm.
	 */
	@Nullable
	private Uri ringtoneUri;

	/* **********************************  Constructors  ********************************* */

	/**
	 * Creates a new alarm with a null context and a default name. Created Alarms should always 
	 * have valid data, though they have lots of dummy data when first created. 
	 */
	public Alarm() {
		this(null, "default name");
	}
	
	/**
	 * Creates a new alarm with the current context. Created Alarms should always have valid data,
	 * though they have lots of dummy data when first created.
	 * @param currContext the context this alarm exists in
	 */
	public Alarm(@Nullable Context currContext) {
		this(currContext, "default name");
	}

	/**
	 * Creates a new alarm with the current context and the given title. Created Alarms should 
	 * always have valid data, though they have lots of dummy data when first created. 
	 * @param currContext the context this alarm exists in, can be null
	 * @param title the name of the alarm, shouldn't be null
	 */
	public Alarm(@Nullable Context currContext, @Nullable String title) {
		// TODO: Dummy data for other fields
		context = currContext;
		if (title == null) name = "default name";
		else name = title;

		ringTime = new GregorianCalendar();

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
		if (context != null)
			ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
	}

	/* ********************************  Methods from Listable  ********************************** */

	/**
	 * Returns whether this is an alarm or not. 
	 * @return always returns true 
	 */
	@Override @Contract(pure = true)
	public boolean isAlarm() { return true; }

	/**
	 * Returns the name of the alarm. 
	 * @return the name, will not be null 
	 */
	@NotNull @Override @Contract(pure = true)
	public String getListableName() { return name; }

	/**
	 * Sets the name of the alarm. If an error is encountered, return code will be nonzero, based
	 * on which error is encountered. See Listable documentation for return codes.
	 * @param newName the new name of the alarm
	 * @return 0 (no error) or an error code specified in Listable documentation
	 */
	@Override
	public int setListableName(String newName) {
		if (newName == null || newName.equals("")) {
			if (DEBUG) Log.e(TAG, "New name is is null or empty.");
			return 1;
		}

		if (newName.indexOf('\t') != -1 || newName.indexOf('/') != -1) {
			if (DEBUG) Log.e(TAG, "New name has tabs in it.");
			return 2;
		}

		name = newName;
		return 0;
	}

	/**
	 * Gets a repeat string that describes the alarm. Uses the current context to get localized 
	 * strings. Repeat strings are based on the state of the alarm (whether it's snoozed) and the 
	 * repeat type. 
	 * @return the repeat string, or empty string if the context is null 
	 */
	@NotNull @Override
	public String getRepeatString() {
		if (context == null) {
			if (DEBUG) Log.e(TAG, "Context was null when trying to get a repeat string.");
			return "";
		}
		
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
				if (DEBUG) Log.e(TAG, "Unknown repeat type!");
				break;
		}
		return repeatString;
	}

	/**
	 * Returns whether it the alarm is active or not. Doesn't take into account parent folders.
	 * @return the active state of the alarm 
	 */
	@Override @Contract(pure = true)
	public boolean isActive() { return alarmIsActive; }

	/**
	 * Changes the active state of the alarm to on (active). 
	 */
	@Override
	public void turnOn() { alarmIsActive = true; }

	/**
	 * Changes the active state of the alarm to off (inactive). 
	 */
	@Override
	public void turnOff() { alarmIsActive = false; }

	/**
	 * Toggles the active state of the alarm (if it's on, turn it off; of it's off, turn it on).
	 */
	@Override
	public void toggleActive() { alarmIsActive = !alarmIsActive; }

	/**
	 * Sets the active state of the alarm.
	 * @param active the state to set the alarm to
	 */
	@Override
	public void setActive(boolean active) { alarmIsActive = active; }

	/**
	 * Gets the next time the alarm is going to ring as a string. 
	 * @return the ring time, will not be null 
	 */ 
	@NotNull @Override
	public String getNextRingTime() {
		// TODO: uppercase/lowercase as a setting? I like lowercase.
		return DateFormat.getTimeInstance(DateFormat.SHORT).format(ringTime.getTime()).toLowerCase();
	}

	/**
	 * Returns the size of the Listable. Alarms don't contain child listables, so this always
	 * returns 1.
	 */
	@Override @Contract(pure = true)
	public int size() { return 1; }

	/**
	 * Clones the Alarm and returns the new alarm.
	 * @return a new Alarm that is a deep copy of the first 
	 */
	@Nullable @Override @Contract(pure = true)
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
	 * Determines whether other is equal to this Alarm or not. Checks for name, repeat type (+ repeat
	 * type fields), ringtone uri, and whether the sound/vibrate/alarm itself is active or not.
	 * @param other the other object to compare to
	 * @return whether the two objects are equal or not
	 */
	@Contract("null -> false")
	public boolean equals(Object other) {
		if (!(other instanceof Alarm)) return false;

		Alarm that = (Alarm) other;
		if (!this.name.equals(that.name) || this.repeatType != that.repeatType ||
				!this.ringTime.equals(that.ringTime)) return false;

		switch (this.repeatType) {
			case REPEAT_ONCE_ABS:
			case REPEAT_DATE_YEARLY:
				break;
			case REPEAT_ONCE_REL:
			case REPEAT_OFFSET:
				// check offsets
				if (this.offsetDays != that.offsetDays || this.offsetHours != that.offsetHours ||
						this.offsetMins != that.offsetMins) return false;
				break;
			case REPEAT_DAY_WEEKLY:
				// check repeatDays
				for (int i = 0; i < repeatDays.length; i++)
					if (this.repeatDays[i] != that.repeatDays[i]) return false;
				break;
			case REPEAT_DAY_MONTHLY:
				// check repeatMonths, repeatWeek
				if (this.repeatWeek != that.repeatWeek) return false;
				// continue to check repeatMonths
			case REPEAT_DATE_MONTHLY:
				// check repeatMonths
				for (int i = 0; i < repeatMonths.length; i++)
					if (this.repeatMonths[i] != that.repeatMonths[i]) return false;
				break;
		}

		if (this.ringtoneUri == null) {
			if (that.ringtoneUri != null) return false;
		}
		else if (!this.ringtoneUri.equals(that.ringtoneUri)) return false;

		return this.alarmIsActive == that.alarmIsActive && this.alarmSoundIsOn == that.alarmSoundIsOn &&
				this.alarmVibrateIsOn == that.alarmVibrateIsOn;
	}

	/**
	 * Creates an edit string from the current alarm. Often used to pass an Alarm between Activities
	 * or threads.
	 * <br/>
	 * Current edit string format (separated by tabs):
	 * [alarm title]	[active]	[repeat info]	[next ring time]	[ringtone uri]	[is snoozed]
	 * [number of snoozes]
	 * <br/>
	 * Repeat type info format (separated by spaces): [type] [type-specific data]
	 * <br/>
	 * Type-specific data:
	 * ONCE_ABS and DATE_YEARLY: none
	 * ONCE_REL and OFFSET: [days] [hours] [mins]
	 * DAY_WEEKLY: [true/false for every day]
	 * DAY_MONTHLY: [week to repeat] [true/false for every month]
	 * DATE_MONTHLY: [true/false for every month]
	 * <br/>
	 * Note: for ringtone URI, if it is null (silent), it will be stored as "null"
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
				if (DEBUG) Log.e(TAG, "Invalid alarm repeat type.");
				return "";
		}

		alarmString.append('\t').append(ringTime.getTimeInMillis());
		alarmString.append('\t');
		if (ringtoneUri == null)
			alarmString.append("null");
		else
			alarmString.append(ringtoneUri.toString());

		alarmString.append('\t').append(alarmSnoozed);
		alarmString.append('\t').append(numSnoozes);
		// TODO: encode the other parts in here (don't forget to add a tab char before it)

		return alarmString.toString();
	}

	/**
	 * Creates a store string from the current alarm. Often used to store the Alarm on disk.
	 * <br/>
	 * Current store string format (separated by tabs):
	 * a	[edit string]
	 */
	@NotNull @Override
	public String toStoreString() { return "a\t" + toEditString(); }

	/* ******************************  Getter and Setter Methods  ******************************* */

	/**
	 * Returns the context stored within the Alarm.
	 */
	@Nullable @Contract(pure = true)
	public Context getContext() { return context; }

	/**
	 * Sets the context.
	 * @param context the new context to set it to, can be null
	 */
	public void setContext(@Nullable Context context) { this.context = context; }

	/**
	 * Returns the ring time of the alarm.
	 */
	@NotNull @Contract(pure = true)
	Calendar getAlarmTimeCalendar() { return ringTime; }

	/**
	 * Returns the next ring time of the alarm in a long.
	 */
	long getAlarmTimeMillis() { return ringTime.getTimeInMillis(); }

	/**
	 * Sets the next ring time of the alarm.
	 * @param time the new time to set the alarm to
	 */
	void setAlarmTimeMillis(long time) {
		if (time < 0) {
			if (DEBUG) Log.e(TAG, "New calendar time was negative.");
			return;
		}
		ringTime.setTimeInMillis(time);
	}

	/**
	 * Gets the repeat type of the alarm.
	 * @return the repeat type, which is always a valid repeat type
	 */
	@Contract(pure = true)
	int getRepeatType() { return repeatType; }

	/**
	 * Sets the repeat type of the alarm. If the repeat type is invalid, does nothing.
	 * @param type the new type to set the alarm to
	 */
	void setRepeatType(int type) {
		if (type < 0 || type >= NUM_REPEAT_TYPES) {
			if (DEBUG) Log.e(TAG, "Repeat type is invalid.");
			return;
		}
		repeatType = type;
		updateRingTime();
	}

	/**
	 * Gets the repeat days of the alarm, even if the repeat type doesn't use it.
	 * @return an array of size 7, whose indices correspond to Calendar day constants - 1
	 */
	@NotNull @Contract(pure = true)
	boolean[] getRepeatDays() { return repeatDays; }

	/**
	 * Gets whether the alarm repeats on the day given.
	 * @param index the day to check repeat data for, corresponding to the Calendar day constant - 1
	 * @return whether the alarm repeats on that day or not, or false if the index is out of bounds
	 */
	boolean getRepeatDays(int index) {
		if (index < 0 || index >= 7) {
			if (DEBUG) Log.e(TAG, "Index given to setRepeatDays is invalid.");
			return false;
		}
		return repeatDays[index];
	}

	/**
	 * Sets whether the alarm should repeat on index or not. If the index is invalid (not between 0
	 * and 6, inclusive), will not do anything.
	 * @param index the day to set repeat data for, corresponding to the Calendar constant - 1
	 * @param active the active state of the specified day
	 */
	void setRepeatDays(int index, boolean active) {
		if (index < 0 || index >= 7) {
			if (DEBUG) Log.e(TAG, "Index given to setRepeatDays is invalid.");
			return;
		}
		repeatDays[index] = active;
		updateRingTime();
	}

	/**
	 * Returns the repeat months of the alarm, even if the current repeat type doesn't use it.
	 * @return an array of size 12, whose indices correspond to the Calendar month constants
	 */
	@NotNull @Contract(pure = true)
	boolean[] getRepeatMonths() { return repeatMonths; }

	/**
	 * Gets whether the alarm is going to ring on the specified month.
	 * @param index the month to check, equal to the corresponding Calendar month constant
	 * @return whether the alarm is set to ring on that month or not, or false if the index is out
	 * of bounds
	 */
	boolean getRepeatMonths(int index) {
		if (index < 0 || index >= 12) {
			if (DEBUG) Log.e(TAG, "Index given to setRepeatMonths is invalid.");
			return false;
		}
		return repeatMonths[index];
	}

	/**
	 * Sets whether the alarm will ring on the specified month. If the index is invalid, will not
	 * do anything.
	 * @param index the month to set, equal to the corresponding Calendar month constant
	 * @param active the active state to set the alarm to for that month
	 */
	void setRepeatMonths(int index, boolean active) {
		if (index < 0 || index >= 12) {
			if (DEBUG) Log.e(TAG, "Index given to setRepeatMonths is invalid.");
			return;
		}
		repeatMonths[index] = active;
		updateRingTime();
	}

	/**
	 * Gets the repeat week of the alarm.
	 * @return the repeat week, an index of the string array alarm_week_strings
	 */
	@Contract(pure = true)
	int getRepeatWeek() { return repeatWeek; }

	/**
	 * Sets the repeat week of the alarm. If the new week is invalid, will not do anything.
	 * @param newWeek the new repeat week, an index of the string array alarm_week_strings
	 */
	void setRepeatWeek(int newWeek) {
		if (newWeek < 0 || newWeek >= 5) {
			if (DEBUG) Log.e(TAG, "New week to repeat on is invalid.");
			return;
		}
		repeatWeek = newWeek;
		updateRingTime();
	}

	/**
	 * Gets the number of days to offset the alarm with.
	 * @return a number greater than or equal to 0
	 */
	@Contract(pure = true)
	int getOffsetDays() { return offsetDays; }

	/**
	 * Sets the number of days to offset by. If invalid (under 0), will not do anything.
	 * @param days the number of days to offset by
	 */
	void setOffsetDays(int days) {
		if (days < 0) {
			if (DEBUG) Log.e(TAG, "New number of days is invalid.");
			return;
		}
		offsetDays = days;
		updateRingTime();
	}

	/**
	 * Gets the hours to offset the alarm with.
	 * @return a number between 0 and 23
	 */
	@Contract(pure = true)
	int getOffsetHours() { return offsetHours; }

	/**
	 * Sets the number of hours to offset the alarm with. If out of bounds (under 0 or over 23), will
	 * not do anything.
	 * @param hours the number of hours to offset, between 0 and 23 inclusive
	 */
	void setOffsetHours(int hours) {
		if (hours < 0 || hours >= 24) {
			if (DEBUG) Log.e(TAG, "New number of hours is invalid.");
			return;
		}
		offsetHours = hours;
		updateRingTime();
	}

	/**
	 * Gets the number of minutes to offset by.
	 * @return a number between 0 and 59 inclusive
	 */
	@Contract(pure = true)
	int getOffsetMins() { return offsetMins; }

	/**
	 * Sets the number of minutes to offset by
	 * @param min a number between 0 and 59 inclusive
	 */
	void setOffsetMins(int min) {
		if (min < 0 || min >= 60) {
			if (DEBUG) Log.e(TAG, "New number of minutes is invalid.");
			return;
		}
		offsetMins = min;
	}

	/**
	 * Gets whether the current alarm is snoozed or not.
	 */
	@Contract(pure = true)
	boolean getIsSnoozed() { return alarmSnoozed; }

	/**
	 * Gets whether the alarm has vibrate on or not.
	 */
	@Contract(pure = true)
	boolean isVibrateOn() { return alarmVibrateIsOn; }

	/**
	 * Sets whether the alarm has vibrate on or not.
	 * @param on the new state to set it to
	 */
	void setVibrateOn(boolean on) { alarmVibrateIsOn = on; }

	/**
	 * Gets whether the alarm has sound on or not.
	 */
	@Contract(pure = true)
	boolean isSoundOn() { return alarmSoundIsOn; }

	/**
	 * Sets whether the alarm has sound on or not.
	 * @param on the new state to set it to
	 */
	void setSoundOn(boolean on) { alarmSoundIsOn = on; }

	/**
	 * Returns the URI of the ringtone this alarm has. Can be null if the alarm is set to silent.
	 */
	@Nullable @Contract(pure = true)
	Uri getRingtoneUri() { return ringtoneUri; }

	/**
	 * Sets the new ringtone URI.
	 * @param newRingtone the new ringtone to set it to, can be null if the ringtone is silent
	 */
	void setRingtoneUri(@Nullable Uri newRingtone) {
		if (DEBUG && newRingtone == null) {
			Log.i(TAG, "The new ringtone is silent.");
		}
		ringtoneUri = newRingtone;
	}

	/**
	 * Gets the name of the ringtone.
	 * @return the name of the ringtone, or empty string if context is null
	 */
	@NotNull @Contract(pure = true)
	String getRingtoneName() {
		if (context == null) {
			if (DEBUG) Log.e(TAG, "Context is null, cannot query the name of the ringtone.");
			return "";
		}
		Ringtone r = RingtoneManager.getRingtone(context, ringtoneUri);
		if (r == null) {
			return context.getResources().getString(R.string.alarm_editor_silent);
		}
		return r.getTitle(context);
	}

	/* ************************************  Static Methods  ********************************** */

	/**
	 * Returns a new Alarm based on the given string. For edit string format, see toEditString().
	 *
	 * @param context current operating context, can be null
	 * @param src edit string to build an Alarm out of, can be null
	 * @return new Alarm based on the edit string, or null if the source string is null, empty, or
	 * formatted incorrectly
	 */
	@Nullable @Contract(pure = true)
	static Alarm fromEditString(@Nullable Context context, @Nullable String src) {
		if (src == null) {
			if (DEBUG) Log.e(TAG, "Edit string is null.");
			return null;
		} else if (src.length() == 0) {
			if (DEBUG) Log.e(TAG, "Edit string is empty.");
			return null;
		}

		String[] fields = src.split("\t");
		if (fields.length != 7) {
			if (DEBUG) Log.e(TAG, "Edit string didn't have a correct number of fields.");
			return null;
		}

		Alarm res = new Alarm(context, fields[0]);
		res.setActive(Boolean.parseBoolean(fields[1]));		// doesn't throw anything

		String[] repeatTypeInfo = fields[2].split(" ");
		try {
			res.setRepeatType(Integer.parseInt(repeatTypeInfo[0]));
		}
		catch (NumberFormatException e) {
			if (DEBUG) Log.e(TAG, "Edit string has an incorrectly formatted repeat type.");
			return null;
		}
		switch(res.repeatType) {
			case REPEAT_DAY_WEEKLY:
				if (repeatTypeInfo.length != 8) {
					if (DEBUG) Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				for (int i = 0; i < 7; i++) {
					res.repeatDays[i] = Boolean.parseBoolean(repeatTypeInfo[i + 1]);
				}
				break;
			case REPEAT_DAY_MONTHLY:
				if (repeatTypeInfo.length != 14) {
					if (DEBUG) Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				res.repeatWeek = Integer.parseInt(repeatTypeInfo[1]);
				for (int i = 0; i < 12; i++) {
					res.repeatMonths[i] = Boolean.parseBoolean(repeatTypeInfo[i + 2]);
				}
				break;
			case REPEAT_DATE_MONTHLY:
				if (repeatTypeInfo.length != 13) {
					if (DEBUG) Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				for (int i = 0; i < 12; i++) {
					res.repeatMonths[i] = Boolean.parseBoolean(repeatTypeInfo[i + 1]);
				}
				break;
			case REPEAT_ONCE_REL:
			case REPEAT_OFFSET:
				if (repeatTypeInfo.length != 4) {
					if (DEBUG) Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				try {
					res.offsetDays = Integer.parseInt(repeatTypeInfo[1]);
					res.offsetHours = Integer.parseInt(repeatTypeInfo[2]);
					res.offsetMins = Integer.parseInt(repeatTypeInfo[3]);
				}
				catch (NumberFormatException e) {
					if (DEBUG) Log.e(TAG, "Edit string has incorrectly formatted offsets.");
					return null;
				}
				break;
			case REPEAT_ONCE_ABS:
			case REPEAT_DATE_YEARLY:
				if (repeatTypeInfo.length != 1) {
					if (DEBUG) Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				break;
			default:
				if (DEBUG) Log.e(TAG, "Edit string had an unknown repeat type.");
				return null;
		}

		res.ringTime.setTimeInMillis(Long.parseLong(fields[3]));
		if ("null".equals(fields[4]))
			res.setRingtoneUri(null);
		else
			res.setRingtoneUri(Uri.parse(fields[4]));

		res.alarmSnoozed = Boolean.parseBoolean(fields[5]);

		try {
			res.numSnoozes = Integer.parseInt(fields[6]);
		}
		catch (NumberFormatException e) {
			if (DEBUG) Log.e(TAG, "Edit string has an incorrectly formatted number of snoozes.");
			return null;
		}

		return res;
	}

	/**
	 * Creates a new alarm from a stored Alarm string. Edit strings and store strings are pretty
	 * much the same for Alarms. For store string format, see toStoreString().
	 *
	 * @param currContext current operating context, can be null
	 * @param src store string to build an Alarm out of, can be null
	 * @return A new Alarm based on the store string, or null if the source string is null, empty, or
	 * formatted incorrectly
	 */
	@Nullable @Contract(pure = true)
	static Alarm fromStoreString(@Nullable Context currContext, @Nullable String src) {
		if (src == null) {
			if (DEBUG) Log.e(TAG, "Store string is null.");
			return null;
		} else if (src.length() == 0) {
			if (DEBUG) Log.e(TAG, "Store string is empty.");
			return null;
		}

		if (!src.startsWith("a\t")) {
			if (DEBUG) Log.e(TAG, "Store string has an unknown ID field.");
			return null;
		}
		return fromEditString(currContext, src.substring(2));		// removes the "a\t"
	}

	/* *********************************  Other Methods  ********************************** */

	/**
	 * Makes a new string that describes this alarm. Use edit or store strings if a full
	 * representation is necessary. Returns simply tne name of the alarm.
	 */
	@NotNull @Override @Contract(pure = true)
	public String toString() { return name; }

	@NotNull
	private String getWeeklyDisplayString() {
		if (context == null) {
			if (DEBUG) Log.e(TAG, "Context is null, cannot get the weekly display string.");
			return "";
		}

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
		if (context == null) {
			if (DEBUG) Log.e(TAG, "Context is null, cannot get the exception months display string.");
			return "";
		}

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
	@NotNull
	private String getOffsetString() {
		if (context == null) {
			if (DEBUG) Log.e(TAG, "Context is null, cannot get the offset display string.");
			return "";
		}

		Resources res = context.getResources();

		boolean previous = false;
		StringBuilder offsetString = new StringBuilder();

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
				if (DEBUG) Log.wtf(TAG, "Somehow the repeat type within the Alarm is wrong.");
				return;
		}
		ringTime.setTimeInMillis(workingClock.getTimeInMillis());
	}

	/**
	 * Snoozes the alarm for 5 minutes. Sets ringTime to five minutes away from original ringTime.
	 * TODO: take into account when the alarm was snoozed (don't just go off of ringTime)?
	 */
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
