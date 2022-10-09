package com.larmlarms.main;

import android.app.Application;

/**
 * The main application. Stores some app-wide variables.
 */

public class MainApplication extends Application {
	/**
	 * Whether the entire app needs restarting. Used for preference changes.
	 */
	public boolean needsRestart = false;
}
