package com.larmlarms.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.larmlarms.BuildConfig;
import com.larmlarms.R;

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

	// **********************************  Lifecycle Methods  ********************************

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
		if (context == null || getActivity() == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "This fragment wasn't associated with a context!");
			return null;
		}
		myAdapter = new RecyclerViewAdapter(getActivity().getApplication(), null);

		// doing things for recycler view
		// rootView is the LinearLayout in recycler_view_frag.xml
		View rootView = inflater.inflate(R.layout.recycler_view_frag, container, false);

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
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	@Override
	public void onResume() {
		super.onResume();

		myAdapter.notifyDataSetChanged();
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
}
