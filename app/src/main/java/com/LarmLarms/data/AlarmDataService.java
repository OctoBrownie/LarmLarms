package com.larmlarms.data;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.larmlarms.BuildConfig;
import com.larmlarms.R;
import com.larmlarms.ringing.RingingService;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles Alarm and AlarmGroup data. Stores/retrieves it from disk and allows other processes to
 * access it. Messages with the same what field mean different things based on what sent it (either
 * AlarmDataService or clients of it)
 * <br/>
 * Inbound messages refers to messages coming in from clients, sent to the service. Outbound messages
 * refer to messages going out to clients, sent by the service.
 */
public class AlarmDataService extends Service {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "DataService";
	/**
	 * Name of the handler thread.
	 */
	private static final String HANDLER_THREAD_NAME = "AlarmDataHandler";
	/**
	 * The name of the file that stores the alarms. Found within private storage for the app.
	 */
	private static final String ALARM_STORE_FILE_NAME = "alarms.txt";

	/**
	 * In a bundle, this is the key used to store a ListableInfo.
	 */
	public static final String BUNDLE_INFO_KEY = "com.larmlarms.bundleKey.INFO";
	/**
	 * In a bundle, this is the key used to store a reduced folder structure (a String ArrayList).
	 */
	public static final String BUNDLE_LIST_KEY = "com.apps.larmlarms.bundleKey.FOLDERS";
	/**
	 * In a bundle, this is the key used to store the name of a Listable.
	 */
	public static final String BUNDLE_NAME_KEY = "com.apps.larmlarms.bundleKey.NAME";
	/**
	 * In a bundle, this is the key used to store the time of an Alarm.
	 */
	public static final String BUNDLE_TIME_KEY = "com.apps.larmlarms.bundleKey.TIME";

	/**
	 * Flag to set if the calling intent doesn't want the service to immediately update the alarm
	 * pending intent when first created. Used with a boolean extra in incoming intents.
	 */
	public static final String EXTRA_NO_UPDATE = "com.apps.larmlarms.extra.NO_UPDATE";

	/* *****************************  Message what field constants  **************************** */

	/**
	 * Inbound: The client is asking for the entire reduced string list for the dataset. The replyTo
	 * field should be filled with a messenger.
	 * <br/>
	 * Outbound: A response with the reduced string array list from the Service. Puts the list in a
	 * bundle accessible by getData() or peekData() using BUNDLE_LIST_KEY for the key.
	 */
	public static final int MSG_GET_FOLDERS = 1;

	/**
	 * Inbound: The client wants to set a Listable to a certain index. The absolute index of the
	 * Listable should be in the arg1 field and a ListableInfo in the data bundle (with
	 * BUNDLE_INFO_KEY for its key). Within the ListableInfo, there should be the new listable and
	 * its absolute index.
	 * <br/>
	 * Outbound: Notifies data change listeners that a listable has been set. Output messages are
	 * of the same form as input messages.
	 */
	public static final int MSG_SET_LISTABLE = 2;
	/**
	 * Inbound: The client wants to add a Listable at the specified index and parent. A ListableInfo
	 * should be in the data bundle (using BUNDLE_INFO_KEY for its key), with the new Listable and
	 * its specified path. May trigger MSG_DATA_FILLED messages to be sent.
	 * <br/>
	 * Outbound: Notifies data change listeners that a listable has been added. Output messages are
	 * of the same form as input messages.
	 */
	public static final int MSG_ADD_LISTABLE = 3;
	/**
	 * Inbound: The client wants to move a Listable to a new folder. arg1 should be filled with
	 * the old absolute index of the Listable. A ListableInfo should be in the data bundle (using 
	 * BUNDLE_INFO_KEY for its key). To identify the new position of the listable, the path of
	 * the parent folder must be in the ListableInfo's path field. Will assume that the new path is
	 * not inside the listable being moved (if it's an AlarmGroup). If the listable field is not null,
	 * the listable will be replaced with the new one.
	 * <br/>
	 * Outbound: Notifies data change listeners that a listable has been moved. Output messages are
	 * of the same form as input messages.
	 */
	public static final int MSG_MOVE_LISTABLE = 4;
	/**
	 * Inbound: The client wants to delete a Listable. The absolute index of the Listable should be
	 * in the arg1 field. May trigger MSG_DATA_EMPTIED messages to be sent.
	 * <br/>
	 * Outbound: Notifies data change listeners that a listable has been deleted. Output messages are
	 * of the same form as input messages.
	 */
	public static final int MSG_DELETE_LISTABLE = 5;

