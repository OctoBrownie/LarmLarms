package com.apps.LarmLarms;

/**
 * A struct containing information about a listable within a nested list of Listables
 */

public class ListableInfo {
	public int index;
	public Listable listable;

	public ListableInfo() {
		index = 0;
		listable = null;
	}
}
