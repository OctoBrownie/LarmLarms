package com.larmlarms.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.larmlarms.main.MainApplication;

/**
 * Receives messages from the system that the device has just booted. Starts up the data service
 * so that alarms are registered in the system.
 */
public class OnBootReceiver extends BroadcastReceiver {

	/**
	 * Only receives messages for device booting.
	 * @param con the current context
	 * @param intent the intent to respond to
	 */
	@Override
	public void onReceive(Context con, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Context context = con.getApplicationContext();

			if (!(context instanceof MainApplication)) {
				// constructor registers the alarms automatically
				new RootFolder("", context);
			}
		}
	}
}
