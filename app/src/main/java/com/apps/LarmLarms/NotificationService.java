package com.apps.LarmLarms;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class NotificationService extends Service {
	private static final String TAG = "NotificationService";
	public static final String CHANNEL_ID = "RingingAlarms";

	public NotificationService() {}

	@Override
	public int onStartCommand(Intent inIntent, int flags, int startId) {
		Alarm currAlarm = Alarm.fromEditString(this, inIntent.getStringExtra(MainActivity.EXTRA_LISTABLE));
		if (currAlarm == null) {
			Log.e(TAG, "Alarm was invalid.");
			stopSelf();
			return Service.START_NOT_STICKY;
		}

		Intent outIntent = new Intent(this, AlarmRingingActivity.class);
		outIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		outIntent.putExtra(MainActivity.EXTRA_LISTABLE, currAlarm.toEditString());
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, outIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(getResources().getString(R.string.app_name))
				.setContentText(currAlarm.getListableName())
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setContentIntent(pendingIntent)
				.setAutoCancel(true)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setFullScreenIntent(pendingIntent, true);

		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (manager != null) {
			manager.notify(0, builder.build());
		}

		stopSelf();
		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) { return null; }
}
