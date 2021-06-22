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
	private DialogInterface.OnClickListener listener;
	private boolean isAlarm;

	RecyclerDialogFrag(DialogInterface.OnClickListener l, boolean isAlarm) {
		listener = l;
		this.isAlarm = isAlarm;
	}

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
