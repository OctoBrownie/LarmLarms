package com.larmlarms.main;

import android.app.Application;

import com.larmlarms.R;
import com.larmlarms.data.RootFolder;

/**
 * The main application. Stores some app-wide variables.
 */

public class MainApplication extends Application {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "MainApplication";


	/**
	 * Whether the entire app needs restarting. Used for preference changes.
	 */
	public boolean needsRestart = false;

	/**
	 * Root folder to for all activities and services in the process to access.
	 */
	public final RootFolder rootFolder;

	/**
	 * Constructor for the main application. Main job is to initialize the root folder.
	 */
	MainApplication() {
		rootFolder = new RootFolder(getResources().getString(R.string.root_folder), RootFolder.getAlarmsFromDisk(this), this);
	}
}
