package de.hsrm.inspector.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by dobae on 25.05.13.
 */
public class ScreenReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.e("", "Intent received: " + intent.toURI());
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			// Disable all gadgets without keep-alive setting.
			// Store state in another map.
		} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			// Restore saved state.
		}
	}
}
