package de.hsrm.inspector.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import de.hsrm.inspector.R;

public class SettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.default_preferences);
	}
}
