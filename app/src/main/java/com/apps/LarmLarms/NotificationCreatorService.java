package com.apps.LarmLarms;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

public class NotificationCreatorService extends Service {
	private static final String TAG = "NotificationCreator";

	public static final String CHANNEL_ID = "RingingAlarms";
	public static final String NOTIFICATION_TAG = "LARMLARM_ALARM";
	public static final int NOTIFICATION_ID = 210;

	public NotificationCreatorService() {}

	@Override
	public int onStartCommand(Intent inIntent, int flags, int startId) {
		Alarm currAlarm = Alarm.fromEditString(this, inIntent.getStringExtra(MainActivity.EXTRA_LISTABLE));
		if (currAlarm == null) {
			Log.e(TAG, "Alarm was invalid.");
			stopSelf();
			return Service.START_NOT_STICKY;
		}

		Intent fullScreenIntent = new Intent(this, AlarmRingingActivity.class);
		fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		fullScreenIntent.putExtra(MainActivity.EXTRA_LISTABLE, currAlarm.toEditString());
		fullScreenIntent.putExtra(MainActivity.EXTRA_LISTABLE_INDEX,
				inIntent.getIntExtra(MainActivity.EXTRA_LISTABLE_INDEX, -1));
		PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0, fullScreenIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		RemoteViews notifView = new RemoteViews(getPackageName(), R.layout.alarm_notification);
		notifView.setTextViewText(R.id.alarm_name_text, currAlarm.getListableName());
		notifView.setOnClickPendingIntent(R.id.snooze_button, fullScreenPendingIntent);
		notifView.setOnClickPendingIntent(R.id.dismiss_button, fullScreenPendingIntent);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(getResources().getString(R.string.app_name))
				.setContentText(currAlarm.getListableName())
				.setTicker(getResources().getString(R.string.notif_ticker))
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setDefaults(Notification.DEFAULT_ALL)
				.setContentIntent(fullScreenPendingIntent)
				.setAutoCancel(true)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setOngoing(true)
				.setFullScreenIntent(fullScreenPendingIntent, true)
				.setStyle(new NotificationCompat.DecoratedCustomViewStyle())
				.setCustomContentView(notifView)
				.setCustomBigContentView(notifView);
		startForeground(NOTIFICATION_ID, builder.build());

		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) { return null; }
}
