package com.apps.LarmLarms;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * Dialog fragment for RecyclerView items. Different dialogs open depending on whether the Listable
 * is an alarm or a folder.
 */
public class RecyclerDialogFrag extends DialogFragment {
	/**
	 * A listener for the dialog.
	 */
	private DialogInterface.OnClickListener listener;

	/**
	 * Stores whether the dialog is being opened for an alarm or folder.
	 */
	private boolean isAlarm;

	/**
	 * Creates a new dialog for a recycler view item.
	 * @param l a listener for the thing
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
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
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
