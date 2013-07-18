package de.hsrm.inspector.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.services.ServerService;

/**
 * {@link PreferenceActivity} to display all {@link Preference} objects of
 * configured {@link Gadget}. All preferences are stored inside the default
 * {@link SharedPreferences}.
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	@SuppressWarnings("static-access")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		if (intent.hasExtra(ServerService.EXTRA_PREFERENCES)) {
			String[] prefs = intent.getExtras().getStringArray(ServerService.EXTRA_PREFERENCES);
			for (String pref : prefs) {
				addPreferencesFromResource(getResources().getIdentifier(pref, "xml", getPackageName()));
			}
		}

		SharedPreferences shared = getPreferenceManager().getDefaultSharedPreferences(getApplicationContext());
		shared.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		Intent request = new Intent("de.inspector.intents");
		request.setData(Uri.parse("inspect://refresh/"));
		startService(request);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		String uri = "inspect://" + ServerService.CMD_PREFERENCE_CHANGED;
		Intent request = new Intent("de.inspector.intents");
		request.setData(Uri.parse(uri));
		request.putExtra(ServerService.DATA_CHANGED_PREFERENCE, key);
		startService(request);
	}
}
