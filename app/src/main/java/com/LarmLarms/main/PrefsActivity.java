package com.LarmLarms.main;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;

import com.LarmLarms.BuildConfig;
import com.LarmLarms.R;

import org.jetbrains.annotations.Nullable;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

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
	 * Within the shared preferences, this is the key for theme, stored as an integer which is the
	 * theme id.
	 */
	public final static String PREF_THEME_KEY = "com.LarmLarms.PREFERENCE_THEME";
	/**
	 * Within the shared preferences, this is the key for using system settings for dark mode, stored
	 * as a boolean. True translates to following the system-wide dark/light mode.
	 */
	public final static String PREF_SYSTEM_DARK_KEY = "com.LarmLarms.PREFERENCE_USE_SYSTEM_DARK";
	/**
	 * Within the shared preferences, this is the key for dark mode (if the app doesn't follow
	 * system settings), stored as a boolean. True translates to dark mode.
	 */
	public final static String PREF_DARK_MODE_KEY = "com.LarmLarms.PREFERENCE_DARK_MODE";
	/**
	 * Stores whether to have the button menu on the top or the bottom for the preferences activity
	 * and listable editor. True translates to menu on the top.
	 */
	public final static String PREF_MENU_POS = "com.LarmLarms.PREFERENCE_MENU_POSITION";

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
	 * Stores whether to use the system dark mode or not.
	 */
	private boolean useSystemDark;
	/**
	 * Assuming useSystemDark is false, stores whether to use dark mode or not.
	 */
	private boolean darkModeOverride;
	/**
	 * Stores whether the user wants the menu position to be on the top or bottom.
	 */
	private boolean menuPosTop;

	/**
	 * Creates the activity (sets up all of the UI)
	 * @param savedInstanceState the previous state, if any
	 */
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		applyPrefsStyle(this);
		setContentView(R.layout.activity_prefs);
		applyPrefsUI(this);

		// theme spinner
		Spinner spinner = findViewById(R.id.themeSpinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
				R.array.themes_list, android.R.layout.simple_spinner_dropdown_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		prefs = getSharedPreferences(PREFS_KEY, MODE_PRIVATE);
		themeId = prefs.getInt(PREF_THEME_KEY, R.style.AppTheme_Beach);
		originalThemeId = themeId;
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

		Switch s = findViewById(R.id.systemDarkSwitch);
		useSystemDark = prefs.getBoolean(PREF_SYSTEM_DARK_KEY, true);
		s.setChecked(useSystemDark);

		s = findViewById(R.id.darkOverrideSwitch);
		darkModeOverride = prefs.getBoolean(PREF_DARK_MODE_KEY, true);
		s.setChecked(darkModeOverride);

		s = findViewById(R.id.menuPlacementSwitch);
		menuPosTop = prefs.getBoolean(PREF_MENU_POS, false);
		s.setChecked(menuPosTop);
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
		editor.putBoolean(PREF_SYSTEM_DARK_KEY, useSystemDark);
		editor.putBoolean(PREF_DARK_MODE_KEY, darkModeOverride);
		editor.putBoolean(PREF_MENU_POS, menuPosTop);
		editor.apply();

		int currNightMode = AppCompatDelegate.getDefaultNightMode();
		boolean nightModeOk;
		if (useSystemDark) nightModeOk = currNightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
		else {
			if (darkModeOverride) nightModeOk = currNightMode == AppCompatDelegate.MODE_NIGHT_YES;
			else nightModeOk = currNightMode == AppCompatDelegate.MODE_NIGHT_NO;
		}

		if (themeId != originalThemeId || !nightModeOk)
			((MainApplication) getApplication()).needsRestart = true;

		finish();
	}

	/**
	 * Called when a switch is flipped
	 * @param view the switch that was flipped
	 */
	public void onSwitchFlipped(View view) {
		int id = view.getId();
		boolean checked = ((Switch) view).isChecked();
		switch(id) {
			case R.id.systemDarkSwitch:
				useSystemDark = checked;
				break;
			case R.id.darkOverrideSwitch:
				darkModeOverride = checked;
				break;
			case R.id.menuPlacementSwitch:
				menuPosTop = checked;
				break;
			default:
				if (BuildConfig.DEBUG) Log.e(TAG, "Unknown switch flipped!");
				break;

		}
	}

	/* ************************************  Spinner Callbacks  ********************************** */

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

	/* **********************************  Other Methods  *************************************** */

	/**
	 * Helper method that applies all of the style-related preferences to components for them.
	 * Includes: theme and night mode (system and forced). Should be called BEFORE inflating any
	 * views.
	 * @param c the context to apply preferences to
	 */
	public static void applyPrefsStyle(Context c) {
		SharedPreferences prefs = c.getSharedPreferences(PREFS_KEY, MODE_PRIVATE);
		c.setTheme(prefs.getInt(PrefsActivity.PREF_THEME_KEY, R.style.AppTheme_Beach));

		if (prefs.getBoolean(PREF_SYSTEM_DARK_KEY, true)) {
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
		}
		else {
			if (prefs.getBoolean(PREF_DARK_MODE_KEY, true)) {
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
			}
			else {
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
			}
		}
	}

	/**
	 * Helper method that applies all UI related preferences to components for them. Applies the
	 * menu placement. Should be called AFTER inflating views.
	 * @param a the activity to apply preferences to
	 */
	public static void applyPrefsUI(Activity a) {
		SharedPreferences prefs = a.getSharedPreferences(PREFS_KEY, MODE_PRIVATE);

		View topMenu = a.findViewById(R.id.topMenu);
		View bottomMenu =  a.findViewById(R.id.bottomMenu);
		if (topMenu != null && bottomMenu != null) {
			if (prefs.getBoolean(PREF_MENU_POS, false)) {
				topMenu.setVisibility(View.VISIBLE);
				bottomMenu.setVisibility(View.GONE);
			}
			else {
				topMenu.setVisibility(View.GONE);
				bottomMenu.setVisibility(View.VISIBLE);
			}
		}
	}
}
