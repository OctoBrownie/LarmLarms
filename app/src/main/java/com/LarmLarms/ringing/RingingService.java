package com.larmlarms.ringing;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.RemoteViews;

import com.larmlarms.BuildConfig;
import com.larmlarms.R;
import com.larmlarms.data.Alarm;
import com.larmlarms.data.AlarmDataService;
import com.larmlarms.main.PrefsActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import androidx.core.app.NotificationCompat;

/**
 * A very short-term service that runs in the background of a currently ringing alarm. Manages the
 * notification for the alarm and playing the alarm sounds.
 */
public class RingingService extends Service implements MediaPlayer.OnPreparedListener,
		MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "RingingService";

	/* ***********************************  Intent extras *************************************** */
	/**
	 * An extra used for carrying an itme in edit string form.
	 */
	public final static String EXTRA_ITEM = "com.apps.larmlarms.extra.ITEM";
	/**
	 * An extra used for carrying an item's path within the data.
	 */
	public final static String EXTRA_ITEM_PATH = "com.apps.larmlarms.extra.PATH";

	/* *********************************  Other static fields  ********************************** */

	/**
	 * String ID for the notification channel the foreground notifications are posted in.
	 */
	public static final String CHANNEL_ID = "RingingAlarms";
	/**
	 * The int ID for the foreground notification itself. There should only be one at any given time,
	 * so using the same ID should be fine.
	 */
	public static final int NOTIFICATION_ID = 42;

	/* ***********************************  Non-static fields ********************************* */

	/**
	 * The current alarm being presented.
	 */
	private Alarm alarm;
	/**
	 * The path of the alarm that's currently ringing.
	 */
	private String alarmPath;

	/**
	 * Plays the ringtone of the alarm. Can be null if the alarm is silent.
	 */
	@Nullable
	private MediaPlayer mediaPlayer;

	/**
	 * Shows whether audio focus is currently granted to us.
	 */
	private boolean audioFocused;

	/**
	 * Shows whether the media player is prepared for playback or not.
	 */
	private boolean playerPrepared;

	/**
	 * Audio focus request to gain/abandon audio focus.
	 */
	@Nullable
	private AudioFocusRequest audioFocusRequest;

	/**
	 * Cached reference to the audio manager.
	 */
	@Nullable
	private AudioManager audioManager;

	/**
	 * Cached reference to the vibrator. Can be null if vibration is disabled or if there is no
	 * vibrator.
	 */
	@Nullable
	private Vibrator vibrator;

	/**
	 * Creates a new AlarmRingingService and initializes a connection to the data service.
	 */
	public RingingService() {
		audioFocused = false;
		playerPrepared = false;
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
		createNotificationChannel(this);

		alarm = Alarm.fromEditString(this, inIntent.getStringExtra(EXTRA_ITEM));
		if (alarm == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Alarm was invalid.");
			stopSelf();
			return Service.START_NOT_STICKY;
		}

		alarmPath = inIntent.getStringExtra(EXTRA_ITEM_PATH);

		// flags for the pending intents
		int PIFlags = PendingIntent.FLAG_UPDATE_CURRENT;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
			PIFlags = PIFlags | PendingIntent.FLAG_MUTABLE;

		// setting up custom foreground notification
		Intent fullScreenIntent = new Intent(this, RingingActivity.class);
		fullScreenIntent.putExtra(EXTRA_ITEM_PATH, alarmPath);
		fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		fullScreenIntent.putExtra(EXTRA_ITEM, alarm.toEditString());
		PendingIntent fullscreenPI = PendingIntent.getActivity(this, 0, fullScreenIntent, PIFlags);

		Intent dismissIntent = new Intent(this, AfterRingingService.class);
		dismissIntent.putExtra(EXTRA_ITEM_PATH, alarmPath);
		dismissIntent.setAction(AfterRingingService.ACTION_DISMISS);
		PendingIntent dismissPI = PendingIntent.getService(this, 0, dismissIntent, PIFlags);

		Intent snoozeIntent = new Intent(dismissIntent);
		snoozeIntent.setAction(AfterRingingService.ACTION_SNOOZE);
		PendingIntent snoozePendingIntent = PendingIntent.getService(this, 0, snoozeIntent, PIFlags);

		// this is so stupid but we can't change styles/themes of a remote view
		int notifLayout;
		SharedPreferences prefs = getSharedPreferences(PrefsActivity.PREFS_KEY, MODE_PRIVATE);
		int themeId = prefs.getInt(PrefsActivity.PREF_THEME_KEY, R.style.AppTheme_Beach);
		if (themeId == R.style.AppTheme_Beach) {
			notifLayout = R.layout.alarm_notification_beach;
		}
		else if (themeId == R.style.AppTheme_Candy) {
			notifLayout = R.layout.alarm_notification_candy;
		}
		else if (themeId == R.style.AppTheme_Grey) {
			notifLayout = R.layout.alarm_notification_grey;
		}
		else if (themeId == R.style.AppTheme_Mint) {
			notifLayout = R.layout.alarm_notification_mint;
		}
		else {
			if (BuildConfig.DEBUG) Log.e(TAG, "Unknown theme specified!");
			stopSelf();
			return Service.START_NOT_STICKY;
		}
		PrefsActivity.applyPrefsStyle(this);

		RemoteViews notifView = new RemoteViews(getPackageName(), notifLayout);
		notifView.setTextViewText(R.id.alarm_name_text, alarm.getName());

		// we need this line to ensure actions pop up on the heads up notification
		notifView.setOnClickPendingIntent(R.id.snoozeButton, snoozePendingIntent);
		notifView.setOnClickPendingIntent(R.id.dismissButton, dismissPI);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(alarm.getName())
				.setContentText(getResources().getString(R.string.notif_description))
				.setTicker(getResources().getString(R.string.notif_ticker))
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setDefaults(Notification.DEFAULT_LIGHTS)
				.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
						getPackageName() + "/raw/silence"))
				.setVibrate(new long[]{0})
				.setContentIntent(fullscreenPI)
				.setAutoCancel(true)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setOngoing(true)
				.setFullScreenIntent(fullscreenPI, true)
				.setStyle(new NotificationCompat.DecoratedCustomViewStyle())
				.setCustomHeadsUpContentView(notifView);


		// ringtone setup
		AudioAttributes audioAttributes = new AudioAttributes.Builder()
				.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
				.setUsage(AudioAttributes.USAGE_ALARM)
				.build();
		if (alarm.getRingtoneUri() != null && alarm.getVolume() != 0) {

			// media player setup
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioAttributes(audioAttributes);
			mediaPlayer.setLooping(true);
			mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

			try {
				mediaPlayer.setDataSource(this, alarm.getRingtoneUri());
				mediaPlayer.setOnPreparedListener(this);
				mediaPlayer.prepareAsync();
			}
			catch (Exception e) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Something went wrong while initializing the alarm sounds.");
				stopSelf();
				return Service.START_NOT_STICKY;
			}

			// audio focus request
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
				AudioFocusRequest.Builder requestBuilder = new AudioFocusRequest.Builder(
						AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
				requestBuilder.setAudioAttributes(audioAttributes)
						.setAcceptsDelayedFocusGain(true)
						.setOnAudioFocusChangeListener(this);
				audioFocusRequest = requestBuilder.build();
				if (audioFocusRequest == null) {
					if (BuildConfig.DEBUG) Log.wtf(TAG, "Audio focus request was null...");
					stopSelf();
					return Service.START_NOT_STICKY;
				}

				audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				if (audioManager == null) {
					if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't reach the audio manager.");
					exitService();
					stopSelf();
					return Service.START_NOT_STICKY;
				}
				int focus = audioManager.requestAudioFocus(audioFocusRequest);
				switch(focus) {
					case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
						audioFocused = true;
						if (mediaPlayer != null && playerPrepared) mediaPlayer.start();
						break;
					case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
					case AudioManager.AUDIOFOCUS_REQUEST_DELAYED:
						audioFocused = false;
						break;
				}
			}
		}

		if (alarm.isVibrateOn()) {
			// vibrator setup
			vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			if (vibrator == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Couldn't reach the vibrator.");
				exitService();
				stopSelf();
				return Service.START_NOT_STICKY;
			}

			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
				VibrationEffect e = VibrationEffect.createWaveform(Alarm.VIBRATION_PATTERN, 1);
				vibrator.vibrate(e, audioAttributes);
			}
			else {
				vibrator.vibrate(Alarm.VIBRATION_PATTERN, 1);
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
	 * Called when the service is being destroyed. Unbinds from the data service if it is bound,
	 * releases the media player if it's not null, and releases audio focus if necessary.
	 */
	@Override
	public void onDestroy() {
		exitService();
	}

	/**
	 * Closes everything. Takes care of the data connection, media player, audio focus, and vibrator.
	 * Doesn't stop the service, though.
	 */
	private void exitService() {
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				if (audioManager != null && audioFocusRequest != null)
					audioManager.abandonAudioFocusRequest(audioFocusRequest);
			}
		}

		if (vibrator != null) {
			vibrator.cancel();
		}
	}

	/* ********************************  MediaPlayer Callbacks  ******************************** */

	/**
	 * Callback for MediaPlayer.OnPreparedListener.
	 * @param mp the media player that was just prepared.
	 */
	@Override
	public void onPrepared(@NotNull MediaPlayer mp) {
		float vol = alarm.getVolume() / 100f;
		mp.setVolume(vol, vol);
		playerPrepared = true;
		if (audioFocused) mp.start();
	}

	/**
	 * Callback for MediaPlayer.OnErrorListener.
	 * @param mp the media player with an error
	 * @param what the type of error that occurred
	 * @param extra extra code specific to the error
	 * @return whether the error was handled or not, always returns false
	 */
	@Override
	public boolean onError(@NotNull MediaPlayer mp, int what, int extra) {
		if (BuildConfig.DEBUG) Log.e(TAG, "Something went wrong while playing the alarm sounds.");
		exitService();
		stopSelf();
		return false;
	}

	/**
	 * For audio focus requests when it becomes not delayed.
	 * @param focusChange the new focus state
	 */
	@Override
	public void onAudioFocusChange(int focusChange) {
		switch(focusChange) {
			case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
				audioFocused = true;
				if (mediaPlayer != null && playerPrepared && !mediaPlayer.isPlaying())
					mediaPlayer.start();
				break;
			case AudioManager.AUDIOFOCUS_LOSS:
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				audioFocused = false;
				if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.stop();
				break;
		}
	}

	/* *************************************  Other Methods  ************************************ */

	/**
	 * Sends a message to the data service using the service's messenger.
	 * @param msg the message to send, can be null
	 * @return whether the message was sent successfully or not
	 */
	private boolean sendMessage(@Nullable Message msg) {
		if (dataService == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Data service is null. Caching message.");
			unsentMessage = msg;
			return false;
		}

		try {
			dataService.send(msg);
		}
		catch (RemoteException e) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Data service is unavailable. Caching message.");
			unsentMessage = msg;
			return false;
		}
		return true;
	}

	/**
	 * Creates a notification channel if the API level requires it. Otherwise, does nothing.
	 * @param context the current context
	 */
	static void createNotificationChannel(@NotNull Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = context.getString(R.string.notif_channel_name);
			String description = context.getString(R.string.notif_channel_description);
			int importance = NotificationManager.IMPORTANCE_HIGH;

			NotificationChannel channel = new NotificationChannel(RingingService.CHANNEL_ID, name, importance);
			channel.setDescription(description);
			channel.setShowBadge(false);
			channel.setBypassDnd(true);
			channel.enableLights(true);

			AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
			attrBuilder.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
					.setUsage(AudioAttributes.USAGE_ALARM);
			channel.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
					context.getPackageName() + "/raw/silence"), attrBuilder.build());

			// Register the channel with the system; can't change the importance or behaviors after this
			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
			if (notificationManager == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "System returned a null notification manager.");
				return;
			}
			notificationManager.createNotificationChannel(channel);
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
		RingingService service;

		/**
		 * Creates a new handler and stores the service that called it.
		 * @param service the service that created the handler
		 */
		private ActivityHandler(@NotNull RingingService service) {
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
				if (BuildConfig.DEBUG) Log.e(TAG, "Message sent to the ringing service was null. Ignoring...");
				return;
			}

			Message outMsg;
			switch(msg.what) {
				case AlarmDataService.MSG_SNOOZE_ALARM:
					outMsg = Message.obtain(null, AlarmDataService.MSG_SNOOZE_ALARM, service.alarmPath, 0);
					break;
				case AlarmDataService.MSG_DISMISS_ALARM:
					outMsg = Message.obtain(null, AlarmDataService.MSG_DISMISS_ALARM, service.alarmPath, 0);
					break;
				default:
					if (BuildConfig.DEBUG) Log.e(TAG, "Unknown message type. Sending to Handler's handleMessage().");
					super.handleMessage(msg);
					return;
			}

			if (!service.sendMessage(outMsg)) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Message couldn't be sent. Waiting for reconnection.");
				return;
			}
			service.stopForeground(true);
			service.exitService();
			service.stopSelf();
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

				exitService();
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
			if (BuildConfig.DEBUG) Log.e(TAG, "The data service crashed.");

			exitService();
			stopSelf();
		}
	}
}
