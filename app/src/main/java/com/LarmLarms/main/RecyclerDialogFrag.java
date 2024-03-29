package com.larmlarms.main;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.larmlarms.BuildConfig;
import com.larmlarms.R;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.jetbrains.annotations.NotNull;

/**
 * Dialog fragment for RecyclerView items. Different dialogs open depending on whether the Listable
 * is an alarm or a folder.
 */
public class RecyclerDialogFrag extends DialogFragment {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "RecyclerDialogFrag";

	/**
	 * A listener for the dialog.
	 */
	private final DialogInterface.OnClickListener listener;

	/**
	 * Stores whether the dialog is being opened for an alarm or folder.
	 */
	private final boolean isAlarm;

	/**
	 * Creates a new dialog for a recycler view item.
	 * @param l a listener for the dialog
	 * @param isAlarm whether the clicked item was an alarm or a folder
	 */
	RecyclerDialogFrag(DialogInterface.OnClickListener l, boolean isAlarm) {
		listener = l;
		this.isAlarm = isAlarm;
	}

	/**
	 * Called when the dialog is being created. Initializes the items within the dialog.
	 * @param savedInstanceState the previous instance state
	 * @return the fully created dialog
	 */
	@Override @NotNull
	public Dialog onCreateDialog(Bundle savedInstanceState) throws IllegalStateException {
		Activity a = getActivity();
		if (a == null) {
			if (BuildConfig.DEBUG) Log.i(TAG, "There wasn't an activity associated with this.");
			throw new IllegalStateException("There was no activity to make a dialog for.");
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		if (isAlarm) {
			builder.setItems(R.array.listable_menu_alarms, listener);
		}
		else {
			builder.setItems(R.array.listable_menu_folders, listener);
		}
		return builder.create();
	}
}
