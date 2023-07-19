package com.larmlarms.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.larmlarms.BuildConfig;
import com.larmlarms.Constants;
import com.larmlarms.R;
import com.larmlarms.data.AlarmGroup;
import com.larmlarms.editor.EditorActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shows just a single folder that the user can scroll through. Must be called with an intent that
 * gives the path of the current folder to use in the form of a string in EXTRA_PATH.
 */
public class FolderViewActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {
	/**
	 * Tag of the class for logging purposes.
	 */
	private static final String TAG = "FolderView";
	
	/**
	 * The TextView that is shown when the list is empty.
	 */
	private View noAlarmsText;
	/**
	 * The FrameView that contains the recycler view fragment
	 */
	private View fragContainer;
	/**
	 * Handle to the current folder.
	 */
	private AlarmGroup currFolder;

	/**
	 * Current path we're editing
	 */
	private String currPath;

	// ********************************* Lifecycle Methods *********************************

	/**
	 * Called when the activity is being created. Caches views to class fields.
	 * @param savedInstanceState the previously saved instance state
	 */
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PrefsActivity.applyPrefsStyle(this);
		setContentView(R.layout.activity_folder_view);
		PrefsActivity.applyPrefsUI(this);

		noAlarmsText = findViewById(R.id.noAlarmsText);
		fragContainer = findViewById(R.id.fragFrame);

		currPath = getIntent().getStringExtra(Constants.EXTRA_PATH);

		int[] buttons = {R.id.backButton, R.id.editButton,
				R.id.addAlarmButton, R.id.addFolderButton, R.id.settingsButton};
		for (int i : buttons) {
			ImageButton b = findViewById(i);
			b.setOnClickListener(this);
			b.setOnLongClickListener(this);
		}

		((TextView) findViewById(R.id.titleText)).setText(currPath);

		Bundle b = new Bundle();
		b.putString(Constants.EXTRA_PATH, currPath);

		// always need to reinflate the frag
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(R.id.fragFrame, RecyclerViewFrag.class, b, "recycler_frag");
		trans.commitNow();
	}

	/**
	 * Called when the activity is resuming. Checks if we need to restart to apply a new theme.
	 */
	@Override
	protected void onResume() {
		super.onResume();

		MainApplication app = (MainApplication) getApplication();
		if (app.needsRestart) {
			app.needsRestart = false;
			recreate();
		}

		currFolder = ((MainApplication) getApplication()).rootFolder.getFolder(currPath);
		
		if (currFolder == null) {
			if (BuildConfig.DEBUG) Log.e(TAG, "The folder couldn't be found.");
			finish();
		}
		else if (currFolder.size() != 1) showFrag();
		else hideFrag();
	}

	// ************************************  Callbacks  **************************************

	/**
	 * Callback for clicks. Starts a new activity for all current buttons.
	 * @param view the view that was clicked
	 */
	@Override
	public void onClick(@NotNull View view) {
		int id = view.getId();
		if (id == R.id.backButton) finish();
		else if (id == R.id.editButton) editFolder();
		else if (id == R.id.addAlarmButton) addNewAlarm(this);
		else if (id == R.id.addFolderButton) addNewFolder(this);
		else if (id == R.id.settingsButton) openSettings(this);
	}

	/**
	 * Callback for long clicks. Shows descriptions of the buttons that are long clicked
	 * @param view the view that was long clicked
	 * @return whether this consumed the long click (always returns true)
	 */
	@Override
	public boolean onLongClick(@NotNull View view) {
		int id = view.getId();
		if (id == R.id.addAlarmButton)
			Toast.makeText(this, R.string.main_alarm_description, Toast.LENGTH_SHORT).show();
		else if (id == R.id.addFolderButton)
			Toast.makeText(this, R.string.main_folder_description, Toast.LENGTH_SHORT).show();
		else if (id == R.id.settingsButton)
			Toast.makeText(this, R.string.main_settings_description, Toast.LENGTH_SHORT).show();

		return true;
	}

	/**
	 * Starts an activity to a new alarm (usually bound to the + button).
	 * @param context the current context (usually an activity)
	 */
	static void addNewAlarm(Context context) {
		// start editor
		Intent intent = new Intent(context, EditorActivity.class);
		intent.setAction(Constants.ACTION_CREATE_ALARM);

		context.startActivity(intent);
	}

	/**
	 * Starts an activity to add a new folder (usually bound to the folder button).
	 * @param context the current context (usually an activity)
	 */
	static void addNewFolder(Context context) {
		// start editor
		Intent intent = new Intent(context, EditorActivity.class);
		intent.setAction(Constants.ACTION_CREATE_FOLDER);

		context.startActivity(intent);
	}

	/**
	 * Starts an activity to change current settings (usually bound to the settings button).
	 * @param context the current context (usually an activity)
	 */
	static void openSettings(Context context) {
		// start the preferences
		Intent intent = new Intent(context, PrefsActivity.class);
		context.startActivity(intent);
	}

	/**
	 * Edits the current folder (usually bound to the edit button).
	 */
	private void editFolder() {
		Intent intent = new Intent(this, EditorActivity.class);
		intent.setAction(Constants.ACTION_EDIT_FOLDER);
		intent.putExtra(Constants.EXTRA_ITEM_INFO, currFolder.getInfo());

		startActivity(intent);
	}

	// ************************************  Other Methods  *************************************

	/**
	 * Shows the recycler view fragment and hides the noAlarmsText.
	 */
	private void showFrag() {
		fragContainer.setVisibility(View.VISIBLE);
		noAlarmsText.setVisibility(View.GONE);
	}

	/**
	 * Hides the recycler view fragment and shows the noAlarmsText.
	 */
	private void hideFrag() {
		fragContainer.setVisibility(View.GONE);
		noAlarmsText.setVisibility(View.VISIBLE);
	}
}
