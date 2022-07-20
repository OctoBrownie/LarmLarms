package com.LarmLarms.main;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.LarmLarms.BuildConfig;
import com.LarmLarms.R;

import org.jetbrains.annotations.Nullable;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for the user to change settings. Doesn't update anything until the user says to save.
 */

public class PrefsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
	/**
	 * Tag of the class for logging purposes.
	 */
	private final static String TAG = "PrefsActivity";

	/**
	 * Key for the app's settings when retrieving from Context.getSharedPreferences()
	 */
	public final static String PREFS_KEY = "com.LarmLarms.PREFERENCES";

	/**
	 * Within the shared preferences, this is the key for theme.
	 */
	public final static String PREF_THEME_KEY = "com.LarmLarms.PREFERENCE_THEME";
	/**
	 * Within the shared preferences, this is the key for using system settings for dark mode.
	 */
	public final static String PREF_SYSTEM_DARK_KEY = "com.LarmLarms.PREFERENCE_SYSTEM_DARK";
	/**
	 * Within the shared preferences, this is the key for dark mode (if the app doesn't follow
	 * system settings).
	 */
	public final static String PREF_DARK_MODE_KEY = "com.LarmLarms.PREFERENCE_DARK_MODE";

	/**
	 * The editor for the preferences.
	 */
	private SharedPreferences prefs;

	/**
	 * The theme before any preferences were changed.
	 */
	private int originalThemeId;

	/**
	 * The currently selected theme.
	 */
	private int themeId;

	/**
	 * Creates the activity (sets up all of the UI)
	 * @param savedInstanceState the previous state, if any
	 */
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		prefs = getSharedPreferences(PREFS_KEY, MODE_PRIVATE);
		themeId = prefs.getInt(PREF_THEME_KEY, R.style.AppTheme_Beach);
		setTheme(themeId);
		originalThemeId = themeId;

		setContentView(R.layout.activity_prefs);

		// theme spinner
		Spinner spinner = findViewById(R.id.themeSpinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
				R.array.themes_list, android.R.layout.simple_spinner_dropdown_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		switch (themeId) {
			case R.style.AppTheme_Beach:
				spinner.setSelection(getResources().getInteger(R.integer.theme_beach));
				break;
			case R.style.AppTheme_Candy:
				spinner.setSelection(getResources().getInteger(R.integer.theme_candy));
				break;
			case R.style.AppTheme_Mint:
				spinner.setSelection(getResources().getInteger(R.integer.theme_mint));
				break;
			case R.style.AppTheme_Grey:
				spinner.setSelection(getResources().getInteger(R.integer.theme_grey));
				break;
			default:
				if (BuildConfig.DEBUG) Log.e(TAG, "Unsupported app theme stored!");
				break;
		}

		spinner.setOnItemSelectedListener(this);

		// AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
	}

	/* ***************************************  Callbacks  ************************************ */

	/**
	 * Called when the back button of the menu is clicked
	 * @param view the back button
	 */
	public void backButtonClicked(View view) { finish(); }

	/**
	 * Called when the save button is clicked. Saves all of the preferences in one commit.
	 * @param view the save button
	 */
	public void saveButtonClicked(View view) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(PREF_THEME_KEY, themeId);
		editor.apply();

		if (themeId != originalThemeId) ((MainApplication) getApplication()).needsRestart = true;

		finish();
	}

	/**
	 * Spinner callback for when an item is selected
	 * @param parent the spinner that was selected
	 * @param view view within the AdapterView that was clicked
	 * @param position position of the view within the Adapter
	 * @param id row ID of the item selected
	 */
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		// for now, just theme selector
		String theme = (String) parent.getItemAtPosition(position);
		if (theme.equals(getString(R.string.theme_beach))) themeId = R.style.AppTheme_Beach;
		else if (theme.equals(getString(R.string.theme_candy))) themeId = R.style.AppTheme_Candy;
		else if (theme.equals(getString(R.string.theme_grey))) themeId = R.style.AppTheme_Grey;
		else if (theme.equals(getString(R.string.theme_mint))) themeId = R.style.AppTheme_Mint;
	}

	/**
	 * Spinner callback for when the spinner is closed and nothing is selected.
	 * @param parent the spinner that was selected
	 */
	@Override
	public void onNothingSelected(AdapterView<?> parent) {}
}
