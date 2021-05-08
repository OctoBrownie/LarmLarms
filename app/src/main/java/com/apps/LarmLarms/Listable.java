package com.apps.LarmLarms;

/**
 * Class allowing the RecyclerViewAdapter to access specific parts of either Alarms or AlarmGroups.
 * Using isAlarm(), it can also discriminate between Alarms and AlarmGroups. This is meant mostly
 * to expose the data necessary for displaying alarms on the main page.
 */

interface Listable {
	// allows calling classes to differentiate between Alarms and Folders
	boolean isAlarm();

	// getter/setter for the (user-defined) name of the listable
	String getListableName();
	void setListableName(String newName);

	// returns a string that shows how the Alarm repeats (blank for folders)
	String getRepeatString();

	// returns a string (a time) showing when the Alarm will repeat next (blank for folders)
	String getNextRingTime();

	// returns whether the Listable is on or off
	boolean isActive();

	// returns the number of items within the Listable, including the Listable itself
	int getNumItems();

	// makes the clone() function required and public
	Listable clone();

	// outputs a string for editing purposes
	String toEditString();

	// outputs a string for storing purposes
	String toStoreString();
}
