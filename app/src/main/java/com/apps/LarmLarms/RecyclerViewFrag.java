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
import org.jetbrains.annotations.Nullable;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Manages the Recycler View fragment and connection to the data service for the recycler view's
 * adapter.
 */
public class RecyclerViewFrag extends Fragment {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "RecyclerViewFragment";

	/**
	 * The adapter for the RecyclerView, recreated every time onCreateView() is called. Can be used
	 * when not bound to the data service, but not advised without a plan to bind to the service
	 * soon.
	 */
	private RecyclerViewAdapter myAdapter;
	/**
	 * The recycler view that this fragment manages.
	 */
	private RecyclerView recyclerView;

	/**
	 * Shows whether it is bound to the data service or not.
	 */
	private boolean boundToDataService = false;
	/**
	 * The service connection to the data service.
	 */
	private DataServiceConnection dataConn;

	/* **********************************  Lifecycle Methods  ******************************** */

	/**
	 * Called when the fragment is being created. Creates a new adapter and sets up the recycler view.
	 * Does not give the recycler view the adapter yet.
	 * @param inflater the layout inflater to use
	 * @param container the container to inflate in
	 * @param savedInstanceState a previous instance state
	 * @return the new root view
	 */
	@Override
	public View onCreateView(@NotNull LayoutInflater inflater, @NotNull ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		myAdapter = new RecyclerViewAdapter(getContext());
		dataConn = new DataServiceConnection();

		// doing things for recycler view
		// rootView is the LinearLayout in recycler_view_frag.xml
		View rootView = inflater.inflate(R.layout.recycler_view_frag, container, false);

		recyclerView = rootView.findViewById(R.id.recycler_view);

		LinearLayoutManager myLayoutManager = new LinearLayoutManager(getActivity());
		recyclerView.setLayoutManager(myLayoutManager);
		recyclerView.scrollToPosition(0);

		return rootView;
	}

	/**
	 * Called when the fragment is being started. Binds to the data service.
	 */
	@Override
	public void onStart() {
		super.onStart();

		getContext().bindService(new Intent(getContext(), AlarmDataService.class), dataConn,
				Context.BIND_AUTO_CREATE);
	}

	/**
	 * Called when the fragment is stopping. Unbinds from the data service.
	 */
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

	/* ************************************  Inner Classes  ********************************** */

	/**
	 * The service connection used for connecting to the data service. Used for passing the messenger
	 * on to the recycler adapter.
	 */
	private class DataServiceConnection implements ServiceConnection {
		/**
		 * Called when the service is connected. Sends the messenger to the adapter and gives the
		 * adapter to the recycler view.
		 * @param className the name of the class that was bound to (unused)
		 * @param service the binder that the service returned
		 */
		@Override
		public void onServiceConnected(@NotNull ComponentName className, @NotNull IBinder service) {
			boundToDataService = true;

			Messenger messenger = new Messenger(service);
			myAdapter.setDataService(messenger);
			recyclerView.setAdapter(myAdapter);
		}

		/**
		 * Called when the data service crashes.
		 * @param className the name of the class that was bound to (unused)
		 */
		@Override
		public void onServiceDisconnected(@NotNull ComponentName className) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The data service crashed.");
			boundToDataService = false;
			recyclerView.setAdapter(null);
			myAdapter.setDataService(null);
		}
	}
}