	/**
	 * Inbound: The client wants to toggle isActive on a Listable. The absolute index of the
	 * Listable should be in the arg1 field. May unsnooze the alarm if it was snoozed before and is
	 * being turned off.
	 * <br/>
	 * Outbound: Notifies data change listeners that a listable has been toggled active. Output
	 * messages are of the same form as input messages.
	 */
	public static final int MSG_TOGGLE_ACTIVE = 6;
	/**
	 * Inbound: The client wants to snooze an Alarm. The absolute index of the Alarm should be
	 * in the arg1 field.
	 * <br/>
	 * Outbound: Notifies data change listeners that a listable has been snoozed. Output messages are
	 * of the same form as input messages.
	 */
	public static final int MSG_SNOOZE_ALARM = 7;
	/**
	 * Inbound: The client wants to unsnooze an Alarm. The absolute index of the Alarm should be
	 * in the arg1 field.
	 * <br/>
	 * Outbound: Notifies data change listeners that a listable has been unsnoozed. Output messages
	 * are of the same form as input messages.
	 */
	public static final int MSG_UNSNOOZE_ALARM = 8;
	/**
	 * Inbound: The client wants to dismiss an Alarm. The absolute index of the Alarm should be
	 * in the arg1 field.
	 * <br/>
	 * Outbound: Notifies data change listeners that a listable has been dismissed. Output messages
	 * are of the same form as input messages.
	 */
	public static final int MSG_DISMISS_ALARM = 9;

	/**
	 * Inbound: A client wants to change its notification of data changes. In the replyTo field
	 * there should be a Messenger to either register or unregister as a data listener. Data
	 * listeners are sent messages when the data has changed based on what has changed. If the Messenger
	 * is already registered, the service will unregister it. When a new one is registered, will
	 * send a MSG_DATA_CHANGED message immediately to it.
	 * <br/>
	 * Outbound: Merely a confirmation that the messenger has been registered as a data changed listener.
	 * The listable count should be in the arg1 field.
	 */
	public static final int MSG_DATA_CHANGED = 10;

	/**
	 * Inbound: A client wants to change its notification of the data being empty or full. In the
	 * replyTo field there should be a Messenger to either register or unregister as a listener. Data
	 * listeners are sent MSG_DATA_EMPTIED messages when there are no more items in the rootFolder,
	 * and MSG_DATA_FILLED when a new listable has been added to an empty rootFolder. If the Messenger
	 * is already registered, the service will unregister it. Right when registered, will send the
	 * listener either MSG_DATA_EMPTIED or MSG_DATA_FILLED.
	 * <br/>
	 * Outbound: N/A
	 */
	public static final int MSG_DATA_EMPTY_LISTENER = 11;
	/**
	 * Inbound: N/A
	 * <br/>
	 * Outbound: Means that data has been deleted and there are no more listables within the rootFolder
	 * anymore. No guarantees can be made about message fields. Only sent to registered empty
	 * listeners.
	 */
	public static final int MSG_DATA_EMPTIED = 12;
	/**
	 * Inbound: N/A
	 * <br/>
	 * Outbound: Means that data has been added and there are now listables within the rootFolder where
	 * it was empty before. No guarantees can be made about message fields. Only sent to registered
	 * empty listeners.
	 */
	public static final int MSG_DATA_FILLED = 13;

	/**
	 * Inbound: A client wants to change its notification of the next alarm. In the replyTo field
	 * there should be a Messenger to either register or unregister as a data listener. Data
	 * listeners are sent MSG_NEXT_ALARM messages whenever the data has changed, even if the next
	 * alarm hasn't. If the Messenger is already registered, the service will unregister it. When a
	 * new one is registered, will send a MSG_NEXT_ALARM message immediately to it.
	 * <br/>
	 * Outbound: Shows the next alarm, which might have changed. The next alarm's time is in the
	 * bundle under BUNDLE_TIME_KEY and its name is as well under BUNDLE_NAME_KEY. If there is no
	 * alarm to ring, the bundle will be null.
	 */
	public static final int MSG_NEXT_ALARM = 14;

	/* *************************************  Instance Fields  ********************************** */

	/**
	 * The thread that the data handler lives in, cannot be null.
	 */
	@NotNull
	private final HandlerThread handlerThread;

	/**
	 * List of registered listeners for data change events. Data change events include adding,
	 * changing, moving, or removing alarms from the list. It also includes some method calls on
	 * listables, such as toggling open/closed a folder or snoozing alarms. Cannot be null.
	 */
	@NotNull
	private final List<Messenger> dataChangeListeners;

