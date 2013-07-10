package de.hsrm.inspector.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import de.hsrm.inspector.services.HttpService;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

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
		shared.registerOnSharedPreferenceChangeListener(this);
	}

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

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		String uri = "inspect://" + HttpService.CMD_PREFERENCE_CHANGED;
		Intent request = new Intent("de.inspector.intents");
		request.setData(Uri.parse(uri));
		request.putExtra(HttpService.DATA_CHANGED_PREFERENCE, key);
		startService(request);
	}
}
