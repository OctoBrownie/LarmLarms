package com.apps.LarmLarms;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class allowing the RecyclerViewAdapter to access specific parts of either Alarms or AlarmGroups.
 * Using isAlarm(), it can also discriminate between Alarms and AlarmGroups. This is meant mostly
 * to expose the data necessary for displaying alarms on the main page.
 */
interface Listable {
	/**
	 * Differentiates between alarms and folders.
	 * @return whether the Listable is an alarm or folder
	 */
	@Contract(pure = true)
	boolean isAlarm();

	/**
	 * Gets the name of the listable.
	 * @return the name of the listable, cannot be null 
	 */
	@NotNull @Contract(pure = true)
	String getListableName();
	/**
	 * Sets the name of the listable. If the name is null, shouldn't do anything. 
	 * @param newName the new name to set it to, can be null
	 */
	void setListableName(@Nullable String newName);

	/**
	 * Returns a string that shows how the Listable (Alarm) repeats (or blank).
	 * @return returns a repeat string, cannot be null 
	 */
	@NotNull @Contract(pure = true)
	String getRepeatString();

	/**
	 * Returns a string (a time only) that shows when the Alarm will repeat next (blank for folders).
	 * @return returns a time string, cannot be null 
	 */
	@NotNull @Contract(pure = true)
	String getNextRingTime();

	/**
	 * Returns whether the Listable is active or not. 
	 */
	@Contract(pure = true)
	boolean isActive();
	/**
	 * Sets the active state of the Listable.
	 * @param isOn the new active state to set the LIstable to 
	 */
	void setActive(boolean isOn);
	/**
	 * Turns the Listable on.
	 */
	void turnOn();
	/**
	 * Turns the Listable off. 
	 */
	void turnOff();
	/**
	 * Toggles the active state of the Listable (if it was on, turn it off; if it was off, turn it on). 
	 */
	void toggleActive();

	/**
	 * Returns the number of items within the Listable, including the Listable itself.
	 */
	@Contract(pure = true)
	int size();

	/**
	 * Clones the Listable. Makes the clone() function required and public.
	 * @return a Listable, can be null but shouldn't be 
	 */
	@Nullable @Contract(pure = true)
	Listable clone();

	/**
	 * Outputs a string for editing purposes.
	 * @return an edit string, cannot be null 
	 */
	@NotNull @Contract(pure = true)
	String toEditString();

	/**
	 * Outputs a string for storing purposes.
	 * @return a store string, cannot be null
	 */
	@NotNull @Contract(pure = true)
	String toStoreString();
}