	/**
	 * List of registered empty listeners. Listeners are sent MSG_DATA_FILLED and MSG_DATA_EMPTIED
	 * messages when the data has either completely emptied or has just filled up from being empty.
	 * Cannot be null.
	 */
	@NotNull
	private final List<Messenger> emptyListeners;

	/**
	 * List of registered listeners for next alarm events. Next alarm events are sent when adding,
	 * changing, moving, or removing alarms from the list. It also includes some method calls on
	 * listables, such as toggling open/closed a folder or snoozing alarms. Cannot be null.
	 */
	@NotNull
	private final List<Messenger> nextAlarmListeners;

	/**
	 * The folder holding the entire dataset. Cannot be null but before the service is bound it will
	 * be an empty folder.
	 */
	@NotNull
	private AlarmGroup rootFolder;

	/**
	 * The next alarm to ring. Null if there's no next alarm to ring.
	 */
	@Nullable
	private Alarm nextAlarm;

	/* **********************************  Lifecycle Methods  ********************************** */

	/**
	 * Initializes AlarmDataService. Since the context hasn't been created yet, initializes some
	 * fields to dummy data.
	 */
	public AlarmDataService() {
		rootFolder = new AlarmGroup();
		dataChangeListeners = new ArrayList<>();
		emptyListeners = new ArrayList<>();
		nextAlarmListeners = new ArrayList<>();
		handlerThread = new HandlerThread(HANDLER_THREAD_NAME);
	}

	/**
	 * Since the context is valid, fetches alarms from disk, checks for notification channels, and
	 * sets the next alarm to ring. Starts the handler thread and registers a new messenger to
	 * handle messages within that thread.
	 * @param intent intent delivered from the first call to Context.bindService()
	 * @return an IBinder for a Messenger
	 */
	@Override @NotNull
	public IBinder onBind(@NotNull Intent intent) {
		rootFolder = new AlarmGroup(getResources().getString(R.string.root_folder), getAlarmsFromDisk(this));

		createNotificationChannel(this);
		if (!intent.getBooleanExtra(EXTRA_NO_UPDATE, false))
			nextAlarm = setNextAlarmToRing(this, rootFolder);

		handlerThread.start();
		Messenger messenger = new Messenger(new MsgHandler(this, handlerThread));
		return messenger.getBinder();
	}

	/**
	 * Called when the service is no longer needed. Kills handlerThread after it has dealt with all
	 * incoming Messages.
	 */
	@Override
	public void onDestroy() {
		handlerThread.quitSafely();
	}

	/* ********************************  Disk Read/Write Methods  ******************************* */
	/**
	 * Initializes alarm data from file.
	 * @param context The context to get file streams from. This value may not be null.
	 * @return A populated ArrayList of Listables or an empty one in the case of an error
	 */
	@NotNull
	public static ArrayList<Item> getAlarmsFromDisk(@NotNull Context context) {
		ArrayList<Item> data = new ArrayList<>();

		try {
			FileInputStream is = context.openFileInput(ALARM_STORE_FILE_NAME);
			InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
			BufferedReader bReader = new BufferedReader(isr);

			String currLine = bReader.readLine();
			StringBuilder currFolder = null;
			Item currItem;

			while (currLine != null) {
				// end of the current folder
				if (!currLine.startsWith("\t") && currFolder != null) {
					currItem = AlarmGroup.fromStoreString(context, currFolder.toString());
					if (currItem != null) { data.add(currItem); }
					currFolder = null;
				}

				// a top-level alarm
				if (currLine.startsWith("a")) {
					currItem = Alarm.fromStoreString(context, currLine);
					if (currItem != null) { data.add(currItem); }
				}
				// start of a top-level folder
				else if (currLine.startsWith("f")) {
					currFolder = new StringBuilder(currLine);
				}
				// part of a top-level folder
				else if (currLine.startsWith("\t") && currFolder != null) {
					currFolder.append('\n').append(currLine);
				}
				else {
					if (BuildConfig.DEBUG) Log.e(TAG, "Invalid line in alarms.txt: " + currLine);
					return new ArrayList<>();
				}
				currLine = bReader.readLine();
			}
			// ends the folder in case there wasn't another alarm after it to close within the loop
			if (currFolder != null) {
				currItem = AlarmGroup.fromStoreString(context, currFolder.toString());
				if (currItem != null) { data.add(currItem); }
			}

			is.close();
		}
		catch (IOException e) {
			if (BuildConfig.DEBUG) Log.e(TAG, e.getMessage());
			return new ArrayList<>();
		}

		if (BuildConfig.DEBUG) Log.i(TAG, "Alarm list retrieved successfully.");
		return data;
	}

