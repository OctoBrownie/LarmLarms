package com.larmlarms.ringing;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.RemoteViews;

import com.larmlarms.BuildConfig;
import com.larmlarms.Constants;
import com.larmlarms.R;
import com.larmlarms.data.Alarm;
import com.larmlarms.data.ItemInfo;
import com.larmlarms.main.PrefsActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import androidx.core.app.NotificationCompat;

/**
 * A short-term service that runs in the background of a currently ringing alarm. Manages the
 * notification for the alarm and playing the alarm sounds. Requires the alarm as an extra in the
 * intent as an extra using EditorActivity.EXTRA_ITEM_INFO as the key.
 */
public class RingingService extends Service implements MediaPlayer.OnPreparedListener,
		MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "RingingService";

	/* *********************************  Other static fields  ********************************** */

	/**
	 * String ID for the notification channel the foreground notifications are posted in.
	 */
	private static final String NOTIFICATION_CHANNEL_ID = "RingingAlarms";
	/**
	 * The int ID for the foreground notification itself. There should only be one at any given time,
	 * so using the same ID should be fine.
	 */
	private static final int NOTIFICATION_ID = 42;

	/* ***********************************  Non-static fields ********************************* */

	/**
	 * The current alarm that's ringing.
	 */
	private ItemInfo alarmInfo;

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

		alarmInfo = inIntent.getParcelableExtra(Constants.EXTRA_ITEM_INFO);
		if (alarmInfo == null || alarmInfo.item == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Alarm was invalid.");
			stopSelf();
			return Service.START_NOT_STICKY;
		}

		// flags for the pending intents
		int PIFlags = PendingIntent.FLAG_UPDATE_CURRENT;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
			PIFlags = PIFlags | PendingIntent.FLAG_MUTABLE;

		// setting up custom foreground notification
		Intent fullScreenIntent = new Intent(this, RingingActivity.class);
		fullScreenIntent.putExtra(Constants.EXTRA_ITEM_INFO, alarmInfo);
		fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent fullscreenPI = PendingIntent.getActivity(this, 0, fullScreenIntent, PIFlags);

		Intent dismissIntent = new Intent(this, AfterRingingService.class);
		dismissIntent.putExtra(Constants.EXTRA_ITEM_INFO, alarmInfo);
		dismissIntent.setAction(Constants.ACTION_DISMISS);
		PendingIntent dismissPI = PendingIntent.getService(this, 0, dismissIntent, PIFlags);

		Intent snoozeIntent = new Intent(dismissIntent);
		snoozeIntent.setAction(Constants.ACTION_SNOOZE);
		PendingIntent snoozePI = PendingIntent.getService(this, 0, snoozeIntent, PIFlags);

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
		notifView.setTextViewText(R.id.alarm_name_text, alarmInfo.item.getName());

		// we need this line to ensure actions pop up on the heads up notification
		notifView.setOnClickPendingIntent(R.id.snoozeButton, snoozePI);
		notifView.setOnClickPendingIntent(R.id.dismissButton, dismissPI);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(alarmInfo.item.getName())
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
		if (((Alarm)alarmInfo.item).getRingtoneUri() != null && ((Alarm)alarmInfo.item).getVolume() != 0) {

			// media player setup
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioAttributes(audioAttributes);
			mediaPlayer.setLooping(true);
			mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

			try {
				mediaPlayer.setDataSource(this, ((Alarm)alarmInfo.item).getRingtoneUri());
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

		if (((Alarm)alarmInfo.item).isVibrateOn()) {
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
	 * Binding is not supported for this class. Will throw an UnsupportedOperationException.
	 * @param intent the intent used to bind to the service, unused in this implementation
	 */
	@Override
	public IBinder onBind(@NotNull Intent intent) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Called when the service is being destroyed. Releases the media player if it's not null, and
	 * releases audio focus if necessary.
	 */
	@Override
	public void onDestroy() {
		exitService();
	}

	/**
	 * Closes everything. Takes care of the media player, audio focus, and vibrator. Doesn't stop
	 * the service itself, though.
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
		if (alarmInfo.item == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The ringing alarm was null somehow.");
			return;
		}

		float vol = ((Alarm)alarmInfo.item).getVolume() / 100f;
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
	 * Creates a notification channel if the API level requires it. Otherwise, does nothing.
	 * @param context the current context
	 */
	static void createNotificationChannel(@NotNull Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = context.getString(R.string.notif_channel_name);
			String description = context.getString(R.string.notif_channel_description);
			int importance = NotificationManager.IMPORTANCE_HIGH;

			NotificationChannel channel = new NotificationChannel(RingingService.NOTIFICATION_CHANNEL_ID, name, importance);
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
}
