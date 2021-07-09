package com.apps.LarmLarms;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Manages the Recycler View fragment.
 */

public class RecyclerViewFrag extends Fragment {
	private static final String TAG = "RecyclerViewFragment";

	/**
	 * The adapter for the RecyclerView, recreated every time onCreateView() is called. Can be used
	 * when not bound to the data service, but not advised without a plan to bind to the service
	 * soon.
	 */
	private RecyclerViewAdapter myAdapter;
	private RecyclerView recyclerView;

	private boolean boundToDataService = false;
	private ServiceConnection dataConn;

	/* **********************************  Lifecycle Methods  ******************************** */

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		myAdapter = new RecyclerViewAdapter(getContext());

		// doing things for recycler view
		// rootView is the LinearLayout in recycler_view_frag.xml
		View rootView = inflater.inflate(R.layout.recycler_view_frag, container, false);

		recyclerView = rootView.findViewById(R.id.recycler_view);

		LinearLayoutManager myLayoutManager = new LinearLayoutManager(getActivity());
		recyclerView.setLayoutManager(myLayoutManager);
		recyclerView.scrollToPosition(0);

		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();

		dataConn = new DataServiceConnection();
		getContext().bindService(new Intent(getContext(), AlarmDataService.class), dataConn,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onStop() {
		super.onStop();

		if (boundToDataService) {
			recyclerView.setAdapter(null);
			myAdapter.setDataService(null);
			getContext().unbindService(dataConn);
			boundToDataService = false;
		}
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
				break;
			case ListableEditorActivity.REQ_EDIT_ALARM:
				new_listable = Alarm.fromEditString(getContext(),
						data.getStringExtra(ListableEditorActivity.EXTRA_LISTABLE));
				if (new_listable == null) {
					Log.e(TAG, "ListableEditor returned with an invalid alarm edit string.");
					return;
				}
				myAdapter.setListableAbs(index, new_listable);
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
				break;
		}
	}

	/* ************************************  Inner Classes  ********************************** */

	private class DataServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(@NotNull ComponentName className, @NotNull IBinder service) {
			boundToDataService = true;

			Messenger messenger = new Messenger(service);
			myAdapter.setDataService(messenger);
			recyclerView.setAdapter(myAdapter);
		}

		@Override
		public void onServiceDisconnected(@NotNull ComponentName className) {
			Log.e(TAG, "The data service crashed.");
			boundToDataService = false;
			recyclerView.setAdapter(null);
			myAdapter.setDataService(null);

		}
	}
}