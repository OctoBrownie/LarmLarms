package com.apps.LarmLarms;

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

import org.jetbrains.annotations.NotNull;

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
	private static final String TAG = "AlarmDataService";
	private static final String HANDLER_THREAD_NAME = "AlarmDataHandler";
	private static final String ALARM_STORE_FILE_NAME = "alarms.txt";

	/**
	 * In a bundle, this is the key used to store a ListableInfo
	 */
	static final String BUNDLE_INFO_KEY = "listableInfoKey";

	/* *****************************  Message what field constants  **************************** */

	/**
	 * Inbound: A client wants to be notified of any data changes. In the replyTo field there should
	 * be a Messenger to send a MSG_DATA_CHANGED message to when data has changed.
	 * <br/>
	 * Outbound: Means that the data has been changed, only sent to registered listeners. No
	 * guarantees about the fields within the message. Means that the new data has been written to
	 * the alarm store and can be queried from the service or read directly from disk.
	 */
	static final int MSG_DATA_CHANGED = 0;

	/**
	 * Inbound: the client is asking for a specific Listable. The specified Listable's absolute
	 * index should be in the arg1 field, and the Messenger to reply to should be in the replyTo
	 * field. Can also take a Handler coming from the getTarget() method, but a Messenger is
	 * preferred and will be used instead if present.
	 * <br/>
	 * Outbound: a response with a ListableInfo from the Service. Puts the absolute index in the arg1
	 * field and the ListableInfo in a bundle accessible by getData() or peekData() using the
	 * BUNDLE_INFO_KEY for the key.
	 */
	static final int MSG_GET_LISTABLE = 1;
	/**
	 * Inbound: the client is asking for the number of Listables in the dataset. No other fields
	 * need to be specified.
	 * <br/>
	 * Outbound: a response with the number of items from the Service. Puts the item count in the
	 * arg1 field.
	 */
	static final int MSG_GET_LISTABLE_COUNT = 2;

	// changing the dataset, triggers MSG_DATA_CHANGED messages
	/**
	 * Inbound: the client wants to set a Listable to a certain idex. The absolute index of the
	 * Listable should be in the arg1 field and the Listable itself in the obj field.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_SET_LISTABLE = 3;
	/**
	 * Inbound: the client wants to add a Listable at the specified index and parent. A ListableInfo
	 * should be in the data bundle (using BUNDLE_INFO_KEY for its key), with the new Listable in the
	 * Listable field, the absolute index of the new parent in absParentIndex, and the new absolute
	 * index in absIndex.
	 * TODO: Figure out what information is needed for adding Listables anywhere in a list
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_ADD_LISTABLE = 4;
	/**
	 * Inbound: the client wants to move a Listable to a new index. A ListableInfo should be in the
	 * data bundle (using BUNDLE_INFO_KEY for its key), with the absolute index of the new parent in
	 * absParentIndex and the new absolute index in absIndex. arg1 should be filled with the old
	 * absolute index of the Listable.
	 * TODO: Figure out what information is needed for moving Listables anywhere in a list
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_MOVE_LISTABLE = 5;
	/**
	 * Inbound: the client wants to delete a Listable. The absolute index of the Listable should be
	 * in the arg1 field.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_DELETE_LISTABLE = 6;

	// changing the Listables themselves, triggers MSG_DATA_CHANGED messages
	/**
	 * Inbound: the client wants to turn a Listable on. The absolute index of the Listable should be
	 * in the arg1 field.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_TURN_ON_LISTABLE = 7;
	/**
	 * Inbound: the client wants to toggle isActive on a Listable. The absolute index of the
	 * Listable should be in the arg1 field.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_TOGGLE_ACTIVE_LISTABLE = 8;
	/**
	 * Inbound: the client wants to toggle an AlarmGroup open/closed. The absolute index of the
	 * AlarmGroup should be in the arg1 field.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_TOGGLE_OPEN_FOLDER = 9;
	/**
	 * Inbound: the client wants to snooze an Alarm. The absolute index of the Alarm should be
	 * in the arg1 field.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_SNOOZE_ALARM = 10;
	/**
	 * Inbound: the client wants to unsnooze an Alarm. The absolute index of the Alarm should be
	 * in the arg1 field.
	 * <br/>
	 * Outbound: N/A
	 */
	static final int MSG_UNSNOOZE_ALARM = 11;

	/* *************************************  Instance Fields  ********************************** */

	private HandlerThread handlerThread;
	private List<Messenger> dataChangeListeners;

	private AlarmGroup dataset;

	/* **********************************  Lifecycle Methods  ********************************** */

	public AlarmDataService() {
		dataset = new AlarmGroup(getResources().getString(R.string.root_folder), getAlarmsFromDisk(this));
		dataChangeListeners = new ArrayList<>();
	}

	/**
	 * Creates a new handler thread and registers a new messenger to handle messages within that
	 * thread.
	 * @param intent intent delivered from the first call to Context.bindService()
	 * @return an IBinder for a Messenger
	 */
	@Override
	public IBinder onBind(Intent intent) {
		handlerThread = new HandlerThread(HANDLER_THREAD_NAME);
		handlerThread.start();
		Messenger messenger = new Messenger(new MsgHandler(this, handlerThread));
		return messenger.getBinder();
	}

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
	static ArrayList<Listable> getAlarmsFromDisk(@NotNull Context context) {
		ArrayList<Listable> data = new ArrayList<>();

		try {
			FileInputStream is = context.openFileInput(ALARM_STORE_FILE_NAME);
			InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
			BufferedReader bReader = new BufferedReader(isr);

			String currLine = bReader.readLine();
			StringBuilder currFolder = null;
			Listable currListable;

			while (currLine != null) {
				if (!currLine.startsWith("\t") && currFolder != null) {
					// removing the last '\n'
					if (currFolder.length() != 0) currFolder.deleteCharAt(currFolder.length() - 1);
					currListable = AlarmGroup.fromStoreString(context, currFolder.toString());
					if (currListable != null) { data.add(currListable); }
				}

				if (currLine.startsWith("a")) {
					currListable = Alarm.fromStoreString(context, currLine);
					if (currListable != null) { data.add(currListable); }
				}
				else if (currLine.startsWith("f")) {
					currFolder = new StringBuilder(currLine);
				}
				else if (currLine.startsWith("\t") && currFolder != null) {
					currFolder.append(currLine);
				}
				else {
					Log.e(TAG, "Invalid line in alarms.txt: " + currLine);
					return new ArrayList<>();
				}
				currLine = bReader.readLine();
			}
			if (currFolder != null) {
				if (currFolder.length() != 0) currFolder.deleteCharAt(currFolder.length() - 1);
				currListable = AlarmGroup.fromStoreString(context, currFolder.toString());
				if (currListable != null) { data.add(currListable); }
			}

			is.close();
		}
		catch (IOException e) {
			Log.e(TAG, e.getMessage());
			return new ArrayList<>();
		}

		Log.i(TAG, "Alarm list retrieved successfully.");
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
			Log.e(TAG, e.getMessage());
		}
	}

	/* **********************************  Handler Methods  ************************************* */

	/**
	 * Responds to an inbound MSG_GET_LISTABLE message. Using the replyTo field from the message,
	 * sends the requested Listable to that Messenger, or if null sends it to the Handler specified
	 * by the getTarget() method.
	 * @param inMsg the inbound MSG_GET_LISTABLE message
	 */
	private void handleGetListable(Message inMsg) {
		// get listable with abs index in arg1
		ListableInfo info = dataset.getListableInfo(inMsg.arg1);
		Message outMsg = Message.obtain(null, MSG_GET_LISTABLE, 0, 0);
		Bundle bundle = new Bundle();
		bundle.putParcelable(BUNDLE_INFO_KEY, info);
		outMsg.setData(bundle);

		if (inMsg.replyTo == null && inMsg.getTarget() == null) {
			Log.e(TAG, "Inbound MSG_GET_LISTABLE message had a null reply to field and no target.");
			return;
		}

		if (inMsg.replyTo != null) {
			try {
				inMsg.replyTo.send(outMsg);
			}
			catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		else {
			inMsg.sendToTarget();
		}
	}

	/**
	 * Responds to an inbound MSG_DATA_CHANGED message. Using the replyTo field from the message,
	 * stores it as a Messenger to send MSG_DATA_CHANGED messages to in the future.
	 * @param inMsg the inbound MSG_DATA_CHANGED message
	 */
	private void addDataChangedListener(Message inMsg) {
		if (inMsg.replyTo == null) {
			// invalid message
			Log.e(TAG, "Inbound MSG_DATA_CHANGED message didn't have a Messenger to reply to.");
		}
		else {
			dataChangeListeners.add(inMsg.replyTo);
		}
	}


	/**
	 * Inner Handler class for handling messages from Messengers.
	 */
	private static class MsgHandler extends Handler {
		AlarmDataService service;

		private MsgHandler(AlarmDataService service, HandlerThread thread) {
			super(thread.getLooper());
			this.service = service;
		}

		@Override
		public void handleMessage(Message msg) {
			// TODO: implement handleMessage
			switch(msg.what) {
				case MSG_DATA_CHANGED:
					service.addDataChangedListener(msg);
					Log.i(TAG, "Added new data changed listener.");
					break;
				case MSG_GET_LISTABLE:
					service.handleGetListable(msg);
					Log.i(TAG, "Got a listable for a client.");
					break;
				default:
					Log.e(TAG, "Unknown message type. Sending to Handler's handleMessage().");
					super.handleMessage(msg);
					break;
			}
		}
	}
}
