package com.larmlarms.ringing;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.larmlarms.BuildConfig;
import com.larmlarms.data.AlarmDataService;

import org.jetbrains.annotations.NotNull;

/**
 * A very short-term service that runs in the background after an alarm has finished ringing. Sends
 * the dismiss or snooze messages to the data service.
 */
public class AfterRingingService extends Service {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "AfterRingingService";

	/**
	 * Action used in intents, used for snoozing alarms. Does not assume anything about the contents
	 * of the intent.
	 */
	static final String ACTION_SNOOZE = "com.apps.larmlarms.action.ACTION_SNOOZE";
	/**
	 * Action used in intents, used for dismissing alarms. Does not assume anything about the
	 * contents of the intent.
	 */
	static final String ACTION_DISMISS = "com.apps.larmlarms.action.ACTION_DISMISS";

	/**
	 * The absolute index of the alarm that finished ringing.
	 */
	private int alarmAbsIndex;

	/**
	 * Whether or not to dismiss the current alarm.
	 */
	private boolean dismissAlarm;

	/**
	 * Called when the service is started. The intent send to the service should have the intended
	 * action (snooze or dismiss, actions specified in RingingActivity) and should contain the
	 * alarm's absolute index in the intent extras (int extra with key RingingService.EXTRA_LISTABLE).
	 * @param inIntent the intent used to start the service
	 * @param flags any flags given to the service
	 * @param startId a unique id from this particular start code
	 * @return how the system should handle the service, always returns Service.START_NOT_STICKY
	 */
	@Override
	public int onStartCommand(@NotNull Intent inIntent, int flags, int startId) {
		stopService(new Intent(this, RingingService.class));

		alarmAbsIndex = inIntent.getIntExtra(RingingService.EXTRA_ITEM_PATH, -1);
		if (alarmAbsIndex == -1) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The absolute index was not specified.");
			stopSelf();
			return Service.START_NOT_STICKY;
		}

		bindService(new Intent(this, AlarmDataService.class)
						.putExtra(AlarmDataService.EXTRA_NO_UPDATE, true),
				new DataServiceConnection(), Context.BIND_AUTO_CREATE);

		dismissAlarm = ACTION_DISMISS.equals(inIntent.getAction());

		return Service.START_NOT_STICKY;
	}

	/**
	 * Binding is not supported for this class. Will throw an UnsupportedOperationException.
	 * @param intent the intent used to bind to the service, unused in this implementation
	 */
	@Override
	public IBinder onBind(@NotNull Intent intent) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/* *************************************  Inner Classes  *********************************** */

	/**
	 * Service connection to the data service.
	 */
	private class DataServiceConnection implements ServiceConnection {
		/**
		 * Called when the data service connects to this service. Sets some fields in the service
		 * and sends any unsent messages. If an unsent message is sent, then the service is killed.
		 * @param className the name of the class that was bound to (unused)
		 * @param service the binder that the service returned
		 */
		@Override
		public void onServiceConnected(@NotNull ComponentName className, @NotNull IBinder service) {
			Messenger dataService = new Messenger(service);
			Message msg = Message.obtain();

			if (dismissAlarm) msg.what = AlarmDataService.MSG_DISMISS_ALARM;
			else msg.what = AlarmDataService.MSG_SNOOZE_ALARM;
			msg.arg1 = alarmAbsIndex;

			try {
				dataService.send(msg);
			}
			catch (RemoteException e) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't send the message to the data service.");
			}

			unbindService(this);
			stopSelf();
		}

		/**
		 * Called when the data service crashes. Stops the entire service.
		 * @param className the name of the class that was bound to (unused)
		 */
		@Override
		public void onServiceDisconnected(@NotNull ComponentName className) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The data service crashed.");
			stopSelf();
		}
	}
}
