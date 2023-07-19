package com.larmlarms.main;

import android.app.Application;

import com.larmlarms.R;
import com.larmlarms.data.RootFolder;

/**
 * The main application. Stores some app-wide variables.
 */

public class MainApplication extends Application {
	/**
	 * Whether the entire app needs restarting. Used for preference changes.
	 */
	public boolean needsRestart = false;

	/**
	 * Root folder to for all activities and services in the process to access.
	 */
	public RootFolder rootFolder;

	@Override
	public void onCreate() {
		super.onCreate();
		rootFolder = new RootFolder(getResources().getString(R.string.root_folder), this);
	}
}
