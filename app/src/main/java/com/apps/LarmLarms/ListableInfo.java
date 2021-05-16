package com.apps.LarmLarms;

/**
 * A struct containing information about a listable within a nested list of Listables
 */

class ListableInfo {
	int index;
	Listable listable;

	ListableInfo() {
		index = 0;
		listable = null;
	}
}
