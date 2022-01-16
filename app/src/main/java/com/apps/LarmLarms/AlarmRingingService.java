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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import androidx.core.app.NotificationCompat;

/**
 * A very short-term service that runs in the background of a currently ringing alarm. Manages the
 * notification for the alarm and playing the alarm sounds.
 */
public class AlarmRingingService extends Service implements MediaPlayer.OnPreparedListener,
		MediaPlayer.OnErrorListener {
	/**
	 * Static flag to enable/disable all logging. 
	 */
	private static final boolean DEBUG = false;
	
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "AlarmRingingService";

	/* ***********************************  Intent extras *************************************** */
	/**
	 * An extra used for carrying a Listable in edit string form.
	 */
	final static String EXTRA_LISTABLE = "com.apps.AlarmsButBetter.LISTABLE";
	/**
	 * An extra used for carrying a Listable's absolute index within the data.
	 */
	final static String EXTRA_LISTABLE_INDEX = "com.apps.AlarmsButBetter.ABS_INDEX";

	/* *********************************  Other static fields  ********************************** */

	/**
	 * String ID for the notification channel the foreground notifications are posted in.
	 */
	static final String CHANNEL_ID = "RingingAlarms";
	/**
	 * The int ID for the foreground notification itself. There should only be one at any given time,
	 * so using the same ID should be fine.
	 */
	static final int NOTIFICATION_ID = 42;

	/* ***********************************  Non-static fields ********************************* */

	/**
	 * Plays the ringtone of the alarm. Can be null if the alarm is silent.
	 */
	@Nullable
	private MediaPlayer mediaPlayer;

	/**
	 * The absolute index of the alarm that's currently ringing.
	 */
	private int alarmAbsIndex;

	/**
	 * Shows whether it is bound to the data service or not.
	 */
	private boolean boundToDataService = false;
	/**
	 * The service connection to the data service.
	 */
	@NotNull
	private ServiceConnection dataConn;

	/**
	 * The messenger of the data service. Used for sending snooze/dismiss messages to it.
	 */
	@Nullable
	private Messenger dataService;
	/**
	 * An unsent message. Only one because this should only need to send one message per alarm.
	 */
	@Nullable
	private Message unsentMessage;

	/**
	 * Creates a new AlarmRingingService and initializes a connection to the data service.
	 */
	public AlarmRingingService() {
		dataConn = new DataServiceConnection();
	}

	/**
	 * Called when the service is started in the background.
	 * @param inIntent the intent used to start the service
	 * @param flags any flags given to the service
	 * @param startId a unique id from this particular start code
	 * @return how the system should handle the service, always returns Service.START_NOT_STICKY
	 */
	@Override
	public int onStartCommand(@NotNull Intent inIntent, int flags, int startId) {
		bindService(new Intent(this, AlarmDataService.class), dataConn, Context.BIND_AUTO_CREATE);

		Alarm currAlarm = Alarm.fromEditString(this,
				inIntent.getStringExtra(EXTRA_LISTABLE));
		if (currAlarm == null) {
			if (DEBUG) Log.e(TAG, "Alarm was invalid.");
			stopSelf();
			return Service.START_NOT_STICKY;
		}

		alarmAbsIndex = inIntent.getIntExtra(EXTRA_LISTABLE_INDEX, -1);

		// setting up custom foreground notification
		Intent fullScreenIntent = new Intent(this, AlarmRingingActivity.class);
		fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		fullScreenIntent.putExtra(EXTRA_LISTABLE, currAlarm.toEditString());
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

		if (currAlarm.getRingtoneUri() != null) {
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
				if (DEBUG) Log.e(TAG, "Something went wrong while initializing the alarm sounds.");
				stopSelf();
				return Service.START_NOT_STICKY;
			}
		}

		startForeground(NOTIFICATION_ID, builder.build());

		return Service.START_NOT_STICKY;
	}

	/**
	 * Called the first time a component wants to bind to this service. Creates a new messenger to
	 * communicate with clients.
	 * @param intent the intent used to bind to the service, unused in this implementation
	 * @return a messenger to AlarmRingingService
	 */
	@Override
	public IBinder onBind(@NotNull Intent intent) {
		Messenger messenger = new Messenger(new ActivityHandler(this));
		return messenger.getBinder();
	}

	/**
	 * Called when the service is being destroyed. Unbinds from the data service if it is bound and
	 * releases the media player if it's not null.
	 */
	@Override
	public void onDestroy() {
		if (boundToDataService) {
			boundToDataService = false;
			dataService = null;
			unbindService(dataConn);
		}
		if (mediaPlayer != null)
			mediaPlayer.release();
	}

	/* ********************************  MediaPlayer Callbacks  ******************************** */

	/**
	 * Callback for MediaPlayer.OnPreparedListener.
	 * @param mp the media player that was just prepared.
	 */
	@Override
	public void onPrepared(@NotNull MediaPlayer mp) { mp.start(); }

	/**
	 * Callback for MediaPlayer.OnErrorListener.
	 * @param mp the media player with an error
	 * @param what the type of error that occurred
	 * @param extra extra code specific to the error
	 * @return whether the error was handled or not, always returns false
	 */
	@Override
	public boolean onError(@NotNull MediaPlayer mp, int what, int extra) {
		if (DEBUG) Log.e(TAG, "Something went wrong while playing the alarm sounds.");
		if (mediaPlayer != null)
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
		if (dataService == null) {
			if (DEBUG) Log.e(TAG, "Data service is null. Caching message.");
			unsentMessage = msg;
			return;
		}

		try {
			dataService.send(msg);
		}
		catch (RemoteException e) {
			if (DEBUG) Log.e(TAG, "Data service is unavailable. Caching message.");
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
		/**
		 * The service that owns this handler.
		 */
		@NotNull
		AlarmRingingService service;

		/**
		 * Creates a new handler and stores the service that called it.
		 * @param service the service that created the handler
		 */
		private ActivityHandler(@NotNull AlarmRingingService service) {
			super(Looper.getMainLooper());
			this.service = service;
		}

		/**
		 * Handles messages from AlarmRingingActivity. Handles either AlarmDataService.MSG_SNOOZE_ALARM
		 * or AlarmDataService.MSG_DISMISS_ALARM.
		 * @param msg the incoming message from the activity
		 */
		@Override
		public void handleMessage(@Nullable Message msg) {
			if (msg == null) {
				if (DEBUG) Log.e(TAG, "Message sent to the ringing service was null. Ignoring...");
				return;
			}

			Message outMsg;
			switch(msg.what) {
				case AlarmDataService.MSG_SNOOZE_ALARM:
					service.stopForeground(true);
					if (service.mediaPlayer != null)
						service.mediaPlayer.stop();
					outMsg = Message.obtain(null, AlarmDataService.MSG_SNOOZE_ALARM, service.alarmAbsIndex, 0);
					service.sendMessage(outMsg);
					break;
				case AlarmDataService.MSG_DISMISS_ALARM:
					service.stopForeground(true);
					if (service.mediaPlayer != null)
						service.mediaPlayer.stop();
					outMsg = Message.obtain(null, AlarmDataService.MSG_DISMISS_ALARM, service.alarmAbsIndex, 0);
					service.sendMessage(outMsg);
					break;
				default:
					if (DEBUG) Log.e(TAG, "Unknown message type. Sending to Handler's handleMessage().");
					super.handleMessage(msg);
					break;
			}
		}
	}

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
			boundToDataService = true;
			dataService = new Messenger(service);

			if (unsentMessage != null) {
				sendMessage(unsentMessage);
				stopSelf();
			}
		}

		/**
		 * Called when the data service crashes. Unsets some fields in the outer service and stops
		 * the entire service.
		 * @param className the name of the class that was bound to (unused)
		 */
		@Override
		public void onServiceDisconnected(@NotNull ComponentName className) {
			if (DEBUG) Log.e(TAG, "The data service crashed.");
			boundToDataService = false;
			dataService = null;

			stopSelf();
		}
	}
}
