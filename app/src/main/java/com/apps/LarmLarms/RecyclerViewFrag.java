package com.apps.LarmLarms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Manages the Recycler View fragment.
 */

public class RecyclerViewFrag extends Fragment {
	private static final String TAG = "RecyclerViewFragment";
	
	private RecyclerViewAdapter myAdapter;

	/* **********************************  Lifecycle Methods  ******************************** */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			ArrayList<Listable> list = initData();

			// initialize adapter b/c we need to ensure data is available after initialization (didn't
			// necessarily call onCreateView yet)
			myAdapter = new RecyclerViewAdapter(getContext(), list);
			Log.i(TAG, "Fragment initialized successfully.");
		}
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

	/* *****************************  Getter and Setter Methods  ****************************** */

	public RecyclerViewAdapter getAdapter() { return myAdapter; }

	/* ************************************  Other Methods  ********************************** */
	public boolean isDataEmpty() { return myAdapter.getItemCount() == 0; }

	private ArrayList<Listable> initData() {
		// TODO: initialize real data
		ArrayList<Listable> data = new ArrayList<>();

		/*
		AlarmGroup folder, insideFolder;
		Alarm currAlarm;

		currAlarm = new Alarm(getActivity(), "good morning");
		currAlarm.setRepeatType(Alarm.REPEAT_ONCE_ABS);
		data.add(currAlarm);

		currAlarm = new Alarm(getActivity(), "eh?");
		currAlarm.setRepeatType(Alarm.REPEAT_ONCE_REL);
		data.add(currAlarm);

		folder = new AlarmGroup(getActivity(), "あ");
		insideFolder = new AlarmGroup(getActivity(), "学校");

		currAlarm = new Alarm(getActivity(), "blah");
		currAlarm.setRepeatType(Alarm.REPEAT_DAY_WEEKLY);
		folder.addListable(currAlarm);

		currAlarm = new Alarm(getActivity(), "hah?");
		currAlarm.setRepeatType(Alarm.REPEAT_DATE_MONTHLY);
		folder.addListable(currAlarm);
		data.add(folder);

		currAlarm = new Alarm(getActivity(), "wut");
		currAlarm.setRepeatType(Alarm.REPEAT_DAY_MONTHLY);
		insideFolder.addListable(currAlarm);
		folder.addListable(insideFolder);

		currAlarm = new Alarm(getActivity(), "?");
		currAlarm.setRepeatType(Alarm.REPEAT_DATE_YEARLY);
		data.add(currAlarm);

		currAlarm = new Alarm(getActivity(), "??");
		currAlarm.setRepeatType(Alarm.REPEAT_OFFSET);
		data.add(currAlarm);
		*/

		Log.i(TAG, "Alarm list retrieved successfully.");
		return data;
	}

	/**
	 * Refreshes alarms to coordinate with updated settings (mostly for time formatting). The data
	 * doesn't change at all, we just need to rebind the ViewHolders to get the correct date string.
	 */
	public void refreshAlarms() { myAdapter.notifyDataSetChanged(); }

	/**
	 * Adds a Listable to the end of the dataset.
	 * @param item the new Listable to add
	 */
	public void addListable(Listable item) {
		if (item == null) {
			Log.e(TAG, "Cannot add Listable. Invalid Listable.");
			return;
		}
		int startAbsPos = myAdapter.getItemCount();		// where the item should be added

		myAdapter.addListable(item);
		myAdapter.notifyItemRangeInserted(startAbsPos, item.getNumItems());
	}

	/**
	 * Replaces the Listable at the specified index. If an error is encountered, it will silently
	 * exit.
	 * @param index the index to replace with the Alarm
	 * @param item the Listable to replace it with
	 */
	public void replaceListable(int index, Listable item) {
		if (index < 0 || index >= myAdapter.getItemCount()) {
			Log.e(TAG, "Cannot replace Listable. Invalid index received.");
			return;
		}
		else if (item == null) {
			Log.e(TAG, "Cannot replace Listable. Invalid Listable.");
			return;
		}

		myAdapter.setListableAbs(index, item);
		myAdapter.notifyItemRangeChanged(index, myAdapter.getItemCount() - index);
	}

	/**
	 * Should be called when ListableEditor returns with a Listable.
	 * @param requestCode the code that we requested with
	 * @param data the intent containing the returned Listable
	 */
	public void onListableCreatorResult(int requestCode, Intent data) {
		Listable new_listable;
		int index = data.getIntExtra(MainActivity.EXTRA_LISTABLE_INDEX, -1);

		switch(requestCode) {
			case MainActivity.REQ_NEW_ALARM:
				new_listable = Alarm.fromEditString(getContext(), data.getStringExtra(MainActivity.EXTRA_LISTABLE));
				if (new_listable == null) {
					Log.e(TAG, "ListableEditor returned with an invalid alarm edit string.");
					return;
				}
				// TODO: add new alarm where its supposed to be nested
				addListable(new_listable);
				Log.i(TAG, "New alarm saved successfully.");
				break;
			case MainActivity.REQ_EDIT_ALARM:
				new_listable = Alarm.fromEditString(getContext(), data.getStringExtra(MainActivity.EXTRA_LISTABLE));
				if (new_listable == null) {
					Log.e(TAG, "ListableEditor returned with an invalid alarm edit string.");
					return;
				}
				replaceListable(index, new_listable);
				Log.i(TAG, "Existing alarm edited successfully.");
				break;
			case MainActivity.REQ_NEW_FOLDER:
				new_listable = AlarmGroup.fromEditString(getContext(), data.getStringExtra(MainActivity.EXTRA_LISTABLE));
				if (new_listable == null) {
					Log.e(TAG, "ListableEditor returned with an invalid folder edit string.");
					return;
				}
				// TODO: add new folder where its supposed to be nested
				addListable(new_listable);
				Log.i(TAG, "New folder saved successfully.");
				break;
			case MainActivity.REQ_EDIT_FOLDER:
				// will not delete children Listables of original AlarmGroup
				new_listable = AlarmGroup.fromEditString(getContext(), data.getStringExtra(MainActivity.EXTRA_LISTABLE));
				if (new_listable == null) {
					Log.e(TAG, "ListableEditor returned with an invalid folder edit string.");
					return;
				}

				Listable old_listable = myAdapter.getListableAbs(index);
				if (old_listable == null || old_listable.isAlarm()) {
					Log.e(TAG, "ListableEditor returned with an invalid index.");
					return;
				}
				((AlarmGroup) new_listable).setListablesInside(((AlarmGroup) old_listable).getListablesInside());

				replaceListable(index, new_listable);
				Log.i(TAG, "Existing folder edited successfully.");
				break;
		}

		myAdapter.setNextAlarmToRing();
	}
}