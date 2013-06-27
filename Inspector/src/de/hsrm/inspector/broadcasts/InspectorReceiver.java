package de.hsrm.inspector.broadcasts;

import java.util.concurrent.ConcurrentHashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.util.Log;
import de.hsrm.inspector.R;
import de.hsrm.inspector.WebServer;
import de.hsrm.inspector.activities.SettingsActivity;
import de.hsrm.inspector.gadgets.intf.Gadget;

/**
 * Created by dobae on 25.05.13.
 */
public class InspectorReceiver extends BroadcastReceiver {

	private static WebServer mServer;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.e("", "Intent received: " + intent.toURI());
		String command = Uri.parse(intent.toURI()).getHost();
		if (command.equals("init")) {
			if (mServer == null) {
				mServer = new WebServer(context);
			}
			mServer.startThread();
		} else if (command.equals("destroy")) {
			if (mServer != null) {
				mServer.stopThread();
			}
		} else if (command.equals("settings")) {
			if (mServer == null) {
				mServer = new WebServer(context);
			}
			mServer.setConfiguration(checkSharedPreferences(context));
			Intent i = new Intent(context, SettingsActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(i);
		}
	}

	private ConcurrentHashMap<String, Gadget> checkSharedPreferences(Context context) {
		ConcurrentHashMap<String, Gadget> config = mServer.getConfiguration();
		SharedPreferences preferences = context.getSharedPreferences(
				context.getString(R.string.configuration_preferences), Context.MODE_PRIVATE);
		synchronized (config) {
			for (Gadget g : config.values()) {
				if (!preferences.contains(g.getIdentifier() + ":"
						+ context.getString(R.string.configuration_initialized))) {
					// Preferences for gadget not initialized.
					Editor e = preferences.edit();
					e.putBoolean(g.getIdentifier() + ":" + context.getString(R.string.configuration_keep_alive),
							g.isKeepAlive());
					e.putLong(g.getIdentifier() + ":" + context.getString(R.string.configuration_timeout),
							g.getTimeout());
					e.putBoolean(g.getIdentifier() + ":" + context.getString(R.string.configuration_initialized), true);
					e.commit();
				} else {
					g.setKeepAlive(preferences.getBoolean(
							g.getIdentifier() + ":" + context.getString(R.string.configuration_keep_alive),
							g.isKeepAlive()));
					g.setTimeout(preferences.getLong(
							g.getIdentifier() + ":" + context.getString(R.string.configuration_timeout), g.getTimeout()));
				}
			}
		}
		return config;
	}
}
