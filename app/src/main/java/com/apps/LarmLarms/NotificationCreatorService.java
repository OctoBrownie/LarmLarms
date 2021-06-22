package com.apps.LarmLarms;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

/**
 * A very short-term service that runs in the background of a currently ringing alarm. Manages the
 * notification for the alarm and playing the alarm sounds.
 */
public class NotificationCreatorService extends Service implements MediaPlayer.OnPreparedListener,
		MediaPlayer.OnErrorListener {
	private static final String TAG = "NotificationCreator";

	static final String CHANNEL_ID = "RingingAlarms";
	static final int NOTIFICATION_ID = 42;

	private MediaPlayer mediaPlayer;

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
		// TODO: add new pending intents that actually reflect these buttons' purposes
		notifView.setOnClickPendingIntent(R.id.snooze_button, fullScreenPendingIntent);
		notifView.setOnClickPendingIntent(R.id.dismiss_button, fullScreenPendingIntent);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(getResources().getString(R.string.app_name))
				.setContentText(currAlarm.getListableName())
				.setTicker(getResources().getString(R.string.notif_ticker))
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
				.setSound(null)
				.setContentIntent(fullScreenPendingIntent)
				.setAutoCancel(true)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setOngoing(true)
				.setFullScreenIntent(fullScreenPendingIntent, true)
				.setStyle(new NotificationCompat.DecoratedCustomViewStyle())
				.setCustomContentView(notifView)
				.setCustomBigContentView(notifView)
				.setCustomHeadsUpContentView(notifView);

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
				.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
				.setUsage(AudioAttributes.USAGE_ALARM)
				.build()
		);
		mediaPlayer.setLooping(true);
		mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

		try {
			mediaPlayer.setDataSource(this, currAlarm.getRingtoneUri());
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.prepareAsync();
		}
		catch (Exception e) {
			Log.e(TAG, "Something went wrong while initializing the alarm sounds.");
			stopSelf();
			return Service.START_NOT_STICKY;
		}

		startForeground(NOTIFICATION_ID, builder.build());

		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) { return null; }

	@Override
	public void onDestroy() { mediaPlayer.release(); }

	/* ****************************  MediaPlayer Lifecycle Methods  ***************************** */

	/**
	 * Callback for MediaPlayer.OnPreparedListener.
	 */
	@Override
	public void onPrepared(MediaPlayer mp) { mediaPlayer.start(); }

	/**
	 * Callback for MediaPlayer.OnErrorListener.
	 */
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.e(TAG, "Something went wrong while playing the alarm sounds.");
		mediaPlayer.release();
		stopSelf();
		return false;
	}

}
