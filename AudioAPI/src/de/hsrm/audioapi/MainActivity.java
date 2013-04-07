package de.hsrm.audioapi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import de.inspector.hsrm.WebServer;

public class MainActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

	private WebServer mServer;
	private SharedPreferences mPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.prefs);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mPreferences.registerOnSharedPreferenceChangeListener(this);
		mServer = new WebServer(getApplicationContext(), getResources().openRawResource(R.raw.inspector));
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d("PREF", key);
	}

}
