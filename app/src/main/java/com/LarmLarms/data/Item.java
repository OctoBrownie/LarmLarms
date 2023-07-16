package com.larmlarms.data;

import android.util.Log;

import com.larmlarms.BuildConfig;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;

/**
 * Class allowing the RecyclerViewAdapter to access specific parts of either Alarms or AlarmGroups.
 * Using isAlarm(), it can also discriminate between Alarms and AlarmGroups. This is meant mostly
 * to expose the data necessary for displaying alarms on the main page.
 */
public abstract class Item implements Comparable<Item> {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "Item";

	/* ************************************  Instance Fields  *********************************** */
	/**
	 * The ID of the folder, filled by its created time.
	 */
	protected final int id;

	/**
	 * Stores the name of the folder. Restricted characters: tabs and backslashes. If the user tries
	 * to set them as part of the name, they will be automatically stripped out.
	 */
	@NotNull
	protected String name;

	/**
	 * Stores the active state of the item. Represents whether the alarms within it will ring or
	 * not, and takes precedence over a child alarm's active state.
	 */
	protected boolean isActive;

	/* *************************************  Constructors  ************************************* */

	/**
	 * Creates a new item with the given name and id.
	 * @param name the new name of the item
	 * @param id the id of the item
	 */
	protected Item(@Nullable String name, int id) {
		this.id = id;
		this.name = "";
		isActive = true;

		setListableName(name);
	}

	/* *************************************  Concrete Methods  ********************************* */

	/**
	 * Returns the ID of the listable.
	 */
	@Contract(pure = true)
	public int getId() { return id; }

	/**
	 * Gets the name of the listable.
	 * @return the name of the listable, cannot be null 
	 */
	@Contract(pure = true) @NotNull
	public String getListableName() { return name; }

	/**
	 * Sets the name of the listable. Nonzero return codes mean an error has occurred. If the new
	 * name is null or empty, the method returns 1, if it contains a restricted character, returns 2.
	 * @param newName the new name to set it to, can be null
	 * @return 0 (no error) or an error code specified above
	 */
	public int setListableName(@Nullable String newName) {
		if (newName == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "New name is null.");
			return 1;
		}

		if (newName.indexOf('\t') != -1 || newName.indexOf('/') != -1) {
			if (BuildConfig.DEBUG) Log.e(TAG, "New name has invalid characters in it.");
			return 2;
		}

		name = newName;
		return 0;
	}

	/**
	 * Returns whether the Listable is active or not.
	 */
	@Contract(pure = true)
	public boolean isActive() { return isActive; }

	/**
	 * Sets the active state of the Listable.
	 * @param isOn the new active state to set the LIstable to
	 */
	public void setActive(boolean isOn) { isActive = isOn; }

	/**
	 * Turns the Listable on.
	 */
	public void turnOn() { isActive = true; }

	/**
	 * Turns the Listable off.
	 */
	public void turnOff() { isActive = false; }

	/**
	 * Toggles the active state of the Listable (if it was on, turn it off; if it was off, turn it on).
	 */
	public void toggleActive() { isActive = !isActive; }

	/* *************************************  Abstract Methods  ********************************* */

	/**
	 * Returns a string that shows how the Listable (Alarm) repeats (or blank).
	 * @return returns a repeat string, cannot be null 
	 */
	@NotNull @Contract(pure = true)
	public abstract String getRepeatString();

	/**
	 * Returns a string (a time only) that shows when the Alarm will repeat next (blank for folders).
	 * @return returns a time string, cannot be null 
	 */
	@NotNull @Contract(pure = true)
	public abstract String getNextRingTime();

	/**
	 * Returns the number of items within the Listable, including the Listable itself.
	 */
	@Contract(pure = true)
	public abstract int size();

	/**
	 * Determines whether two objects are equal to each other or not.
	 * @param other the other object to compare to
	 * @return whether the two objects are equal or not
	 */
	public abstract boolean equals(Object other);

	/**
	 * Outputs a string for editing purposes.
	 * @return an edit string, cannot be null 
	 */
	@NotNull @Contract(pure = true)
	public abstract String toEditString();

	/**
	 * Outputs a string for storing purposes.
	 * @return a store string, cannot be null
	 */
	@NotNull @Contract(pure = true)
	abstract String toStoreString();
}
