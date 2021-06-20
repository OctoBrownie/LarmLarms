package com.apps.LarmLarms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {
	private final static String TAG = "MainActivity";

	// tag for intents carrying alarms
	final static String EXTRA_LISTABLE = "com.apps.AlarmsButBetter.ALARM";
	final static String EXTRA_LISTABLE_INDEX = "com.apps.AlarmsButBetter.ALARM_INDEX";
	final static String EXTRA_REQ_ID = "com.apps.AlarmsButBetter.REQ_ID";

	// used when calling AlarmCreator, so we know the activity results came back from an AlarmCreator
	final static int REQ_NEW_ALARM = 0;
	final static int REQ_EDIT_ALARM = 1;
	final static int REQ_NEW_FOLDER = 2;
	final static int REQ_EDIT_FOLDER = 3;

	// some important views
	private RecyclerViewFrag myRecyclerFrag;
	private View noAlarmsText;
	private View fragContainer;

	/* ********************************* Lifecycle Methods ********************************* */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		noAlarmsText = findViewById(R.id.no_alarms_text);
		fragContainer = findViewById(R.id.frag_frame);

		// always need to reinflate the frag
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(R.id.frag_frame, RecyclerViewFrag.class, null, "recycler_frag");
		trans.commitNow();

		myRecyclerFrag = (RecyclerViewFrag) getSupportFragmentManager().findFragmentByTag("recycler_frag");

		if (myRecyclerFrag.isDataEmpty()) { hideFrag(); }

		Log.i(TAG, "Activity created successfully.");
	}

	@Override
	protected void onResume() {
		super.onResume();

		// update all alarms in myRecyclerFrag to reflect current settings
		myRecyclerFrag.refreshAlarms();
	}

	/* ************************************  Callbacks  ************************************** */

	// onClick callback for the + Alarm button (id: addNewAlarmButton)
	public void addNewAlarm(View view) {
		// start AlarmCreator activity
		Intent intent = new Intent(this, ListableEditorActivity.class);
		intent.putExtra(EXTRA_REQ_ID, REQ_NEW_ALARM);

		// noinspection deprecation
		startActivityForResult(intent, REQ_NEW_ALARM);
	}

	// onClick callback for the add new folder button
	public void addNewFolder(View view) {
		// start AlarmCreator activity
		Intent intent = new Intent(this, ListableEditorActivity.class);
		intent.putExtra(EXTRA_REQ_ID, REQ_NEW_FOLDER);

		// noinspection deprecation
		startActivityForResult(intent, REQ_NEW_FOLDER);
	}

	/**
	 * Callback for when Activities finish with a result. Deals with the output of each Activity.
	 *
	 * For AlarmCreators: adds the newly-made or edited alarm into the RecyclerView
	 *
	 * @param requestCode the request code the closed Activity was started with
	 * @param resultCode the result code the closed Activity returned
	 * @param data the intent returned by the closed Activity
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO: if this isn't true for some other activity we come back from, change
		if (resultCode == RESULT_CANCELED) {
			Log.i(TAG, "Action cancelled.");
			return;
		}
		if (resultCode != RESULT_OK || data == null || data.getStringExtra(EXTRA_LISTABLE) == null) {
			Log.e(TAG, "Data from ListableEditorActivity was invalid.");
			return;
		}

		showFrag();

		switch(requestCode) {
			case REQ_NEW_ALARM:
			case REQ_EDIT_ALARM:
			case REQ_NEW_FOLDER:
			case REQ_EDIT_FOLDER:
				myRecyclerFrag.onListableCreatorResult(requestCode, data);
				break;
			default:
				Log.e(TAG, "Unknown request code returned.");
		}

	}

	/**
	 * Creates a floating context menu for the passed view. Only used for Listables within the
	 * RecyclerView.
	 * @param menu the menu to inflate into
	 * @param v the view to inflate a menu for
	 * @param menuInfo extra info about the menu
	 */
	// TODO: on my phone at least, context menu looks like a popup menu
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.listable_menu, menu);
	}

	/* ************************************  Other Methods  ************************************* */

	private void showFrag() {
		fragContainer.setVisibility(View.VISIBLE);
		noAlarmsText.setVisibility(View.GONE);
		Log.i("RecyclerViewFragment", "Recycler view shown.");
	}

	private void hideFrag() {
		fragContainer.setVisibility(View.GONE);
		noAlarmsText.setVisibility(View.VISIBLE);
		Log.i("RecyclerViewFragment", "Recycler view hidden.");
	}

	void editExistingListable(final Listable listable, final int index) {
		// start new activity (AlarmCreator)
		Intent intent = new Intent(this, ListableEditorActivity.class);

		// add extras (Listable, index, req id)
		intent.putExtra(EXTRA_LISTABLE, listable.toEditString());
		intent.putExtra(EXTRA_LISTABLE_INDEX, index);

		int req;
		if (listable.isAlarm()) { req = REQ_EDIT_ALARM; }
		else { req = REQ_EDIT_FOLDER; }

		intent.putExtra(EXTRA_REQ_ID, req);

		startActivityForResult(intent, req);
	}
}