	/**
	 * Writes all alarms in myAdapter to app-specific file storage, with file name ALARM_STORE_FILE_NAME
	 * @param context The context to get file streams from. This value may not be null.
	 * @param data The data to write, doesn't include the AlarmGroup itself (uses getListables() to
	 *             retrieve Listables to write). This value may not be null.
	 */
	private static void writeAlarmsToDisk(@NotNull Context context, @NotNull AlarmGroup data) {
		try {
			File alarmFile = new File(context.getFilesDir(), ALARM_STORE_FILE_NAME);
			//noinspection ResultOfMethodCallIgnored
			alarmFile.createNewFile();

			FileOutputStream os = context.openFileOutput(ALARM_STORE_FILE_NAME, Context.MODE_PRIVATE);

			StringBuilder builder = new StringBuilder();
			for (Item l : data.getListables()) {
				builder.append(l.toStoreString()).append('\n');
			}
			// delete the last '\n'
			if (builder.length() != 0) builder.deleteCharAt(builder.length() - 1);

			os.write(builder.toString().getBytes());
			os.close();
		}
		catch (IOException e) {
			if (BuildConfig.DEBUG) Log.e(TAG, e.getMessage());
		}
	}

	/* ********************************  Handle Message Methods  ******************************** */

	/**
	 * Responds to an inbound MSG_GET_LISTABLE message. Using the replyTo field from the message,
	 * sends the requested Listable to that Messenger, or if null sends it to the Handler specified
	 * by the getTarget() method.
	 * @see #MSG_GET_FOLDERS
	 * @param inMsg the inbound MSG_GET_LISTABLE message
	 */
	private void handleGetFolders(@NotNull Message inMsg) {
		if (inMsg.replyTo == null && inMsg.getTarget() == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_GET_FOLDERS: Message had a null reply to field and no target.");
			return;
		}

		// get listable with abs index in arg1
		ArrayList<String> list = rootFolder.toPathList();
		Message outMsg = Message.obtain(null, MSG_GET_FOLDERS);
		Bundle bundle = new Bundle();
		bundle.putStringArrayList(BUNDLE_LIST_KEY, list);
		outMsg.setData(bundle);
		outMsg.arg1 = inMsg.arg1;

		if (inMsg.replyTo != null) {
			try {
				inMsg.replyTo.send(outMsg);
			}
			catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		else {
			inMsg.getTarget().dispatchMessage(outMsg);
		}
	}

	/**
	 * Responds to an inbound MSG_SET_LISTABLE message. Sets the listable with absolute index arg1
	 * to the new listable in the data bundle.
	 * @see #MSG_SET_LISTABLE
	 * @param inMsg the inbound MSG_SET_LISTABLE message
	 */
	private void handleSetListable(@NotNull Message inMsg) {
		if (inMsg.getData() == null) return;

		ItemInfo info = inMsg.getData().getParcelable(BUNDLE_INFO_KEY);
		if (info == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_SET_LISTABLE: ListableInfo was null.");
			return;
		}
		if (info.item == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_SET_LISTABLE: Listable was null.");
			return;
		}

		Item removed = rootFolder.setListableAbs(info.absIndex, info.item);
		if (removed == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_SET_LISTABLE: There was no listable to set.");
			return;
		}

		// transfer listables if possible/necessary
		if (removed instanceof AlarmGroup && info.item instanceof AlarmGroup) {
			((AlarmGroup) info.item).setListables(((AlarmGroup) removed).getListables());
		}

		save();

		Message outMsg = Message.obtain(inMsg);

		Bundle b = new Bundle();
		b.putParcelable(BUNDLE_INFO_KEY, info);
		outMsg.setData(b);

		sendDataChanged(outMsg);
	}

	/**
	 * Responds to an inbound MSG_ADD_LISTABLE message. Adds the listable where based on path given.
	 * @see #MSG_ADD_LISTABLE
	 * @param inMsg the inbound MSG_ADD_LISTABLE message
	 */
	private void handleAddListable(@NotNull Message inMsg) {
		ItemInfo info = inMsg.getData().getParcelable(BUNDLE_INFO_KEY);
		if (info == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_ADD_LISTABLE: ListableInfo was null.");
			return;
		}
		if (info.item == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_ADD_LISTABLE: Listable was not specified.");
			return;
		}
		if (info.path == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_ADD_LISTABLE: Path was not specified.");
			return;
		}

		rootFolder.addItem(info.item, info.path);
		save();

		Message outMsg = Message.obtain(null, MSG_ADD_LISTABLE);
		Bundle b = new Bundle();

		b.putParcelable(BUNDLE_INFO_KEY, info);
		outMsg.setData(b);

		sendDataChanged(outMsg);

		// just added the first new Listable
		if (rootFolder.size() == 2) {
			sendDataFilled();
		}
	}

	/**
	 * Responds to an inbound MSG_MOVE_LISTABLE message. Moves a listable to a different folder.
	 * @see #MSG_MOVE_LISTABLE
	 */
	private void handleMoveListable(@NotNull Message inMsg) {
		ItemInfo info = inMsg.getData().getParcelable(BUNDLE_INFO_KEY);
		if (info == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_MOVE_LISTABLE: Listable was null.");
			return;
		}

		if (info.path == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_MOVE_LISTABLE: Path was not specified.");
			return;
		}

		// rootFolder.moveListableAbs(info.item, info.path, inMsg.arg1);
		save();
		
		Bundle b = new Bundle();
		b.putParcelable(BUNDLE_INFO_KEY, info);
		
		Message outMsg = Message.obtain(inMsg);
		outMsg.setData(b);
		sendDataChanged(outMsg);
	}

	/**
	 * Responds to an inbound MSG_DELETE_LISTABLE message. Deletes the listable at the absolute index
	 * specified by inMsg.arg1
	 * @see #MSG_DELETE_LISTABLE
	 * @param inMsg the inbound MSG_DELETE_LISTABLE message
	 */
	private void handleDeleteListable(@NotNull Message inMsg) {
		rootFolder.deleteListableAbs(inMsg.arg1);
		save();
		sendDataChanged(Message.obtain(inMsg));

		// just deleted the last Listable
		if (rootFolder.size() == 1) sendDataEmptied();
	}

	/**
	 * Responds to an inbound MSG_TOGGLE_ACTIVE message. Toggles the active state of the listable at
	 * the absolute index specified by arg1. Will also unsnooze the alarm if it was previously snoozed.
	 * @see #MSG_TOGGLE_ACTIVE
	 * @param inMsg the inbound MSG_TOGGLE_ACTIVE message
	 */
	private void handleToggleActive(@NotNull Message inMsg) {
		Item l = rootFolder.getListableAbs(inMsg.arg1, true);
		if (l == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_TOGGLE_ACTIVE: Index of listable was out of bounds.");
			return;
		}
		l.toggleActive();
		if (l instanceof Alarm) ((Alarm) l).updateRingTime();
		save();
		sendDataChanged(Message.obtain(inMsg));
	}

	/**
	 * Responds to an inbound MSG_SNOOZE_ALARM message. Snoozes the alarm specified by the absolute
	 * index specified by arg1.
	 * @param inMsg the inbound MSG_SNOOZE_ALARM message
	 */
	private void handleSnoozeAlarm(@NotNull Message inMsg) {
		Item l = rootFolder.getListableAbs(inMsg.arg1, false);
		if (l == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_SNOOZE_ALARM: Listable index was out of bounds.");
			return;
		}
		else if (!(l instanceof Alarm)) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_SNOOZE_ALARM: Listable was a folder.");
			return;
		}

		((Alarm) l).snooze();
		save();
		sendDataChanged(Message.obtain(inMsg));
	}

