package de.hsrm.audioapi;

import android.content.SharedPreferences;
import android.content.res.Resources;
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
		mServer = new WebServer(getApplicationContext(), getResources().openRawResource(R.raw.inspector_config));
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Resources res = getResources();
		Log.d("PREF", key);
		if (key.equals(res.getString(R.string.pref_activate_support))) {
			boolean activate = sharedPreferences.getBoolean(res.getString(R.string.pref_activate_support), false);
			Log.d("", activate + "");
			if (activate) {
				mServer.startThread();
			} else {
				mServer.stopThread();
			}
		}
	}

}
