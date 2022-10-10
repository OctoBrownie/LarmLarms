package com.larmlarms.main;

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

import com.larmlarms.BuildConfig;
import com.larmlarms.R;
import com.larmlarms.data.AlarmDataService;

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
	 * Bundle key for the recycler view's layout manager's instance state.
	 */
	private static final String BUNDLE_INSTANCE_STATE = "com.larmlarms.RECYCLER_STATE_KEY";

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
	public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) throws IllegalStateException {
		Context context = getContext();
		if (context == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "This fragment wasn't associated with a context!");
			return null;
		}
		myAdapter = new RecyclerViewAdapter(getContext());
		dataConn = new DataServiceConnection();

		// doing things for recycler view
		// rootView is the LinearLayout in recycler_view_frag.xml
		View rootView = inflater.inflate(R.layout.fragment_recycler_view, container, false);

		recyclerView = rootView.findViewById(R.id.recycler_view);

		LinearLayoutManager myLayoutManager = new LinearLayoutManager(getActivity());
		if (savedInstanceState != null) {
			myLayoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(BUNDLE_INSTANCE_STATE));
		}
		else {
			recyclerView.scrollToPosition(0);
		}
		recyclerView.setLayoutManager(myLayoutManager);
		recyclerView.setAdapter(myAdapter);

		return rootView;
	}

	/**
	 * Called when the fragment is being started. Binds to the data service.
	 */
	@Override
	public void onStart() {
		super.onStart();

		Context c = getContext();
		if (c == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "Context for the fragment was null!");
			return;
		}
		c.bindService(new Intent(getContext(), AlarmDataService.class), dataConn, Context.BIND_AUTO_CREATE);
	}

	/**
	 * Called when the fragment is closing and it wants to save its previous state. Saves the recycler
	 * view's state.
	 * @param outState the bundle to save things to
	 */
	@Override
	public void onSaveInstanceState(@NotNull Bundle outState) {
		if (recyclerView.getLayoutManager() == null) return;
		outState.putParcelable(BUNDLE_INSTANCE_STATE, recyclerView.getLayoutManager().onSaveInstanceState());
	}

	/**
	 * Called when the fragment is stopping. Unbinds from the data service.
	 */
	@Override
	public void onStop() {
		super.onStop();

		if (boundToDataService) {
			myAdapter.setDataService(null);

			Context c = getContext();
			if (c == null) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Context for the fragment was null!");
				return;
			}
			c.unbindService(dataConn);

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
		}

		/**
		 * Called when the data service crashes.
		 * @param className the name of the class that was bound to (unused)
		 */
		@Override
		public void onServiceDisconnected(@NotNull ComponentName className) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The data service crashed.");
			boundToDataService = false;
			myAdapter.setDataService(null);
		}
	}
}
