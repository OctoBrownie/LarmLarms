package com.LarmLarms.data;

import android.content.Context;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;

import com.LarmLarms.BuildConfig;
import com.LarmLarms.R;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Class managing alarms and their behavior.
 * TODO: implement Parcelable so edit strings become unnecessary?
 */
public final class Alarm implements Listable, Cloneable {

	/* ************************************  Constants  *********************************** */

	/**
	 * Tag of the class for logging purposes.
	 */
	private final static String TAG = "Alarm";

	/**
	 * Represents an alarm that doesn't repeat (rings once) and is specified by an absolute date/time.
	 * Only uses field ringTime.
	 */
	public static final int REPEAT_ONCE_ABS = 0;
	/**
	 * Represents an alarm that doesn't repeat (rings once) and is specified by an offset from the
	 * current time (relative date/time). Uses field ringTime for storing date/time, and offset
	 * variables for storing the offset from creation/modification time.
	 */
	public static final int REPEAT_ONCE_REL = 1;
	/**
	 * Represents an alarm that repeats at a given time on specific days every week. Uses field
	 * ringTime to specify the time to ring and repeatDays for the days of the week to ring.
	 */
	public static final int REPEAT_DAY_WEEKLY = 2;
	/**
	 * Represents an alarm that repeats on a specific date (a number) every month. Uses field
	 * ringTime for the date (not month) and time of the alarm, repeatMonths for the months to ring
	 * on (can skip months).
	 */
	public static final int REPEAT_DATE_MONTHLY = 3;
	/**
	 * Represents an alarm that repeats on a specific day (day of a week) every month. Uses field
	 * ringTime for the time of the alarm and the day of the week, repeatMonths for the months to
	 * ring on (can skip months), and repeatWeek for the week to ring on (first week, second week,
	 * last week, etc).
	 */
	public static final int REPEAT_DAY_MONTHLY = 4;
	/**
	 * Represents an alarm that rings at a certain date/time every year. Only uses field ringTime.
	 */
	public static final int REPEAT_DATE_YEARLY = 5;
	/**
	 * Represents an alarm that repeats every offset from the previous time. Uses field ringTime to
	 * store date/time of the next ring time, and offset variables to store the offset to generate
	 * the next ring time (add them to ringTime).
	 */
	public static final int REPEAT_OFFSET = 6;

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
	 * The id of the Alarm, which is filled by the created time.
	 */
	private final int id;

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
	 * Used for REPEAT_ONCE_REL and REPEAT_OFFSET. Stores whether the offset should be off of the
	 * current time or some specified time. Doesn't actually affect any calculations in this class,
	 * more for display purposes.
	 */
	private boolean offsetFromNow;

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
	 * The volume to play the alarm at. Should be an integer between 0 and 100.
	 */
	private int volume;
	/**
	 * The URI of the ringtone of the alarm.
	 */
	@Nullable
	private Uri ringtoneUri;

	/* **********************************  Constructors  ********************************* */
	
	/**
	 * Creates a new alarm with the current context. Created Alarms should always have valid data,
	 * though they have lots of dummy data when first created.
	 * @param currContext the context this alarm exists in
	 */
	public Alarm(@Nullable Context currContext) {
		this(currContext, "default name",
				(int) (Calendar.getInstance().getTimeInMillis() % Integer.MAX_VALUE));
	}

	/**
	 * Creates a new alarm with the current context. Created Alarms should always have valid data,
	 * though they have lots of dummy data when first created.
	 * @param currContext the context this alarm exists in
	 * @param title the name of the alarm
	 */
	public Alarm(@Nullable Context currContext, @Nullable String title) {
		this(currContext, title, (int) (Calendar.getInstance().getTimeInMillis() % Integer.MAX_VALUE));
	}

	/**
	 * Creates a new alarm with the current context and the given title. Created Alarms should 
	 * always have valid data, though they have lots of dummy data when first created. 
	 * @param currContext the context this alarm exists in, can be null
	 * @param title the name of the alarm, shouldn't be null
	 */
	private Alarm(@Nullable Context currContext, @Nullable String title, int id) {
		context = currContext;
		this.id = id;
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
		offsetFromNow = true;

		alarmVibrateIsOn = true;
		alarmIsActive = true;

		alarmSnoozed = false;
		numSnoozes = 0;

		volume = 60;
		ringtoneUri = Settings.System.DEFAULT_RINGTONE_URI;
	}

	/* ********************************  Methods from Listable  ********************************** */

