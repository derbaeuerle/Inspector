package de.hsrm.inspector.activities;

import java.util.Arrays;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import de.hsrm.inspector.R;

public class SettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.default_preferences);
		SharedPreferences preferences = getSharedPreferences(getString(R.string.configuration_preferences),
				MODE_PRIVATE);
		Object[] prefs = preferences.getAll().keySet().toArray();
		String[] keys = Arrays.asList(prefs).toArray(new String[prefs.length]);
		Arrays.sort(keys);
		for (String key : keys) {
			if (!key.contains(getString(R.string.configuration_initialized))) {
				Log.d("", key);
			}
		}
	}
}
