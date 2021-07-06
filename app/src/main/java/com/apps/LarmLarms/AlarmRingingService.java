package com.apps.LarmLarms;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

import org.jetbrains.annotations.Nullable;

import androidx.core.app.NotificationCompat;

/**
 * A very short-term service that runs in the background of a currently ringing alarm. Manages the
 * notification for the alarm and playing the alarm sounds.
 */
public class AlarmRingingService extends Service implements MediaPlayer.OnPreparedListener,
		MediaPlayer.OnErrorListener {
	private static final String TAG = "NotificationCreator";

	static final String CHANNEL_ID = "RingingAlarms";
	static final int NOTIFICATION_ID = 42;

	private MediaPlayer mediaPlayer;

	private int alarmAbsIndex;

	private boolean boundToDataService = false;
	private ServiceConnection dataConn;

	private Messenger dataService;
	private Message unsentMessage;

	public AlarmRingingService() {}

	@Override
	public int onStartCommand(Intent inIntent, int flags, int startId) {
		Alarm currAlarm = Alarm.fromEditString(this,
				inIntent.getStringExtra(ListableEditorActivity.EXTRA_LISTABLE));
		if (currAlarm == null) {
			Log.e(TAG, "Alarm was invalid.");
			stopSelf();
			return Service.START_NOT_STICKY;
		}

		alarmAbsIndex = inIntent.getIntExtra(ListableEditorActivity.EXTRA_LISTABLE_INDEX, -1);

		// setting up custom foreground notification
		Intent fullScreenIntent = new Intent(this, AlarmRingingActivity.class);
		fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		fullScreenIntent.putExtra(ListableEditorActivity.EXTRA_LISTABLE, currAlarm.toEditString());
		PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0, fullScreenIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		Intent dismissIntent = new Intent(fullScreenIntent);
		dismissIntent.setAction(AlarmRingingActivity.ACTION_DISMISS);
		PendingIntent dismissPendingIntent = PendingIntent.getActivity(this, 0, dismissIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		Intent snoozeIntent = new Intent(fullScreenIntent);
		snoozeIntent.setAction(AlarmRingingActivity.ACTION_SNOOZE);
		PendingIntent snoozePendingIntent = PendingIntent.getActivity(this, 0, snoozeIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		RemoteViews notifView = new RemoteViews(getPackageName(), R.layout.alarm_notification);
		notifView.setTextViewText(R.id.alarm_name_text, currAlarm.getListableName());

		notifView.setOnClickPendingIntent(R.id.snooze_button, snoozePendingIntent);
		notifView.setOnClickPendingIntent(R.id.dismiss_button, dismissPendingIntent);

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

		// media player setup
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
	public IBinder onBind(Intent intent) {
		dataConn = new DataServiceConnection();
		bindService(new Intent(this, AlarmDataService.class), dataConn, Context.BIND_AUTO_CREATE);

		Messenger messenger = new Messenger(new ActivityHandler(this));
		return messenger.getBinder();
	}

	@Override
	public void onDestroy() {
		if (boundToDataService) {
			boundToDataService = false;
			dataService = null;
			unbindService(dataConn);
		}
		mediaPlayer.release();
	}

	/* ********************************  MediaPlayer Callbacks  ******************************** */

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

	/* *************************************  Other Methods  ************************************ */

	/**
	 * Sends a message to the data service using the service's messenger.
	 * @param msg the message to send, can be null
	 */
	private void sendMessage(@Nullable Message msg) {
		try {
			dataService.send(msg);
		}
		catch (NullPointerException | RemoteException e) {
			Log.e(TAG, "Data service is unavailable. Caching message.");
			unsentMessage = msg;
		}
	}

	/* *************************************  Inner Classes  *********************************** */

	/**
	 * Inner Handler class for handling messages from AlarmRingingActivity. Handles
	 * AlarmDataService.MSG_SNOOZE_ALARM and AlarmDataService.MSG_DISMISS_ALARM messages, but doesn't
	 * check any of the fields.
	 */
	private static class ActivityHandler extends Handler {
		AlarmRingingService service;

		private ActivityHandler(AlarmRingingService service) {
			super(Looper.getMainLooper());
			this.service = service;
		}

		@Override
		public void handleMessage(Message msg) {
			Message outMsg;
			switch(msg.what) {
				case AlarmDataService.MSG_SNOOZE_ALARM:
					service.stopForeground(true);
					outMsg = Message.obtain(null, AlarmDataService.MSG_SNOOZE_ALARM, service.alarmAbsIndex);
					service.sendMessage(outMsg);
					break;
				case AlarmDataService.MSG_DISMISS_ALARM:
					service.stopForeground(true);
					outMsg = Message.obtain(null, AlarmDataService.MSG_DISMISS_ALARM, service.alarmAbsIndex);
					service.sendMessage(outMsg);
					break;
				default:
					Log.e(TAG, "Unknown message type. Sending to Handler's handleMessage().");
					super.handleMessage(msg);
					break;
			}
		}
	}

	private class DataServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			boundToDataService = true;
			dataService = new Messenger(service);

			if (unsentMessage != null) {
				sendMessage(unsentMessage);
				stopSelf();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.e(TAG, "The data service crashed.");
			boundToDataService = false;
			dataService = null;

		}
	}
}