	/**
	 * Returns the ID of the alarm.
	 */
	@Override @Contract(pure = true)
	public int getId() { return id; }

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
			if (BuildConfig.DEBUG) Log.e(TAG, "New name is is null or empty.");
			return 1;
		}

		if (newName.indexOf('\t') != -1 || newName.indexOf('/') != -1) {
			if (BuildConfig.DEBUG) Log.e(TAG, "New name has tabs in it.");
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
			if (BuildConfig.DEBUG) Log.e(TAG, "Context was null when trying to get a repeat string.");
			return "";
		}
		
		Resources res = context.getResources();

		StringBuilder repeatString = new StringBuilder();
		String dateStr = DateFormat.getDateFormat(context).format(ringTime.getTime());
		String[] ordinals = res.getStringArray(R.array.alarm_ordinals);

		// snoozed or not
		if (alarmSnoozed) {
			// TODO: change number of minutes to snooze
			repeatString.append(String.format(res.getString(R.string.alarm_snooze), numSnoozes*5));
			repeatString.append('\n');
		}

		String months, exceptMonths;		// used for DATE_MONTHLY and DAY_MONTHLY
		switch (repeatType) {
			case REPEAT_ONCE_ABS:
			case REPEAT_ONCE_REL:
				repeatString.append(String.format(res.getString(R.string.alarm_once_abs_rel), dateStr));
				break;
			case REPEAT_DAY_WEEKLY:
				repeatString.append(getWeeklyDisplayString());
				break;
			case REPEAT_DATE_MONTHLY:
				int dateOfMonth = ringTime.get(Calendar.DATE);
				months = getMonthsString();
				exceptMonths = getExceptionMonthsString();
				if (months.length() <= exceptMonths.length()) {
					// use months
					repeatString.append(String.format(res.getString(R.string.alarm_date_monthly),
							months, ordinals[dateOfMonth - 1]));
				}
				else {
					// use exception months
					repeatString.append(String.format(res.getString(R.string.alarm_date_monthly_except),
							ordinals[dateOfMonth - 1], exceptMonths));
				}
				break;
			case REPEAT_DAY_MONTHLY:
				String[] weekdays = (new DateFormatSymbols()).getWeekdays();

				months = getMonthsString();
				exceptMonths = getExceptionMonthsString();
				if (months.length() <= exceptMonths.length()) {
					// use months
					repeatString.append(String.format(res.getString(R.string.alarm_day_monthly),
							res.getStringArray(R.array.alarm_week_strings)[repeatWeek],
							weekdays[ringTime.get(Calendar.DAY_OF_WEEK)], months));
				}
				else {
					// use exception months
					repeatString.append(String.format(res.getString(R.string.alarm_day_monthly_except),
							res.getStringArray(R.array.alarm_week_strings)[repeatWeek],
							weekdays[ringTime.get(Calendar.DAY_OF_WEEK)], exceptMonths));
				}
				break;
			case REPEAT_DATE_YEARLY:
				// TODO: don't show the year
				repeatString.append(String.format(res.getString(R.string.alarm_date_yearly), dateStr));
				break;
			case REPEAT_OFFSET:
				repeatString.append(String.format(res.getString(R.string.alarm_offset),
						getOffsetString(), dateStr));
				break;
			default:
				if (BuildConfig.DEBUG) Log.e(TAG, "Unknown repeat type!");
				return "";
		}
		return repeatString.toString();
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
	 * Changes the active state of the alarm to off (inactive). Will also unsnooze it if necessary.
	 */
	@Override
	public void turnOff() {
		alarmIsActive = false;
		unsnooze();
	}

	/**
	 * Toggles the active state of the alarm (if it's on, turn it off; of it's off, turn it on). Will
	 * also unsnooze it if necessary.
	 */
	@Override
	public void toggleActive() {
		alarmIsActive = !alarmIsActive;
		unsnooze();
	}

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
		if (context == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Context was null when trying to get the next ring time.");
			return "";
		}
		return DateFormat.getTimeFormat(context).format(ringTime.getTime()).toLowerCase();
	}

	/**
	 * Returns the size of the Listable. Alarms don't contain child listables and never collapses,
	 * so this always returns 1.
	 */
	@Override @Contract(pure = true)
	public int visibleSize() { return 1; }

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
			that.repeatDays = this.repeatDays.clone();
			that.repeatMonths = this.repeatMonths.clone();
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
				this.getUnsnoozedAlarmTimeMillis() != that.getUnsnoozedAlarmTimeMillis())
			return false;

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

		return this.volume == that.volume && this.alarmIsActive == that.alarmIsActive &&
				this.alarmVibrateIsOn == that.alarmVibrateIsOn;
	}
	
	/**
	 * Compares this alarm with the other object. Folders are always considered "before" alarms.
	 * Alarms are compared with this precedence: name, type (uses the values of the constants, 
	 * lower is first), time fields (depends on the type, see below), then id. It shouldn't be 
	 * necessary to compare anything further since their ids should always be unique. Doesn't quite
	 * correlate with the equals method, since they check different things (for example, equals
	 * doesn't check for id). 
	 * 
	 * For time fields based on alarm type:
	 * ONCE_ABS - checks ring date/time
	 * ONCE_REL and OFFSET - checks offsets (higher frequencies are first), offsetFromNow (alarms 
	 * offset from now are first), and ring date/time 
	 * DAY_WEEKLY - checks repeatDays (higher number of repeating days first, but if equal it's 
	 * whichever rings more earliest in the week), and ring time
	 * DATE_MONTHLY - checks repeatMonths (higher number of repeating months first, but if equal 
	 * it's whichever rings more earliest in the year), and ring date/time (controls for month/year)
	 * DAY_MONTHLY - checks repeatMonths (higher number of repeating months first, but if equal 
	 * it's whichever rings more earliest in the year), day of the month (week then day), and ring 
	 * time 
	 * DATE_YEARLY - checks ring date/time (controls for year)
	 * 
	 * @param other the object to compare with
	 * @return this alarm compared to the listable (negative if this is first, positive if this is 
	 * second, 0 if they're equal).
	 */
	@Override
	public int compareTo(@NotNull Object other) {
		if (other instanceof AlarmGroup) return 1;
		
		Alarm that = (Alarm) other;
		int temp = this.name.compareTo(that.name);
		if (temp != 0) return temp;
		
		temp = this.repeatType - that.repeatType;
		if (temp != 0) return temp;
		
		// for certain repeat types requiring manipulating ringTime
		Calendar thisTime, thatTime, sysClock = Calendar.getInstance(); 
		switch(this.repeatType) {
		case REPEAT_ONCE_ABS:
			temp = this.ringTime.compareTo(that.ringTime);
			if (temp != 0) return temp;
			break;
		case REPEAT_ONCE_REL:
		case REPEAT_OFFSET:
			temp = this.offsetDays - that.offsetDays;
			if (temp != 0) return temp;
			
			temp = this.offsetHours - that.offsetHours;
			if (temp != 0) return temp;
			
			temp = this.offsetMins - that.offsetMins;
			if (temp != 0) return temp;
			
			if (this.offsetFromNow ^ that.offsetFromNow) 
				return this.offsetFromNow ? -1 : 1;
			
			temp = this.ringTime.compareTo(that.ringTime);
			if (temp != 0) return temp;
			break;
		case REPEAT_DAY_WEEKLY: {
			boolean thisFirst = true, set = false;
			
			for (int i = 0; i < 7; i++) {
				if (this.repeatDays[i]) temp--;
				if (that.repeatDays[i]) temp++;
				if (!set && temp != 0) {
					thisFirst = temp <= 0;
					set = true;
				}
			}
			if (temp != 0) return temp;
			if (set) return thisFirst ? -1 : 1;
			
			temp = this.ringTime.get(Calendar.HOUR_OF_DAY) - 
					that.ringTime.get(Calendar.HOUR_OF_DAY);
			if (temp != 0) return temp;
			
			temp = this.ringTime.get(Calendar.MINUTE) - 
					that.ringTime.get(Calendar.MINUTE);
			if (temp != 0) return temp;
			break;
		}
		case REPEAT_DATE_MONTHLY: {
			boolean thisFirst = true, set = false;
			
			for (int i = 0; i < 12; i++) {
				if (this.repeatMonths[i]) temp--;
				if (that.repeatMonths[i]) temp++;
				if (!set && temp != 0) {
					thisFirst = temp <= 0;
					set = true;
				}
			}
			if (temp != 0) return temp;
			if (set) return thisFirst ? -1 : 1;
			
			thisTime = (Calendar) this.ringTime.clone();
			thatTime = (Calendar) that.ringTime.clone();
			thisTime.set(Calendar.MONTH, sysClock.get(Calendar.MONTH));
			thisTime.set(Calendar.YEAR, sysClock.get(Calendar.YEAR));
			thatTime.set(Calendar.MONTH, sysClock.get(Calendar.MONTH));
			thatTime.set(Calendar.YEAR, sysClock.get(Calendar.YEAR));
			
			temp = thisTime.compareTo(thatTime);
			if (temp != 0) return temp;
			break;
		}
		case REPEAT_DAY_MONTHLY: {
			boolean thisFirst = true, set = false;
			
			for (int i = 0; i < 12; i++) {
				if (this.repeatMonths[i]) temp--;
				if (that.repeatMonths[i]) temp++;
				if (!set && temp != 0) {
					thisFirst = temp <= 0;
					set = true;
				}
			}
			if (temp != 0) return temp;
			if (set) return thisFirst ? -1 : 1;
			
			temp = this.ringTime.get(Calendar.DAY_OF_WEEK_IN_MONTH) - 
					that.ringTime.get(Calendar.DAY_OF_WEEK_IN_MONTH);
			if (temp != 0) return temp;
			
			temp = this.ringTime.get(Calendar.DAY_OF_WEEK) - 
					that.ringTime.get(Calendar.DAY_OF_WEEK);
			if (temp != 0) return temp;
			
			temp = this.ringTime.get(Calendar.HOUR_OF_DAY) - 
					that.ringTime.get(Calendar.HOUR_OF_DAY);
			if (temp != 0) return temp;
			
			temp = this.ringTime.get(Calendar.MINUTE) - 
					that.ringTime.get(Calendar.MINUTE);
			if (temp != 0) return temp;
			break;
		}
		case REPEAT_DATE_YEARLY:
			thisTime = (Calendar) this.ringTime.clone();
			thatTime = (Calendar) that.ringTime.clone();
			thisTime.set(Calendar.YEAR, sysClock.get(Calendar.YEAR));
			thatTime.set(Calendar.YEAR, sysClock.get(Calendar.YEAR));
			
			temp = thisTime.compareTo(thatTime);
			if (temp != 0) return temp;
			break;
		}
		
		return this.id - that.id;
	}

	/**
	 * Creates an edit string from the current alarm. Often used to pass an Alarm between Activities
	 * or threads. To get an Alarm back from an edit string, use fromEditString(String).
	 * <br/>
	 * Current edit string format (separated by TABS):
	 * [id] [alarm title] [active] [repeat info] [next ring time] [ringtone uri] [is snoozed]
	 * [# of snoozes] [volume]
	 * <br/>
	 * Repeat type format (separated by SPACES): [type] [type-specific data]
	 * <br/>
	 * Type-specific data:
	 * ONCE_ABS and DATE_YEARLY: none
	 * ONCE_REL and OFFSET: [days] [hours] [mins] [is offset from time]
	 * DAY_WEEKLY: [true/false for every day]
	 * DAY_MONTHLY: [week to repeat] [true/false for every month]
	 * DATE_MONTHLY: [true/false for every month]
	 * <br/>
	 * Note: for ringtone URI, if it is null (silent), it will be stored as "null"
	 */
	@NotNull @Override @Contract(pure = true)
	public String toEditString() {
		StringBuilder alarmString = new StringBuilder();
		alarmString.append(id).append('\t');
		alarmString.append(name).append('\t');
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
				alarmString.append(' ').append(offsetFromNow);
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
				alarmString.append(' ').append(isOffsetFromNow());
				break;
			default:
				if (BuildConfig.DEBUG) Log.e(TAG, "Invalid alarm repeat type.");
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
		alarmString.append('\t').append(volume);

		return alarmString.toString();
	}

	/**
	 * Creates a store string from the current alarm. Often used to store the Alarm on disk.
	 * <br/>
	 * Current store string format (separated by tabs):
	 * a	[edit string]
	 */
	@NotNull @Override @Contract(pure = true)
	public String toStoreString() { return "a\t" + toEditString(); }

	/* ******************************  Getter and Setter Methods  ******************************* */

	/**
	 * Sets the context.
	 * @param context the new context to set it to, can be null
	 */
	public void setContext(@Nullable Context context) { this.context = context; }

	/**
	 * Returns the ring time of the alarm.
	 */
	@NotNull @Contract(pure = true)
	public Calendar getAlarmTimeCalendar() { return ringTime; }

	/**
	 * Returns the next ring time of the alarm in a long.
	 */
	long getAlarmTimeMillis() { return ringTime.getTimeInMillis(); }

	/**
	 * Returns the next ring time of the alarm in a long and deletes any snooze periods that affect
	 * the time.
	 */
	@Contract(pure = true)
	private long getUnsnoozedAlarmTimeMillis() {
		Calendar cal = (Calendar) ringTime.clone();
		if (alarmSnoozed) cal.add(Calendar.MINUTE, -5*numSnoozes);

		return cal.getTimeInMillis();
	}

	/**
	 * Sets the next ring time of the alarm.
	 * @param time the new time to set the alarm to
	 */
	public void setAlarmTimeMillis(long time) {
		if (time < 0) {
			if (BuildConfig.DEBUG) Log.e(TAG, "New calendar time was negative.");
			return;
		}
		ringTime.setTimeInMillis(time);
	}

	/**
	 * Gets the repeat type of the alarm.
	 * @return the repeat type, which is always a valid repeat type
	 */
	@Contract(pure = true)
	public int getRepeatType() { return repeatType; }

	/**
	 * Sets the repeat type of the alarm. If the repeat type is invalid, does nothing.
	 * @param type the new type to set the alarm to
	 */
	public void setRepeatType(int type) {
		if (type < 0 || type >= NUM_REPEAT_TYPES) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Repeat type is invalid.");
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
	public boolean[] getRepeatDays() { return repeatDays; }

	/**
	 * Returns the repeat months of the alarm, even if the current repeat type doesn't use it.
	 * @return an array of size 12, whose indices correspond to the Calendar month constants
	 */
	@NotNull @Contract(pure = true)
	public boolean[] getRepeatMonths() { return repeatMonths; }

	/**
	 * Gets the repeat week of the alarm.
	 * @return the repeat week, an index of the string array alarm_week_strings
	 */
	@Contract(pure = true)
	public int getRepeatWeek() { return repeatWeek; }

	/**
	 * Sets the repeat week of the alarm. If the new week is invalid, will not do anything.
	 * @param newWeek the new repeat week, an index of the string array alarm_week_strings
	 */
	public void setRepeatWeek(int newWeek) {
		if (newWeek < 0 || newWeek >= 5) {
			if (BuildConfig.DEBUG) Log.e(TAG, "New week to repeat on is invalid.");
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
	public int getOffsetDays() { return offsetDays; }

	/**
	 * Sets the number of days to offset by. If invalid (under 0), will not do anything.
	 * @param days the number of days to offset by
	 */
	public void setOffsetDays(int days) {
		if (days < 0) {
			if (BuildConfig.DEBUG) Log.e(TAG, "New number of days is invalid.");
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
	public int getOffsetHours() { return offsetHours; }

	/**
	 * Sets the number of hours to offset the alarm with. If out of bounds (under 0 or over 23), will
	 * not do anything.
	 * @param hours the number of hours to offset, between 0 and 23 inclusive
	 */
	public void setOffsetHours(int hours) {
		if (hours < 0 || hours >= 24) {
			if (BuildConfig.DEBUG) Log.e(TAG, "New number of hours is invalid.");
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
	public int getOffsetMins() { return offsetMins; }

	/**
	 * Sets the number of minutes to offset by
	 * @param min a number between 0 and 59 inclusive
	 */
	public void setOffsetMins(int min) {
		if (min < 0 || min >= 60) {
			if (BuildConfig.DEBUG) Log.e(TAG, "New number of minutes is invalid.");
			return;
		}
		offsetMins = min;
	}

	/**
	 * Gets whether the offset is from the current time or not.
	 */
	@Contract(pure = true)
	public boolean isOffsetFromNow() { return offsetFromNow; }

	/**
	 * Sets whether the offset is from the current time or not.
	 */
	public void setOffsetFromNow(boolean offsetFromNow) { this.offsetFromNow = offsetFromNow; }

	/**
	 * Gets whether the current alarm is snoozed or not.
	 */
	@Contract(pure = true)
	boolean getIsSnoozed() { return alarmSnoozed; }

	/**
	 * Gets whether the alarm has vibrate on or not.
	 */
	@Contract(pure = true)
	public boolean isVibrateOn() { return alarmVibrateIsOn; }

	/**
	 * Sets whether the alarm has vibrate on or not.
	 * @param on the new state to set it to
	 */
	public void setVibrateOn(boolean on) { alarmVibrateIsOn = on; }

	/**
	 * Gets the volume of the alarm.
	 */
	@Contract(pure = true)
	public int getVolume() { return volume; }

	/**
	 * Sets the volume of the alarm.
	 * @param vol the new volume, should be between 0 and 100
	 */
	public void setVolume(int vol) {
		if (vol < 0) vol = 0;
		if (vol > 100) vol = 100;

		volume = vol;
	}

	/**
	 * Returns the URI of the ringtone this alarm has. Can be null if the alarm is set to silent.
	 */
	@Nullable @Contract(pure = true)
	public Uri getRingtoneUri() { return ringtoneUri; }

	/**
	 * Sets the new ringtone URI.
	 * @param newRingtone the new ringtone to set it to, can be null if the ringtone is silent
	 */
	public void setRingtoneUri(@Nullable Uri newRingtone) {
		if (BuildConfig.DEBUG && newRingtone == null) {
			Log.i(TAG, "The new ringtone is silent.");
		}
		ringtoneUri = newRingtone;
	}

	/**
	 * Gets the name of the ringtone.
	 * @return the name of the ringtone, or empty string if context is null
	 */
	@NotNull @Contract(pure = true)
	public String getRingtoneName() {
		if (context == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Context is null, cannot query the name of the ringtone.");
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
	 * Returns a new Alarm based on the given string. For edit string creation and format, see
	 * toEditString().
	 *
	 * @param context current operating context, can be null
	 * @param src edit string to build an Alarm out of, can be null
	 * @return new Alarm based on the edit string, or null if the source string is null, empty, or
	 * formatted incorrectly
	 */
	@Nullable @Contract(pure = true)
	public static Alarm fromEditString(@Nullable Context context, @Nullable String src) {
		if (src == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Edit string is null.");
			return null;
		} else if (src.length() == 0) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Edit string is empty.");
			return null;
		}

		String[] fields = src.split("\t");
		if (fields.length != 8 && fields.length != 9) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Edit string didn't have a correct number of fields.");
			return null;
		}

		Alarm res;

		try {
			res = new Alarm(context, fields[1], Integer.parseInt(fields[0]));
		}
		catch (NumberFormatException e) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Edit string has an invalid id.");
			return null;
		}

		res.setActive(Boolean.parseBoolean(fields[2]));		// doesn't throw anything

		String[] repeatTypeInfo = fields[3].split(" ");
		try {
			res.setRepeatType(Integer.parseInt(repeatTypeInfo[0]));
		}
		catch (NumberFormatException e) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Edit string has an incorrectly formatted repeat type.");
			return null;
		}
		switch(res.repeatType) {
			case REPEAT_DAY_WEEKLY:
				if (repeatTypeInfo.length != 8) {
					if (BuildConfig.DEBUG) Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				for (int i = 0; i < 7; i++) {
					res.repeatDays[i] = Boolean.parseBoolean(repeatTypeInfo[i + 1]);
				}
				break;
			case REPEAT_DAY_MONTHLY:
				if (repeatTypeInfo.length != 14) {
					if (BuildConfig.DEBUG) Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				res.repeatWeek = Integer.parseInt(repeatTypeInfo[1]);
				for (int i = 0; i < 12; i++) {
					res.repeatMonths[i] = Boolean.parseBoolean(repeatTypeInfo[i + 2]);
				}
				break;
			case REPEAT_DATE_MONTHLY:
				if (repeatTypeInfo.length != 13) {
					if (BuildConfig.DEBUG) Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				for (int i = 0; i < 12; i++) {
					res.repeatMonths[i] = Boolean.parseBoolean(repeatTypeInfo[i + 1]);
				}
				break;
			case REPEAT_ONCE_REL:
			case REPEAT_OFFSET:
				if (repeatTypeInfo.length != 5) {
					if (BuildConfig.DEBUG) Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				try {
					res.offsetDays = Integer.parseInt(repeatTypeInfo[1]);
					res.offsetHours = Integer.parseInt(repeatTypeInfo[2]);
					res.offsetMins = Integer.parseInt(repeatTypeInfo[3]);
				}
				catch (NumberFormatException e) {
					if (BuildConfig.DEBUG) Log.e(TAG, "Edit string has incorrectly formatted offsets.");
					return null;
				}

				res.offsetFromNow = Boolean.parseBoolean(repeatTypeInfo[4]);
				break;
			case REPEAT_ONCE_ABS:
			case REPEAT_DATE_YEARLY:
				if (repeatTypeInfo.length != 1) {
					if (BuildConfig.DEBUG) Log.e(TAG, "Edit string had the wrong number of repeat type fields.");
					return null;
				}
				break;
			default:
				if (BuildConfig.DEBUG) Log.e(TAG, "Edit string had an unknown repeat type.");
				return null;
		}

		res.ringTime.setTimeInMillis(Long.parseLong(fields[4]));

		if ("null".equals(fields[5])) res.setRingtoneUri(null);
		else res.setRingtoneUri(Uri.parse(fields[5]));

		res.alarmSnoozed = Boolean.parseBoolean(fields[6]);

		try {
			res.numSnoozes = Integer.parseInt(fields[7]);
		}
		catch (NumberFormatException e) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Edit string has an invalid number of snoozes.");
			return null;
		}

		try {
			res.setVolume(Integer.parseInt(fields[8]));
		}
		catch (NumberFormatException e) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Edit string has an invalid volume.");
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
			if (BuildConfig.DEBUG) Log.e(TAG, "Store string is null.");
			return null;
		} else if (src.length() == 0) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Store string is empty.");
			return null;
		}

		if (!src.startsWith("a\t")) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Store string has an unknown ID field.");
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

	/**
	 * Gets the display string for REPEAT_DAY_WEEKLY, specifically representing which days the 
	 * alarm repeats on. Will return "Weekly on [days]" unless it's a special set of days (every 
	 * day, weekends, weekdays, or none).
	 * @return formatted string resource alarm_weekly, special cases, or empty
	 */
	@NotNull @Contract(pure = true)
	public String getWeeklyDisplayString() {
		if (context == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Context is null, cannot get the weekly display string.");
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
	 * Returns the months that the alarm is going to ring on.
	 * @return formatted string resource alarm_monthly, special cases, or empty
	 */
	@NotNull @Contract(pure = true)
	public String getMonthsString() {
		if (context == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Context is null, cannot get the months display string.");
			return "";
		}

		Resources res = context.getResources();

		StringBuilder months = new StringBuilder();
		String separator = res.getString(R.string.separator);
		String[] monthStrings = (new DateFormatSymbols()).getShortMonths();
		List<String> monthsToAdd = new ArrayList<>();
		int numMonths = 0;
		boolean evenFlag = true, oddFlag = true;

		for (int i = 0; i < repeatMonths.length; i++) {
			// have to flip the equals because repeatMonths[0] represents January (1)
			evenFlag = evenFlag && (i % 2 != 0) == repeatMonths[i];
			oddFlag = oddFlag && (i % 2 == 0) == repeatMonths[i];

			if (repeatMonths[i]) {
				monthsToAdd.add(monthStrings[i]);
				numMonths++;
			}
		}

		if (numMonths == 0) { return res.getString(R.string.alarm_no_months); }
		if (numMonths == 12) { return res.getString(R.string.alarm_all_months); }
		if (numMonths == 6 && evenFlag) { return res.getString(R.string.alarm_even_months); }
		if (numMonths == 6 && oddFlag) { return res.getString(R.string.alarm_odd_months); }

		months.append(monthsToAdd.get(0));
		if (numMonths > 1) {
			// use normal separator
			for (int i = 1; i < numMonths - 1; i++) {
				months.append(separator);
				months.append(monthsToAdd.get(i));
			}

			months.append(res.getString(R.string.final_separator));
			months.append(monthsToAdd.get(monthsToAdd.size() - 1));
		}

		return String.format(res.getString(R.string.alarm_monthly), months.toString());
	}

	/**
	 * Returns the exception months string, or empty string if no exceptions.
	 * @return formatted string resource alarm_monthly_exception or empty
	 */
	@NotNull @Contract(pure = true)
	private String getExceptionMonthsString() {
		if (context == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Context is null, cannot get the exception months display string.");
			return "";
		}

		Resources res = context.getResources();

		StringBuilder months = new StringBuilder();
		String separator = res.getString(R.string.separator);
		String[] monthStrings = (new DateFormatSymbols()).getShortMonths();
		List<String> monthsToAdd = new ArrayList<>();
		int numMonths = 0;

		for (int i = 0; i < repeatMonths.length; i++) {
			if (!repeatMonths[i]) {
				monthsToAdd.add(monthStrings[i]);
				numMonths++;
			}
		}

		if (numMonths == 0) { return ""; }

		if (numMonths == 1) {
			months.append(monthsToAdd.get(0));
		}
		else {
			months.append(monthStrings[0]);

			// use normal separator
			for (int i = 1; i < numMonths - 1; i++) {
				months.append(separator);
				months.append(monthsToAdd.get(i));
			}

			months.append(res.getString(R.string.final_separator));
			months.append(monthsToAdd.get(monthsToAdd.size() - 1));
		}

		return String.format(res.getString(R.string.alarm_monthly_exception), months.toString());
	}

	/**
	 * Builds a string for offsets. Returns a new string with days, hours, and minutes.
	 */
	@NotNull @Contract(pure = true)
	private String getOffsetString() {
		if (context == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Context is null, cannot get the offset display string.");
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
	 * is not active. If the alarm is ONCE_ABS and ringTime has passed, the method will reset it to
	 * ring on the earliest day with the same time (it will stay 13:00 if it was 13:00 originally).
	 * If the alarm is ONCE_REL, and ringTime has passed, will reset to offset after current time.
	 * Tries not to rely on the previous value of ringTime, but will take values of guaranteed stored
	 * constants (except OFFSET, which will use the entirety of the previous ring time).
	 *
	 * NOTE: if the date doesn't exist (ex. April 31 for DATE_MONTHLY), it will simply skip it (will
	 * not schedule an alarm for May 1)
	 */
	public void updateRingTime() {
		if (!alarmIsActive || alarmSnoozed) { return; }

		GregorianCalendar workingClock = new GregorianCalendar();
		final GregorianCalendar currTime = new GregorianCalendar();

		int currMonth = workingClock.get(Calendar.MONTH);
		final int thisMonth = currMonth;

		// whether or not workingClock has been set yet
		boolean clockSet = false;
		
		// whether the end of the current time interval (ex: last day of week) has been reached, so
		// we can wrap around to the next one
		boolean wrap = false;

		if (repeatType != REPEAT_ONCE_REL && repeatType != REPEAT_OFFSET) {
			// common to all the non-offset repeat types that use workingClock
			workingClock.set(Calendar.HOUR_OF_DAY, ringTime.get(Calendar.HOUR_OF_DAY));
			workingClock.set(Calendar.MINUTE, ringTime.get(Calendar.MINUTE));
			workingClock.set(Calendar.SECOND, 0);
			workingClock.set(Calendar.MILLISECOND, 0);
		}

		// use break to set workingClock to ringTime, return to not
		switch(repeatType) {
			case REPEAT_ONCE_ABS:
				// only changes if the alarm is overdue
				if (ringTime.after(workingClock)) { return; }

				while (workingClock.before(currTime)) {
					workingClock.add(Calendar.DAY_OF_MONTH, 1);
				}
				break;
			case REPEAT_ONCE_REL:
				// only changes if the alarm is overdue
				if (ringTime.after(workingClock)) { return; }

				workingClock.add(Calendar.DAY_OF_MONTH, offsetDays);
				workingClock.add(Calendar.HOUR_OF_DAY, offsetHours);
				workingClock.add(Calendar.MINUTE, offsetMins);
				offsetFromNow = true;
				break;
			case REPEAT_DAY_WEEKLY:
				// trying to avoid any namespace issues
				{
					// days of week follow Calendar constants
					int dayOfWeek = workingClock.get(Calendar.DAY_OF_WEEK);
					final int todayDayOfWeek = dayOfWeek;

					while (!clockSet || workingClock.before(currTime)) {
						if (wrap) workingClock.add(Calendar.WEEK_OF_MONTH, 1);
						if (repeatDays[dayOfWeek - 1]) {
							workingClock.set(Calendar.DAY_OF_WEEK, dayOfWeek);
							clockSet = true;
						}

						// go to the next day of the week
						dayOfWeek = dayOfWeek % 7 + 1;
						wrap = dayOfWeek == Calendar.SUNDAY;

						// we've wrapped all the way around
						if (dayOfWeek == todayDayOfWeek) {
							if (repeatDays[dayOfWeek - 1]) break;

							if (BuildConfig.DEBUG) Log.i(TAG, "There are no repeat days to set the next alarm to.");
							return;
						}
					}
				}
				break;
			case REPEAT_DATE_MONTHLY:
				// third condition is in case the day of the month (ex. 31st) doesn't exist
				while (!clockSet || workingClock.before(currTime) ||
						workingClock.get(Calendar.DAY_OF_MONTH) != ringTime.get(Calendar.DAY_OF_MONTH)) {
					if (wrap) workingClock.add(Calendar.YEAR, 1);
					if (repeatMonths[currMonth]) {
						workingClock.set(Calendar.MONTH, currMonth);
						workingClock.set(Calendar.DAY_OF_MONTH, ringTime.get(Calendar.DAY_OF_MONTH));
						clockSet = true;
					}

					// go to the next month
					currMonth = (currMonth + 1) % 12;
					wrap = currMonth == Calendar.JANUARY;

					// we've wrapped all the way around
					if (currMonth == thisMonth) {
						if (repeatMonths[currMonth]) {
							workingClock.set(Calendar.DAY_OF_MONTH, ringTime.get(Calendar.DAY_OF_MONTH));
							break;
						}
						if (BuildConfig.DEBUG) Log.i(TAG, "There are no repeat months to set the next alarm to.");
						return;
					}
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

				while (!clockSet || workingClock.before(currTime)) {
					if (wrap) workingClock.add(Calendar.YEAR, 1);
					if (repeatMonths[currMonth]) {
						workingClock.set(Calendar.MONTH, currMonth);
						clockSet = true;
					}

					// go to the next month
					currMonth = (currMonth + 1) % 12;
					wrap = currMonth == Calendar.JANUARY;

					// we've wrapped all the way around
					if (currMonth == thisMonth) {
						if (repeatMonths[currMonth]) {
							break;
						}
						if (BuildConfig.DEBUG) Log.i(TAG, "There are no repeat months to set the next alarm to.");
						return;
					}
				}
				break;
			case REPEAT_DATE_YEARLY:
				{
					int day = ringTime.get(Calendar.DAY_OF_MONTH);
					int month = ringTime.get(Calendar.MONTH);

					workingClock.set(Calendar.DAY_OF_MONTH, day);
					workingClock.set(Calendar.MONTH, month);

					while (workingClock.before(currTime) || workingClock.get(Calendar.DAY_OF_MONTH) != day) {
						workingClock.add(Calendar.YEAR, 1);
						workingClock.set(Calendar.DAY_OF_MONTH, day);
						workingClock.set(Calendar.MONTH, month);
					}
				}
				break;
			case REPEAT_OFFSET:
				while (ringTime.before(workingClock)) {
					ringTime.add(Calendar.DAY_OF_MONTH, offsetDays);
					ringTime.add(Calendar.HOUR_OF_DAY, offsetHours);
					ringTime.add(Calendar.MINUTE, offsetMins);
				}
				return;
			default:
				if (BuildConfig.DEBUG) Log.wtf(TAG, "Somehow the repeat type within the Alarm is wrong.");
				return;
		}
		ringTime.setTimeInMillis(workingClock.getTimeInMillis());
	}

	/**
	 * Snoozes the alarm for 5 minutes. Sets ringTime to five minutes away from original ringTime.
	 */
	public void snooze() {
		alarmSnoozed = true;
		numSnoozes++;
		// TODO: change number of minutes to snooze?
		// if so, also gotta change unsnooze() and getUnsnoozedAlarmTimeMillis()
		ringTime.add(Calendar.MINUTE, 5);
	}

	/**
	 * Unsnoozes the alarm if it was snoozed previously. Won't do anything if it wasn't snoozed.
	 */
	public void unsnooze() {
		alarmSnoozed = false;

		ringTime.add(Calendar.MINUTE, -5*numSnoozes);
		numSnoozes = 0;
	}

	/**
	 * Dismisses the current alarm.
	 */
	public void dismiss() {
		unsnooze();

		switch (repeatType) {
			case Alarm.REPEAT_ONCE_ABS:
			case Alarm.REPEAT_ONCE_REL:
				turnOff();
				break;
			case Alarm.REPEAT_DAY_WEEKLY:
			case Alarm.REPEAT_DATE_MONTHLY:
			case Alarm.REPEAT_DAY_MONTHLY:
			case Alarm.REPEAT_DATE_YEARLY:
			case Alarm.REPEAT_OFFSET:
				updateRingTime();
				break;
			default:
				if (BuildConfig.DEBUG) Log.wtf(TAG, "The alarm repeat type was invalid...?");
				break;
		}
	}
}
