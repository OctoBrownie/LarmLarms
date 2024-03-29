package com.larmlarms.editor;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormatSymbols;
import java.util.Arrays;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.larmlarms.BuildConfig;

/**
 * Dialog fragment for ListableEditor. Creates a dialog either for the days of the week or for the
 * months of the year.
 */
public class EditorDialogFrag extends DialogFragment implements
		DialogInterface.OnMultiChoiceClickListener, DialogInterface.OnClickListener {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "EditorDialogFrag";

	/**
	 * A listener for the dialog. Only receives the positive button click
	 */
	@Nullable
	private final DialogCloseListener listener;

	/**
	 * The days of the week that have been selected
	 */
	@NotNull
	private final boolean[] selected;

	private final boolean isDays;

	/**
	 * Creates a new dialog for a recycler view item.
	 * @param l listener for when the dialog closes
	 * @param selected days currently repeated on
	 */
	EditorDialogFrag(@Nullable DialogCloseListener l, boolean isDays, @NotNull boolean[] selected) {
		listener = l;
		this.isDays = isDays;
		this.selected = selected;
	}

	/**
	 * Called when the dialog is being created. Initializes the items within the dialog.
	 * @param savedInstanceState the previous instance state
	 * @return the fully created dialog
	 */
	@NotNull @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) throws IllegalStateException {
		Activity a = getActivity();
		if (a == null) {
			if (BuildConfig.DEBUG) Log.i(TAG, "There wasn't an activity associated with this.");
			throw new IllegalStateException("There was no activity to make a dialog for.");
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(a);

		String[] items;

		if (isDays) {
			items = (new DateFormatSymbols()).getWeekdays();
			items = Arrays.copyOfRange(items, 1, 8);
		}
		else {
			items = (new DateFormatSymbols()).getMonths();
		}
		builder.setMultiChoiceItems(items, selected, this);

		builder.setPositiveButton(android.R.string.ok, this);
		builder.setNegativeButton(android.R.string.cancel, this);
		return builder.create();
	}

	/**
	 * Onclick callback for the positive/negative buttons on the dialog. Propagates the event back
	 * to the dialog listener.
	 * @param dialog the dialog whose buttons were clicked
	 * @param which which button was clicked
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (listener != null) listener.onDialogClose(isDays, which);
	}

	/**
	 * Onclick callback for the checkboxes on the dialog. Changes the respective entry in selected
	 * (which will affect the original Alarm's fields)
	 * @param dialog the dialog whose checkboxes wer clicked
	 * @param which which checkbox was clicked
	 * @param isChecked whether the checkbox was checked or not
	 */
	@Override
	public void onClick(DialogInterface dialog, int which, boolean isChecked) {
		selected[which] = isChecked;
	}

	/**
	 * Interface allowing classes to listen for when the dialog closes.
	 */
	public interface DialogCloseListener {
		/**
		 * Callback for when the dialog closes
		 * @param isDays whether it was a days dialog (true) or a months dialog (false)
		 * @param which which button was clicked
		 */
		void onDialogClose(boolean isDays, int which);
	}
}
