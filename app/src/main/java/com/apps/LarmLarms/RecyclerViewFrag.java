package com.apps.LarmLarms;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Manages the Recycler View fragment.
 */

public class RecyclerViewFrag extends Fragment {
	private static final String TAG = "RecyclerViewFragment";
	private static final String ALARM_STORE_FILE_NAME = "alarms.txt";
	
	private RecyclerViewAdapter myAdapter;

	/* **********************************  Lifecycle Methods  ******************************** */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// initialize adapter b/c we need to ensure data is available after initialization (didn't
		// necessarily call onCreateView yet)
		if (savedInstanceState == null) {
			myAdapter = new RecyclerViewAdapter(getContext(), getAlarmsFromDisk(getContext()));
			Log.i(TAG, "Fragment initialized successfully.");
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		myAdapter.setListables(getAlarmsFromDisk(getContext()));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// rootView is the LinearLayout in recycler_view_frag.xml
		View rootView = inflater.inflate(R.layout.recycler_view_frag, container, false);

		RecyclerView myRecyclerView = rootView.findViewById(R.id.recycler_view);

		LinearLayoutManager myLayoutManager = new LinearLayoutManager(getActivity());
		myRecyclerView.setLayoutManager(myLayoutManager);
		myRecyclerView.scrollToPosition(0);
		myRecyclerView.setAdapter(myAdapter);

		return rootView;
	}

	@Override
	public void onStop() {
		super.onStop();
		writeAlarmsToDisk(getContext(), myAdapter);
	}

	/* ************************************  Callbacks  ************************************** */

	/**
	 * Should be called when ListableEditor returns with a Listable. Deals with the listables based
	 * on request code.
	 * @param requestCode the code that we requested with
	 * @param data the intent containing the returned Listable
	 */
	void onListableCreatorResult(int requestCode, Intent data) {
		Listable new_listable;
		int index = data.getIntExtra(ListableEditorActivity.EXTRA_LISTABLE_INDEX, -1);

		switch(requestCode) {
			case ListableEditorActivity.REQ_NEW_ALARM:
				new_listable = Alarm.fromEditString(getContext(),
						data.getStringExtra(ListableEditorActivity.EXTRA_LISTABLE));
				if (new_listable == null) {
					Log.e(TAG, "ListableEditor returned with an invalid alarm edit string.");
					return;
				}
				// TODO: add new alarm where its supposed to be nested
				myAdapter.addListable(new_listable);
				Log.i(TAG, "New alarm saved successfully.");
				break;
			case ListableEditorActivity.REQ_EDIT_ALARM:
				new_listable = Alarm.fromEditString(getContext(),
						data.getStringExtra(ListableEditorActivity.EXTRA_LISTABLE));
				if (new_listable == null) {
					Log.e(TAG, "ListableEditor returned with an invalid alarm edit string.");
					return;
				}
				myAdapter.setListableAbs(index, new_listable);
				Log.i(TAG, "Existing alarm edited successfully.");
				break;
			case ListableEditorActivity.REQ_NEW_FOLDER:
				new_listable = AlarmGroup.fromEditString(
						data.getStringExtra(ListableEditorActivity.EXTRA_LISTABLE));
				if (new_listable == null) {
					Log.e(TAG, "ListableEditor returned with an invalid folder edit string.");
					return;
				}
				// TODO: add new folder where its supposed to be nested
				myAdapter.addListable(new_listable);
				Log.i(TAG, "New folder saved successfully.");
				break;
			case ListableEditorActivity.REQ_EDIT_FOLDER:
				// will not delete children Listables of original AlarmGroup
				new_listable = AlarmGroup.fromEditString(
						data.getStringExtra(ListableEditorActivity.EXTRA_LISTABLE));
				if (new_listable == null) {
					Log.e(TAG, "ListableEditor returned with an invalid folder edit string.");
					return;
				}

				Listable old_listable = myAdapter.getListableAbs(index);
				if (old_listable == null || old_listable.isAlarm()) {
					Log.e(TAG, "ListableEditor returned with an invalid index.");
					return;
				}
				((AlarmGroup) new_listable).setListables(((AlarmGroup) old_listable).getListables());

				myAdapter.setListableAbs(index, new_listable);
				Log.i(TAG, "Existing folder edited successfully.");
				break;
		}

		writeAlarmsToDisk(getContext(), myAdapter);
		myAdapter.setNextAlarmToRing();
	}

	/* ************************************  Other Methods  ********************************** */
	boolean isDataEmpty() { return myAdapter.getItemCount() == 0; }

	/**
	 * Initializes alarm data from file.
	 * @return A populated ArrayList of Listables or an empty one in the case of an error
	 */
	@NotNull
	static ArrayList<Listable> getAlarmsFromDisk(Context context) {
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
	 */
	static void writeAlarmsToDisk(Context context, RecyclerViewAdapter adapter) {
		try {
			File alarmFile = new File(context.getFilesDir(), ALARM_STORE_FILE_NAME);
			//noinspection ResultOfMethodCallIgnored
			alarmFile.createNewFile();

			FileOutputStream os = context.openFileOutput(ALARM_STORE_FILE_NAME, Context.MODE_PRIVATE);

			StringBuilder builder = new StringBuilder();
			for (Listable l : adapter.getListables()) {
				builder.append(l.toStoreString()).append('\n');
			}
			// delete the last '\n'
			if (builder.length() != 0) builder.deleteCharAt(builder.length() - 1);

			System.out.println("Here is what we wrote to disk: " + builder.toString());

			os.write(builder.toString().getBytes());
			os.close();
		}
		catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	/**
	 * Refreshes alarms to coordinate with updated settings (mostly for time formatting). The data
	 * doesn't change at all, we just need to rebind the ViewHolders to get the correct date string.
	 */
	void refreshAlarms() { myAdapter.notifyDataSetChanged(); }
}