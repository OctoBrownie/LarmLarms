package com.larmlarms.ringing;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.larmlarms.BuildConfig;
import com.larmlarms.Constants;
import com.larmlarms.data.Alarm;
import com.larmlarms.data.ItemInfo;
import com.larmlarms.main.MainApplication;

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

		ItemInfo info = inIntent.getParcelableExtra(Constants.EXTRA_ITEM_INFO);
		if (info == null || info.item == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The info struct was null.");
			stopSelf();
			return Service.START_NOT_STICKY;
		}

		Alarm a = (Alarm) ((MainApplication) getApplication()).rootFolder
				.getItemById(info.path, info.item.getId());
		if (a == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The alarm was null.");
			return Service.START_NOT_STICKY;
		}
		if (Constants.ACTION_DISMISS.equals(inIntent.getAction())) a.dismiss();
		else a.snooze();

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
}