	/**
	 * Responds to an inbound MSG_UNSNOOZE_ALARM message. Unsnoozes the alarm specified by the
	 * absolute index specified by arg1.
	 * @see #MSG_UNSNOOZE_ALARM
	 * @param inMsg the inbound MSG_UNSNOOZE_ALARM message
	 */
	private void handleUnsnoozeAlarm(@NotNull Message inMsg) {
		Item l = rootFolder.getListableAbs(inMsg.arg1, false);
		if (l == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_UNSNOOZE_ALARM: Listable index was out of bounds.");
			return;
		}
		else if (!(l instanceof Alarm)) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_UNSNOOZE_ALARM: Listable was a folder.");
			return;
		}

		((Alarm) l).unsnooze();
		save();
		sendDataChanged(Message.obtain(inMsg));
	}

	/**
	 * Responds to an inbound MSG_DISMISS_ALARM message. Dismisses the alarm specified by the
	 * absolute index specified by arg1.
	 * @see #MSG_DISMISS_ALARM
	 * @param inMsg the inbound MSG_DISMISS_ALARM message
	 */
	private void handleDismissAlarm(@NotNull Message inMsg) {
		Item l = rootFolder.getListableAbs(inMsg.arg1, false);
		if (l == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_DISMISS_ALARM: Listable index was out of bounds.");
			return;
		}
		else if (!(l instanceof Alarm)) {
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_DISMISS_ALARM: Listable was a folder.");
			return;
		}

		((Alarm) l).dismiss();
		save();
		sendDataChanged(Message.obtain(inMsg));
	}

	/**
	 * Responds to an inbound MSG_DATA_CHANGED message. Using the replyTo field from the message,
	 * either registers it (if not registered) as a data listener or unregisters it (if already
	 * registered).
	 * @see #MSG_DATA_CHANGED
	 * @param inMsg the inbound MSG_DATA_CHANGED message
	 */
	private void handleDataChanged(@NotNull Message inMsg) {
		if (inMsg.replyTo == null) {
			// invalid message
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_DATA_CHANGED: Message didn't have a Messenger to reply to.");
			return;
		}

		int index = dataChangeListeners.indexOf(inMsg.replyTo);
		if (index == -1) {
			dataChangeListeners.add(inMsg.replyTo);
			Message outMsg = Message.obtain(null, MSG_DATA_CHANGED);
			outMsg.arg1 = rootFolder.size() - 1;
			sendDataChanged(outMsg);
		}
		else dataChangeListeners.remove(index);
	}

	/**
	 * Responds to an inbound MSG_DATA_EMPTY_LISTENER message. Using the replyTo field, either
	 * registers it as a new empty listener (if not previously registered) or unregisters it as a
	 * listener (if previously registered).
	 * @param inMsg the inbound MSG_DATA_EMPTY_LISTENER message
	 */
	private void handleDataEmpty(@NotNull Message inMsg) {
		if (inMsg.replyTo == null) {
			// invalid message
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_DATA_EMPTY_LISTENER: Message didn't have a Messenger to reply to.");
			return;
		}

		int index = emptyListeners.indexOf(inMsg.replyTo);
		if (index == -1) {
			// register the empty listener
			emptyListeners.add(inMsg.replyTo);

			// send a DATA_EMPTIED or DATA_FILLED to the new listener
			if (rootFolder.size() == 1)
				sendDataEmptied(inMsg.replyTo);
			else
				sendDataFilled(inMsg.replyTo);
		}
		else {
			// unregister the empty listener
			emptyListeners.remove(index);
		}
	}

	/**
	 * Responds to an inbound MSG_NEXT_ALARM message. Using the replyTo field from the message,
	 * either registers it (if not registered) as a listener or unregisters it (if already
	 * registered).
	 * @param inMsg the inbound MSG_DATA_CHANGED message
	 */
	private void handleNextAlarm(@NotNull Message inMsg) {
		if (inMsg.replyTo == null) {
			// invalid message
			if (BuildConfig.DEBUG) Log.e(TAG, "MSG_NEXT_ALARM: Message didn't have a Messenger to reply to.");
			return;
		}

		int index = nextAlarmListeners.indexOf(inMsg.replyTo);
		if (index == -1) {
			nextAlarmListeners.add(inMsg.replyTo);
			sendNextAlarm(inMsg.replyTo);
		}
		else nextAlarmListeners.remove(index);
	}

	/* ********************************  Send Message Methods  ******************************** */

	/**
	 * Sends a message to all registered data change listeners.
	 * @param msg The message to send (should already be filled out). Recycles the message when done.
	 */
	private void sendDataChanged(@NotNull Message msg) {
		for (Messenger m : dataChangeListeners) {
			sendMessage(m, msg);
		}
		msg.recycle();
	}

	/**
	 * Send a MSG_DATA_EMPTIED message to all registered empty listeners.
	 */
	private void sendDataEmptied() {
		for (Messenger m : emptyListeners) {
			sendDataEmptied(m);
		}
	}
	/**
	 * Send a MSG_DATA_EMPTIED message to one specific messenger.
	 * @param m the messenger to send the message to
	 */
	private void sendDataEmptied(@NotNull Messenger m) {
		Message outMsg = Message.obtain(null, MSG_DATA_EMPTIED);
		try {
			m.send(outMsg);
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send a MSG_DATA_FILLED message to all registered empty listeners.
	 */
	private void sendDataFilled() {
		for (Messenger m : emptyListeners) {
			sendDataFilled(m);
		}
	}
	/**
	 * Send a MSG_DATA_FILLED message to one specific messenger.
	 * @param m the messenger to send the message to
	 */
	private void sendDataFilled(@NotNull Messenger m) {
		Message outMsg = Message.obtain(null, MSG_DATA_FILLED);
		try {
			m.send(outMsg);
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends MSG_NEXT_ALARM messages to all registered next alarm listeners. The message should
	 * have the time the alarm will ring in bundle under BUNDLE_TIME_KEY and the name of the alarm
	 * in the bundle under BUNDLE_NAME_KEY.
	 */
	private void sendNextAlarm() {
		for (Messenger m : nextAlarmListeners) {
			sendNextAlarm(m);
		}
	}
	/**
	 * Sends MSG_NEXT_ALARM messages to one specific messenger. The message should have the time the
	 * alarm will ring in bundle under BUNDLE_TIME_KEY and the name of the alarm in the bundle under
	 * BUNDLE_NAME_KEY.
	 * @param m the messenger to send the message to
	 */
	private void sendNextAlarm(@NotNull Messenger m) {
		Message outMsg = Message.obtain(null, MSG_NEXT_ALARM);

		Bundle b = null;
		if (nextAlarm != null) {
			b = new Bundle();
			b.putLong(BUNDLE_TIME_KEY, nextAlarm.getAlarmTimeMillis());
			b.putString(BUNDLE_NAME_KEY, nextAlarm.getName());
		}
		outMsg.setData(b);

		try {
			m.send(outMsg);
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets the next alarm to ring. Does not create a new pending intent, rather updates the current
	 * one. Tells AlarmManager to wake up and call AlarmRingingService. Sends MSG_NEXT_ALARM if
	 * necessary.
	 * @param context the current context
	 * @param rootFolder the folder to look in for the alarms
	 */
	static Alarm setNextAlarmToRing(@NotNull Context context, @NotNull AlarmGroup rootFolder) {
		ItemInfo next = getNextRingingAlarm(rootFolder.getListables());

		Intent intent = new Intent(context, RingingService.class);
		if (next.item != null) {
			intent.putExtra(RingingService.EXTRA_ITEM, next.item.toEditString());
			intent.putExtra(RingingService.EXTRA_ITEM_PATH, next.absIndex);
		}

		AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pendingIntent;

		// flags for the pending intent
		int PIFlags = PendingIntent.FLAG_UPDATE_CURRENT;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
			PIFlags = PIFlags | PendingIntent.FLAG_MUTABLE;

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			pendingIntent = PendingIntent.getForegroundService(context, 0, intent, PIFlags);
		}
		else {
			pendingIntent = PendingIntent.getService(context, 0, intent, PIFlags);
		}

		if (manager == null || pendingIntent == null) {
			if (BuildConfig.DEBUG) Log.i(TAG, "Couldn't reach alarm manager or the service to get" +
					"the pending intent.");
			return (Alarm) next.item;
		}

		if (next.item == null) {
			if (BuildConfig.DEBUG) Log.i(TAG, "No next listable to register to ring.");
			manager.cancel(pendingIntent);
		}
		else {
			if (BuildConfig.DEBUG) Log.i(TAG, "Sent an intent to AlarmManager.");
			manager.setAlarmClock(
					new AlarmManager.AlarmClockInfo(((Alarm) next.item).getAlarmTimeMillis(),
							pendingIntent),
					pendingIntent);
		}
		return (Alarm) next.item;
	}

	/* *************************************  Other Methods  *********************************** */

	/**
	 * Sends a message to one specific messenger.
	 * @param m the messenger to send the message to
	 * @param msg The message to send (should already be filled out). A copy of the message is
	 *               actually sent (the original can be reused)
	 */
	private void sendMessage(@NotNull Messenger m, @NotNull Message msg) {
		Message outMsg = Message.obtain(msg);
		try {
			m.send(outMsg);
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Searches for the next Alarm that will ring. Returns the listable and absolute index of the
	 * listable (within the current dataset) within a ListableInfo struct.
	 * @param data the dataset to look through, cannot be null
	 * @return a ListableInfo with alarm and absolute index (real index) filled correctly, alarm can
	 * be null if there is no active alarm within the data given
	 */
	@NotNull
	private static ItemInfo getNextRingingAlarm(@NotNull ArrayList<Item> data) {
		ItemInfo nextAlarm = new ItemInfo();
		Item l;				// represents the current listable being searched
		int absIndex = 0;		// represents the absolute index currently being searched (real index)

		for (int i = 0; i < data.size(); i++) {
			l = data.get(i);

			if (!l.isActive()) {
				absIndex += l.size();
				continue;
			}

			if (l instanceof Alarm) {
				((Alarm) l).updateRingTime();

				// check whether it could be the next listable
				if (nextAlarm.item == null || ((Alarm) l).getAlarmTimeMillis() <
						((Alarm) nextAlarm.item).getAlarmTimeMillis()) {
					nextAlarm.item = l;
					nextAlarm.absIndex = absIndex;
				}
				absIndex++;
			}
			else {
				ItemInfo possible = getNextRingingAlarm(((AlarmGroup) l).getListables());
				// there is no candidate in this folder
				if (possible.item == null) {
					absIndex += l.size();
					continue;
				}
				// we had no candidate before or this candidate is better
				if (nextAlarm.item == null || ((Alarm) possible.item).getAlarmTimeMillis() <
						((Alarm) nextAlarm.item).getAlarmTimeMillis()) {
					nextAlarm.item = possible.item;
					nextAlarm.absIndex = absIndex + possible.absIndex + 1;
				}
				absIndex += l.size();
			}
		}
		return nextAlarm;
	}

	/**
	 * Saves everything to disk and sets the alarms to ring.
	 */
	private void save() {
		writeAlarmsToDisk(this, rootFolder);
		nextAlarm = setNextAlarmToRing(this, rootFolder);
		sendNextAlarm();
	}

	/* ***********************************  Inner Classes  ************************************* */

	/**
	 * Inner Handler class for handling messages from Messengers.
	 */
	private static class MsgHandler extends Handler {
		/**
		 * The service that created the handler. Gives access to the handle methods in the service.
		 */
		@NotNull
		AlarmDataService service;

		/**
		 * Creates a new handler with a specified service and thread.
		 * @param service the service that created the handler
		 * @param thread the handler thread to run on
		 */
		private MsgHandler(@NotNull AlarmDataService service, @NotNull HandlerThread thread) {
			super(thread.getLooper());
			this.service = service;
		}

		/**
		 * Handles messages based on their field what. Sends it off to the service's appropriate
		 * handling method and logs the event.
		 * @param msg the incoming Message to handle
		 */
		@Override
		public void handleMessage(@Nullable Message msg) {
			if (msg == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Message sent to the data service was null. Ignoring...");
				return;
			}

			switch(msg.what) {
				case MSG_GET_FOLDERS:
					service.handleGetFolders(msg);
					if (BuildConfig.DEBUG) Log.d(TAG, "Got folder structure for a client.");
					break;
				case MSG_SET_LISTABLE:
					service.handleSetListable(msg);
					if (BuildConfig.DEBUG) Log.d(TAG, "Set a listable to a specific index.");
					break;
				case MSG_ADD_LISTABLE:
					service.handleAddListable(msg);
					if (BuildConfig.DEBUG) Log.d(TAG, "Added a listable.");
					break;
				case MSG_MOVE_LISTABLE:
					service.handleMoveListable(msg);
					if (BuildConfig.DEBUG) Log.d(TAG, "Moved a listable.");
					break;
				case MSG_DELETE_LISTABLE:
					service.handleDeleteListable(msg);
					if (BuildConfig.DEBUG) Log.d(TAG, "Deleted a listable.");
					break;
				case MSG_TOGGLE_ACTIVE:
					service.handleToggleActive(msg);
					if (BuildConfig.DEBUG) Log.d(TAG, "Toggled a listable's active state.");
					break;
				case MSG_SNOOZE_ALARM:
					service.handleSnoozeAlarm(msg);
					if (BuildConfig.DEBUG) Log.d(TAG, "Snoozed an alarm.");
					break;
				case MSG_UNSNOOZE_ALARM:
					service.handleUnsnoozeAlarm(msg);
					if (BuildConfig.DEBUG) Log.d(TAG, "Unsnoozed an alarm.");
					break;
				case MSG_DISMISS_ALARM:
					service.handleDismissAlarm(msg);
					if (BuildConfig.DEBUG) Log.d(TAG, "Dismissed an alarm.");
					break;
				case MSG_DATA_CHANGED:
					service.handleDataChanged(msg);
					if (BuildConfig.DEBUG) Log.d(TAG, "Added or removed a data changed listener.");
					break;
				case MSG_DATA_EMPTY_LISTENER:
					service.handleDataEmpty(msg);
					if (BuildConfig.DEBUG) Log.d(TAG, "Added or removed a data empty listener.");
					break;
				case MSG_NEXT_ALARM:
					service.handleNextAlarm(msg);
					if (BuildConfig.DEBUG) Log.d(TAG, "Added or removed a next alarm listener.");
					break;
				default:
					if (BuildConfig.DEBUG) Log.e(TAG, "Unknown message type.");
					super.handleMessage(msg);
					break;
			}
		}
	}
}
