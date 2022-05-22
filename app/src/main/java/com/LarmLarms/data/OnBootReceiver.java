package com.LarmLarms.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.LarmLarms.R;

/**
 * Receives messages from the system that the device has just booted. Starts up the data service
 * so that alarms are registered in the system.
 */
public class OnBootReceiver extends BroadcastReceiver {
	private final static String TAG = "OnBootReceiver";

	/**
	 * Only receives messages for device booting.
	 * @param con the current context
	 * @param intent the intent to respond to
	 */
	@Override
	public void onReceive(Context con, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Context context = con.getApplicationContext();

			AlarmGroup rootFolder = new AlarmGroup(context.getResources().getString(R.string.root_folder), AlarmDataService.getAlarmsFromDisk(context));
			AlarmDataService.createNotificationChannel(context);
			AlarmDataService.setNextAlarmToRing(context, rootFolder);
			Log.i(TAG, "Done~");
		}
	}
}
