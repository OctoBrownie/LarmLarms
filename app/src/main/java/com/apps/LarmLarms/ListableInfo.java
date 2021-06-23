package com.apps.LarmLarms;

/**
 * A struct containing information about a listable within a nested list of Listables
 */

class ListableInfo {
	int absIndex, relIndex, numIndents;
	Listable listable;
	AlarmGroup parentListable;

	ListableInfo() {
		relIndex = 0;
		absIndex = 0;
		numIndents = 0;
		listable = null;
		parentListable = null;
	}
}
