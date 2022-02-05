package com.apps.LarmLarms;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

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
	 * Debug constant, enables/disables log messages.
	 */
	private final static boolean DEBUG = true;

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
	static final String BUNDLE_INFO_KEY = "com.apps.LarmLarms.bundleKey.INFO";
	/**
	 * In a bundle, this is the key used to store a reduced folder structure (a String ArrayList).
	 */
	static final String BUNDLE_LIST_KEY = "com.apps.LarmLarms.bundleKey.FOLDERS";

	/* *****************************  Message what field constants  **************************** */

	/**
	 * Inbound: the client is asking for a specific Listable. The specified Listable's absolute
	 * index should be in the arg1 field, and the Messenger to reply to should be in the replyTo
	 * field. Can also take a Handler coming from the getTarget() method, but a Messenger is
	 * preferred and will be used instead if present.
	 * <br/>
	 * Outbound: a response with a ListableInfo from the Service. Puts the ListableInfo in a bundle
	 * accessible by getData() or peekData() using BUNDLE_INFO_KEY for the key. Puts the absolute
	 * index of the listable in field arg1.
	 */
	static final int MSG_GET_LISTABLE = 0;
	/**
	 * Inbound: the client is asking for the entire reduced string list for the dataset. The replyTo
	 * field should be filled with a messenger.
	 * <br/>
	 * Outbound: a response with the reduced string array list from the Service. Puts the list in a
	 * bundle accessible by getData() or peekData() using BUNDLE_LIST_KEY for the key.
	 */
	static final int MSG_GET_FOLDERS = 1;

	/**
	 * Inbound: the client wants to set a Listable to a certain index. The absolute index of the
	 * Listable should be in the arg1 field and a ListableInfo in the data bundle (with
	 * BUNDLE_INFO_KEY for its key). Within the ListableInfo, there should be the new listable in 
	 * the listable field. Triggers MSG_DATA_CHANGED messages to be sent.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_SET_LISTABLE = 2;
	/**
	 * Inbound: the client wants to add a Listable at the specified index and parent. A ListableInfo
	 * should be in the data bundle (using BUNDLE_INFO_KEY for its key), with the new Listable in the
	 * Listable field, the absolute index of the new parent in absParentIndex, and the new absolute
	 * index in absIndex. Triggers MSG_DATA_CHANGED messages and may trigger MSG_DATA_FILLED messages
	 * to be sent.
	 * TODO: Figure out what information is needed for adding Listables anywhere in a list
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_ADD_LISTABLE = 3;
	/**
	 * Inbound: the client wants to move a Listable to a new index. arg1 should always be filled with 
	 * the old absolute index of the Listable. A ListableInfo should be in the data bundle (using 
	 * BUNDLE_INFO_KEY for its key). To identify the new position of the listable, the path of
	 * the parent folder must be in the ListableInfo's path field. Will assume that the new path is
	 * not inside the listable being moved (if it's an AlarmGroup). If the listable field is not null,
	 * the listable will be replaced with the new one. Triggers MSG_DATA_CHANGED messages to be sent.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_MOVE_LISTABLE = 4;
	/**
	 * Inbound: the client wants to delete a Listable. The absolute index of the Listable should be
	 * in the arg1 field. Triggers MSG_DATA_CHANGED and may trigger MSG_DATA_EMPTIED messages to be
	 * sent.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_DELETE_LISTABLE = 5;

	/**
	 * Inbound: the client wants to toggle isActive on a Listable. The absolute index of the
	 * Listable should be in the arg1 field. Does NOT trigger MSG_DATA_CHANGED messages to be sent.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_TOGGLE_ACTIVE = 6;
	/**
	 * Inbound: the client wants to toggle an AlarmGroup open/closed. The absolute index of the
	 * AlarmGroup should be in the arg1 field. Triggers MSG_DATA_CHANGED messages to be sent.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_TOGGLE_OPEN_FOLDER = 7;
	/**
	 * Inbound: the client wants to snooze an Alarm. The absolute index of the Alarm should be
	 * in the arg1 field. Triggers MSG_DATA_CHANGED messages to be sent.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_SNOOZE_ALARM = 8;
	/**
	 * Inbound: the client wants to unsnooze an Alarm. The absolute index of the Alarm should be
	 * in the arg1 field. Triggers MSG_DATA_CHANGED messages to be sent.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_UNSNOOZE_ALARM = 9;
	/**
	 * Inbound: the client wants to dismiss an Alarm. The absolute index of the Alarm should be
	 * in the arg1 field. Triggers MSG_DATA_CHANGED messages to be sent.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_DISMISS_ALARM = 10;

	/**
	 * Inbound: A client wants to change its notification of data changes. In the replyTo field
	 * there should be a Messenger to either register or unregister as a data listener. Data
	 * listeners are sent MSG_DATA_CHANGED messages when the data has changed. If the Messenger
	 * is already registered, the service will unregister it. When a new one is registered, will
	 * send a MSG_DATA_CHANGED message immediately to it.
	 * <br/>
	 * Outbound: Means that the data has been changed, only sent to registered listeners. Listable
	 * count should be in the arg1 field. Means that the new data has been written to the alarm store
	 * and can be queried from the service or read directly from disk.
	 */
	static final int MSG_DATA_CHANGED = 11;

	/**
	 * Inbound: A client wants to change its notification of the data being empty or full. In the
	 * replyTo field there should be a Messenger to either register or unregister as a listener. Data
	 * listeners are sent MSG_DATA_EMPTIED messages when there are no more listables in the rootFolder,
	 * and MSG_DATA_FILLED when a new listable has been added to an empty rootFolder. If the Messenger
	 * is already registered, the service will unregister it. Right when registered, will send the
	 * listener either MSG_DATA_EMPTIED or MSG_DATA_FILLED.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_DATA_EMPTY_LISTENER = 12;
	/**
	 * Inbound: N/A
	 * <br/>
	 * Outbound: Means that data has been deleted and there are no more listables within the rootFolder
	 * anymore. No guarantees can be made about message fields. Only sent to registered empty
	 * listeners.
	 */
	static final int MSG_DATA_EMPTIED = 13;
	/**
	 * Inbound: N/A
	 * <br/>
	 * Outbound: Means that data has been added and there are now listables within the rootFolder where
	 * it was empty before. No guarantees can be made about message fields. Only sent to registered
	 * empty listeners.
	 */
	static final int MSG_DATA_FILLED = 14;

	/* *************************************  Instance Fields  ********************************** */

	/**
	 * The thread that the data handler lives in, cannot be null.
	 */
	@NotNull
	private HandlerThread handlerThread;

	/**
	 * List of registered listeners for data change events. Data change events include adding,
	 * changing, moving, or removing alarms from the list. It also includes some method calls on
	 * listables, such as toggling open/closed a folder or snoozing alarms. Cannot be null.
	 */
	@NotNull
	private List<Messenger> dataChangeListeners;

	/**
	 * List of registered empty listeners. Listeners are sent MSG_DATA_FILLED and MSG_DATA_EMPTIED
	 * messages when the data has either completely emptied or has just filled up from being empty.
	 * Cannot be null.
	 */
	@NotNull
	private List<Messenger> emptyListeners;

	/**
	 * The folder holding the entire dataset. Cannot be null but before the service is bound it will
	 * be an empty folder.
	 */
	@NotNull
	private AlarmGroup rootFolder;

	/* **********************************  Lifecycle Methods  ********************************** */

	/**
	 * Initializes AlarmDataService. Since the context hasn't been created yet, initializes some
	 * fields to dummy data.
	 */
	public AlarmDataService() {
		rootFolder = new AlarmGroup();
		dataChangeListeners = new ArrayList<>();
		emptyListeners = new ArrayList<>();
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

		createNotificationChannel();
		setNextAlarmToRing();

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
	private static ArrayList<Listable> getAlarmsFromDisk(@NotNull Context context) {
		ArrayList<Listable> data = new ArrayList<>();

		try {
			FileInputStream is = context.openFileInput(ALARM_STORE_FILE_NAME);
			InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
			BufferedReader bReader = new BufferedReader(isr);

			String currLine = bReader.readLine();
			StringBuilder currFolder = null;
			Listable currListable;

			while (currLine != null) {
				// end of the current folder
				if (!currLine.startsWith("\t") && currFolder != null) {
					currListable = AlarmGroup.fromStoreString(context, currFolder.toString());
					if (currListable != null) { data.add(currListable); }
					currFolder = null;
				}

				// a top-level alarm
				if (currLine.startsWith("a")) {
					currListable = Alarm.fromStoreString(context, currLine);
					if (currListable != null) { data.add(currListable); }
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
					if (DEBUG) Log.e(TAG, "Invalid line in alarms.txt: " + currLine);
					return new ArrayList<>();
				}
				currLine = bReader.readLine();
			}
			// ends the folder in case there wasn't another alarm after it to close within the loop
			if (currFolder != null) {
				currListable = AlarmGroup.fromStoreString(context, currFolder.toString());
				if (currListable != null) { data.add(currListable); }
			}

			is.close();
		}
		catch (IOException e) {
			if (DEBUG) Log.e(TAG, e.getMessage());
			return new ArrayList<>();
		}

		if (DEBUG) Log.i(TAG, "Alarm list retrieved successfully.");
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
			for (Listable l : data.getListables()) {
				builder.append(l.toStoreString()).append('\n');
			}
			// delete the last '\n'
			if (builder.length() != 0) builder.deleteCharAt(builder.length() - 1);

			os.write(builder.toString().getBytes());
			os.close();
		}
		catch (IOException e) {
			if (DEBUG) Log.e(TAG, e.getMessage());
		}
	}

	/* ********************************  Handle Message Methods  ******************************** */

	/**
	 * Responds to an inbound MSG_GET_LISTABLE message. Using the replyTo field from the message,
	 * sends the requested Listable to that Messenger, or if null sends it to the Handler specified
	 * by the getTarget() method.
	 * @param inMsg the inbound MSG_GET_LISTABLE message
	 */
	private void handleGetListable(@NotNull Message inMsg) {
		if (inMsg.replyTo == null && inMsg.getTarget() == null) {
			if (DEBUG) Log.e(TAG, "MSG_GET_LISTABLE: Message had a null reply to field and no target.");
			return;
		}

		// get listable with abs index in arg1
		ListableInfo info = rootFolder.getListableInfo(inMsg.arg1);
		Message outMsg = Message.obtain(null, MSG_GET_LISTABLE);
		Bundle bundle = new Bundle();
		bundle.putParcelable(BUNDLE_INFO_KEY, info);
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
	 * Responds to an inbound MSG_GET_LISTABLE message. Using the replyTo field from the message,
	 * sends the requested Listable to that Messenger, or if null sends it to the Handler specified
	 * by the getTarget() method.
	 * @param inMsg the inbound MSG_GET_LISTABLE message
	 */
	private void handleGetFolders(@NotNull Message inMsg) {
		if (inMsg.replyTo == null && inMsg.getTarget() == null) {
			if (DEBUG) Log.e(TAG, "MSG_GET_FOLDERS: Message had a null reply to field and no target.");
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
	 * @param inMsg the inbound MSG_SET_LISTABLE message
	 */
	private void handleSetListable(@NotNull Message inMsg) {
		if (inMsg.getData() == null) return;

		ListableInfo info = inMsg.getData().getParcelable(BUNDLE_INFO_KEY);
		if (info == null) return;

		rootFolder.setListableAbs(inMsg.arg1, info.listable);
		writeAlarmsToDisk(this, rootFolder);
		setNextAlarmToRing();
		sendDataChanged();
	}

	/**
	 * Responds to an inbound MSG_ADD_LISTABLE message. For now, just adds the listable to the end
	 * of the rootFolder.
	 * TODO: add the listable anywhere in the dataset
	 * @param inMsg the inbound MSG_ADD_LISTABLE message
	 */
	private void handleAddListable(@NotNull Message inMsg) {
		ListableInfo info = inMsg.getData().getParcelable(BUNDLE_INFO_KEY);
		if (info == null) {
			if (DEBUG) Log.e(TAG, "MSG_ADD_LISTABLE: Listable was null.");
			return;
		}

		rootFolder.addListable(info.listable);
		writeAlarmsToDisk(this, rootFolder);
		setNextAlarmToRing();
		sendDataChanged();

		// just added the first new Listable
		if (rootFolder.size() == 2) {
			sendDataFilled();
		}
	}

	/**
	 * Responds to an inbound MSG_MOVE_LISTABLE message. Doesn't do anything right now.
	 */
	private void handleMoveListable(@NotNull Message inMsg) {
		ListableInfo info = inMsg.getData().getParcelable(BUNDLE_INFO_KEY);
		if (info == null) {
			if (DEBUG) Log.e(TAG, "MSG_MOVE_LISTABLE: Listable was null.");
			return;
		}

		if (info.path == null) {
			if (DEBUG) Log.e(TAG, "MSG_MOVE_LISTABLE: Path was not specified.");
			return;
		}

		String[] folders = info.path.split("/");	// ignore the first one (root folder name)
		AlarmGroup currFolder = rootFolder;
		for (int i = 1; i < folders.length; i++) {
			currFolder = currFolder.getFolder(folders[i]);
			if (currFolder == null) {
				if (DEBUG) Log.e(TAG, "MSG_MOVE_LISTABLE: Couldn't find the specified path.");
				return;
			}
		}

		Listable newListable = info.listable;
		if (newListable == null) {
			// replace with what's currently at the abs index
			newListable = rootFolder.deleteListableAbs(inMsg.arg1);
		}
		else {
			// replace with the new listable
			rootFolder.deleteListableAbs(inMsg.arg1);
		}

		currFolder.addListable(newListable);
		rootFolder.refreshLookup();

		writeAlarmsToDisk(this, rootFolder);
		setNextAlarmToRing();
		sendDataChanged();
	}

	/**
	 * Responds to an inbound MSG_DELETE_LISTABLE message. Deletes the listable at the absolute index
	 * specified by inMsg.arg1
	 * @param inMsg the inbound MSG_DELETE_LISTABLE message
	 */
	private void handleDeleteListable(@NotNull Message inMsg) {
		rootFolder.deleteListableAbs(inMsg.arg1);
		writeAlarmsToDisk(this, rootFolder);
		setNextAlarmToRing();
		sendDataChanged();

		// just deleted the last Listable
		if (rootFolder.size() == 1) {
			sendDataEmptied();
		}
	}

	/**
	 * Responds to an inbound MSG_TOGGLE_ACTIVE message. Toggles the active state of the listable at
	 * the absolute index specified by arg1.
	 * @param inMsg the inbound MSG_TOGGLE_ACTIVE message
	 */
	private void handleToggleActive(@NotNull Message inMsg) {
		Listable l = rootFolder.getListableAbs(inMsg.arg1);
		if (l == null) {
			if (DEBUG) Log.e(TAG, "MSG_TOGGLE_ACTIVE: Index of listable was out of bounds.");
			return;
		}
		l.toggleActive();

		writeAlarmsToDisk(this, rootFolder);
		setNextAlarmToRing();
	}

	/**
	 * Responds to an inbound MSG_TOGGLE_OPEN_FOLDER message. Toggles the open state of the folder at
	 * the absolute index specified by arg1.
	 * @param inMsg the inbound MSG_TOGGLE_OPEN_FOLDER message
	 */
	private void handleToggleOpen(@NotNull Message inMsg) {
		Listable l = rootFolder.getListableAbs(inMsg.arg1);
		if (l == null) {
			if (DEBUG) Log.e(TAG, "MSG_TOGGLE_OPEN_FOLDER: Listable index was out of bounds.");
			return;
		}
		else if (l.isAlarm()) {
			if (DEBUG) Log.e(TAG, "MSG_TOGGLE_OPEN_FOLDER: Listable was an alarm.");
			return;
		}
		((AlarmGroup) l).toggleOpen();

		writeAlarmsToDisk(this, rootFolder);
		rootFolder.refreshLookup();
		sendDataChanged();
	}

	/**
	 * Responds to an inbound MSG_SNOOZE_ALARM message. Snoozes the alarm specified by the absolute
	 * index specified by arg1.
	 * @param inMsg the inbound MSG_SNOOZE_ALARM message
	 */
	private void handleSnoozeAlarm(@NotNull Message inMsg) {
		Listable l = rootFolder.getListableAbs(inMsg.arg1);
		if (l == null) {
			if (DEBUG) Log.e(TAG, "MSG_SNOOZE_ALARM: Listable index was out of bounds.");
			return;
		}
		else if (l.isAlarm()) {
			if (DEBUG) Log.e(TAG, "MSG_SNOOZE_ALARM: Listable was a folder.");
			return;
		}
		((Alarm) l).snooze();

		writeAlarmsToDisk(this, rootFolder);
		setNextAlarmToRing();
		sendDataChanged();
	}

	/**
	 * Responds to an inbound MSG_UNSNOOZE_ALARM message. Unsnoozes the alarm specified by the
	 * absolute index specified by arg1.
	 * @param inMsg the inbound MSG_UNSNOOZE_ALARM message
	 */
	private void handleUnsnoozeAlarm(@NotNull Message inMsg) {
		Listable l = rootFolder.getListableAbs(inMsg.arg1);
		if (l == null) {
			if (DEBUG) Log.e(TAG, "MSG_UNSNOOZE_ALARM: Listable index was out of bounds.");
			return;
		}
		else if (!l.isAlarm()) {
			if (DEBUG) Log.e(TAG, "MSG_UNSNOOZE_ALARM: Listable was a folder.");
			return;
		}
		((Alarm) l).unsnooze();

		writeAlarmsToDisk(this, rootFolder);
		setNextAlarmToRing();
		sendDataChanged();
	}

	/**
	 * Responds to an inbound MSG_DISMISS_ALARM message. Dismisses the alarm specified by the
	 * absolute index specified by arg1.
	 * @param inMsg the inbound MSG_DISMISS_ALARM message
	 */
	private void handleDismissAlarm(@NotNull Message inMsg) {
		Listable l = rootFolder.getListableAbs(inMsg.arg1);
		if (l == null) {
			if (DEBUG) Log.e(TAG, "MSG_DISMISS_ALARM: Listable index was out of bounds.");
			return;
		}
		else if (!l.isAlarm()) {
			if (DEBUG) Log.e(TAG, "MSG_DISMISS_ALARM: Listable was a folder.");
			return;
		}

		((Alarm) l).unsnooze();

		switch (((Alarm) l).getRepeatType()) {
			case Alarm.REPEAT_ONCE_ABS:
			case Alarm.REPEAT_ONCE_REL:
				l.turnOff();
				break;
			case Alarm.REPEAT_DAY_WEEKLY:
			case Alarm.REPEAT_DATE_MONTHLY:
			case Alarm.REPEAT_DAY_MONTHLY:
			case Alarm.REPEAT_DATE_YEARLY:
			case Alarm.REPEAT_OFFSET:
				((Alarm) l).updateRingTime();
				break;
			default:
				if (DEBUG) Log.wtf(TAG, "MSG_DISMISS_ALARM: The alarm repeat type was invalid...?");
				break;
		}

		writeAlarmsToDisk(this, rootFolder);
		setNextAlarmToRing();
		sendDataChanged();
	}

	/**
	 * Responds to an inbound MSG_DATA_CHANGED message. Using the replyTo field from the message,
	 * either registers it (if not registered) as a data listener or unregisters it (if already
	 * registered).
	 * @param inMsg the inbound MSG_DATA_CHANGED message
	 */
	private void handleDataChanged(@NotNull Message inMsg) {
		if (inMsg.replyTo == null) {
			// invalid message
			if (DEBUG) Log.e(TAG, "MSG_DATA_CHANGED: Message didn't have a Messenger to reply to.");
		}
		else {
			int index = dataChangeListeners.indexOf(inMsg.replyTo);
			if (index == -1) {
				dataChangeListeners.add(inMsg.replyTo);

				Message outMsg = Message.obtain(null, MSG_DATA_CHANGED);
				outMsg.arg1 = rootFolder.size() - 1;
				try {
					inMsg.replyTo.send(outMsg);
				}
				catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			else
				dataChangeListeners.remove(index);
		}
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
			if (DEBUG) Log.e(TAG, "MSG_DATA_EMPTY_LISTENER: Message didn't have a Messenger to reply to.");
		}
		else {
			int index = emptyListeners.indexOf(inMsg.replyTo);
			if (index == -1) {
				// register the empty listener
				emptyListeners.add(inMsg.replyTo);

				// send a DATA_EMPTIED or DATA_FILLED to the new listener
				Message outMsg = Message.obtain();
				if (rootFolder.size() == 1)
					outMsg.what = MSG_DATA_EMPTIED;
				else
					outMsg.what = MSG_DATA_FILLED;

				try {
					inMsg.replyTo.send(outMsg);
				}
				catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			else {
				// unregister the empty listener
				emptyListeners.remove(index);
			}
		}
	}

	/* ********************************  Send Message Methods  ******************************** */

	/**
	 * Sends MSG_DATA_CHANGED messages to all registered data change listeners. The message should
	 * have the number of listables in the arg1 field.
	 */
	private void sendDataChanged() {
		Message outMsg;
		int numListables = rootFolder.size() - 1;

		for (Messenger m : dataChangeListeners) {
			outMsg = Message.obtain(null, MSG_DATA_CHANGED);
			outMsg.arg1 = numListables;
			try {
				m.send(outMsg);
			}
			catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Send a MSG_DATA_EMPTIED message to all registered empty listeners.
	 */
	private void sendDataEmptied() {
		Message outMsg;
		for (Messenger m : emptyListeners) {
			outMsg = Message.obtain(null, MSG_DATA_EMPTIED);
			try {
				m.send(outMsg);
			}
			catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Send a MSG_DATA_FILLED message to all registered empty listeners.
	 */
	private void sendDataFilled() {
		Message outMsg;
		for (Messenger m : emptyListeners) {
			outMsg = Message.obtain(null, MSG_DATA_FILLED);
			try {
				m.send(outMsg);
			}
			catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sets the next alarm to ring. Does not create a new pending intent, rather updates the current
	 * one. Tells AlarmManager to wake up and call AlarmRingingService.
	 */
	private void setNextAlarmToRing() {
		ListableInfo next = getNextRingingAlarm(rootFolder.getListables());

		Intent intent = new Intent(this, AlarmRingingService.class);
		if (next.listable != null) {
			intent.putExtra(AlarmRingingService.EXTRA_LISTABLE, next.listable.toEditString());
			intent.putExtra(AlarmRingingService.EXTRA_LISTABLE_INDEX, next.absIndex);
		}

		AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		PendingIntent pendingIntent;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			pendingIntent = PendingIntent.getForegroundService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		}
		else {
			pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		}

		if (manager != null && pendingIntent != null) {
			if (next.listable == null) {
				if (DEBUG) Log.i(TAG, "No next listable to register to ring.");
				manager.cancel(pendingIntent);
				return;
			}

			manager.setAlarmClock(
					new AlarmManager.AlarmClockInfo(((Alarm) next.listable).getAlarmTimeMillis(),
							pendingIntent),
					pendingIntent);
			if (DEBUG) Log.i(TAG, "Sent an intent to AlarmManager.");
		}
	}

	/* *************************************  Other Methods  *********************************** */

	/**
	 * Searches for the next Alarm that will ring. Returns the listable and absolute index of the
	 * listable (within the current dataset) within a ListableInfo struct.
	 * @param data the dataset to look through, cannot be null
	 * @return a ListableInfo with alarm and absolute index filled correctly, alarm can be null if
	 * there is no active alarm within the data given
	 */
	@NotNull
	private static ListableInfo getNextRingingAlarm(@NotNull ArrayList<Listable> data) {
		ListableInfo nextAlarm = new ListableInfo();
		// represents the current listable being searched
		Listable l;
		// represents the absolute index currently being searched
		int absIndex = 0;

		for (int i = 0; i < data.size(); i++) {
			l = data.get(i);

			if (!l.isActive()) {
				absIndex += l.size();
				continue;
			}

			if (l.isAlarm()) {
				((Alarm) l).updateRingTime();

				// check whether it could be the next listable
				if (nextAlarm.listable == null || ((Alarm) l).getAlarmTimeMillis() <
						((Alarm) nextAlarm.listable).getAlarmTimeMillis()) {
					nextAlarm.listable = l;
					nextAlarm.absIndex = absIndex;
				}
				absIndex++;
			}
			else {
				ListableInfo possible = getNextRingingAlarm(((AlarmGroup) l).getListables());
				// there is no candidate in this folder
				if (possible.listable == null) {
					absIndex += l.size();
					continue;
				}
				// we had no candidate before or this candidate is better
				if (nextAlarm.listable == null || ((Alarm) possible.listable).getAlarmTimeMillis() <
						((Alarm) nextAlarm.listable).getAlarmTimeMillis()) {
					nextAlarm.listable = possible.listable;
					nextAlarm.absIndex = absIndex + possible.absIndex;
				}
				absIndex += l.size();
			}
		}
		return nextAlarm;
	}

	/**
	 * Creates a notification channel if the API level requires it. Otherwise, does nothing.
	 */
	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = getString(R.string.notif_channel_name);
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel channel = new NotificationChannel(AlarmRingingService.CHANNEL_ID, name, importance);
			channel.setShowBadge(false);
			channel.setBypassDnd(true);
			channel.enableLights(true);
			channel.enableVibration(true);
			channel.setSound(null, null);

			// Register the channel with the system; can't change the importance or behaviors after this
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			if (notificationManager == null) {
				if (DEBUG) Log.e(TAG, "System returned a null notification manager.");
				return;
			}
			notificationManager.createNotificationChannel(channel);
		}
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
				if (DEBUG) Log.e(TAG, "Message sent to the data service was null. Ignoring...");
				return;
			}

			switch(msg.what) {
				case MSG_GET_LISTABLE:
					service.handleGetListable(msg);
					if (DEBUG) Log.d(TAG, "Got a listable for a client.");
					break;
				case MSG_GET_FOLDERS:
					service.handleGetFolders(msg);
					if (DEBUG) Log.d(TAG, "Got folder structure for a client.");
					break;
				case MSG_SET_LISTABLE:
					service.handleSetListable(msg);
					if (DEBUG) Log.d(TAG, "Set a listable to a specific index.");
					break;
				case MSG_ADD_LISTABLE:
					service.handleAddListable(msg);
					if (DEBUG) Log.d(TAG, "Added a listable to the end of the rootFolder.");
					break;
				case MSG_MOVE_LISTABLE:
					service.handleMoveListable(msg);
					if (DEBUG) Log.d(TAG, "Moved a listable.");
					break;
				case MSG_DELETE_LISTABLE:
					service.handleDeleteListable(msg);
					if (DEBUG) Log.d(TAG, "Deleted the listable at the specified absolute index.");
					break;
				case MSG_TOGGLE_ACTIVE:
					service.handleToggleActive(msg);
					if (DEBUG) Log.d(TAG, "Toggled a listable's active state.");
					break;
				case MSG_TOGGLE_OPEN_FOLDER:
					service.handleToggleOpen(msg);
					if (DEBUG) Log.d(TAG, "Toggled a folder's open state.");
					break;
				case MSG_SNOOZE_ALARM:
					service.handleSnoozeAlarm(msg);
					if (DEBUG) Log.d(TAG, "Snoozed an alarm.");
					break;
				case MSG_UNSNOOZE_ALARM:
					service.handleUnsnoozeAlarm(msg);
					if (DEBUG) Log.d(TAG, "Unsnoozed an alarm.");
					break;
				case MSG_DISMISS_ALARM:
					service.handleDismissAlarm(msg);
					if (DEBUG) Log.d(TAG, "Dismissed an alarm.");
					break;
				case MSG_DATA_CHANGED:
					service.handleDataChanged(msg);
					if (DEBUG) Log.d(TAG, "Added or removed a data changed listener.");
					break;
				case MSG_DATA_EMPTY_LISTENER:
					service.handleDataEmpty(msg);
					if (DEBUG) Log.d(TAG, "Added or removed a data empty listener.");
					break;
				default:
					if (DEBUG) Log.e(TAG, "Unknown message type. Sending to Handler's handleMessage().");
					super.handleMessage(msg);
					break;
			}
		}
	}
}
