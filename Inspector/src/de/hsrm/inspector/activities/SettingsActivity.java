package de.hsrm.inspector.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import de.hsrm.inspector.services.HttpService;

public class SettingsActivity extends PreferenceActivity {

	// private static final String INITIALIZED = "initialited";

	@SuppressWarnings("static-access")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		if (intent.hasExtra(HttpService.EXTRA_PREFERENCES)) {
			String[] prefs = intent.getExtras().getStringArray(HttpService.EXTRA_PREFERENCES);
			for (String pref : prefs) {
				addPreferencesFromResource(getResources().getIdentifier(pref, "xml", getPackageName()));
			}
		}

		SharedPreferences shared = getPreferenceManager().getDefaultSharedPreferences(getApplicationContext());
		for (String key : shared.getAll().keySet()) {
			Log.d("KEYS", key);
		}
		// if (!shared.contains(INITIALIZED)) {
		// Editor editor = shared.edit();
		// iterateRoot(getPreferenceScreen(), shared, editor);
		// editor.putBoolean(INITIALIZED, true);
		// editor.commit();
		// }
	}

	// private void iterateRoot(PreferenceScreen root, SharedPreferences shared,
	// Editor editor) {
	// for (int i = 0; i < root.getPreferenceCount(); i++) {
	// Preference pref = root.getPreference(i);
	// if (pref instanceof PreferenceCategory) {
	// iterateCategory((PreferenceCategory) pref, shared, editor);
	// }
	// }
	// }
	//
	// private void iterateCategory(PreferenceCategory category,
	// SharedPreferences shared, Editor editor) {
	// for (int p = 0; p < category.getPreferenceCount(); p++) {
	// Preference pref = category.getPreference(p);
	// if (!shared.contains(pref.getKey())) {
	// Log.d("PREF", "missing key in shared: " + pref.getKey());
	// copyValue(pref, pref.getSharedPreferences(), editor);
	// }
	// }
	// }
	//
	// private void copyValue(Preference pref, SharedPreferences shared, Editor
	// editor) {
	// SharedPreferences prefs = pref.getSharedPreferences();
	// try {
	// String value = prefs.getString(pref.getKey(), "");
	// editor.putString(pref.getKey(), value);
	// } catch (ClassCastException e) {
	// try {
	// boolean value = prefs.getBoolean(pref.getKey(), false);
	// editor.putBoolean(pref.getKey(), value);
	// } catch (ClassCastException e1) {
	// try {
	// float value = prefs.getFloat(pref.getKey(), 0f);
	// editor.putFloat(pref.getKey(), value);
	// } catch (ClassCastException e2) {
	// try {
	// int value = prefs.getInt(pref.getKey(), 0);
	// editor.putInt(pref.getKey(), value);
	// } catch (ClassCastException e3) {
	// try {
	// long value = prefs.getLong(pref.getKey(), 0);
	// editor.putLong(pref.getKey(), value);
	// } catch (ClassCastException e4) {
	// }
	// }
	// }
	// }
	// }
	// }

	@SuppressWarnings("static-access")
	@Override
	protected void onPause() {
		super.onPause();
		SharedPreferences prefs = getPreferenceManager().getDefaultSharedPreferences(getApplicationContext());
		for (String key : prefs.getAll().keySet()) {
			if (key.equals("audio:permission")) {
				Log.d("PAUSE", key + ": " + prefs.getString(key, ""));
			}
		}

		Intent request = new Intent("de.inspector.intents");
		request.setData(Uri.parse("inspect://refresh/"));
		startService(request);
	}
}
