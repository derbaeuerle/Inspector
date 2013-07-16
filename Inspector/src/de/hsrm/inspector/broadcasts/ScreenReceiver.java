package de.hsrm.inspector.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * {@link BroadcastReceiver} to receive screen events of device. The
 * {@link #onReceive(Context, Intent)} method translates android intents into
 * intents of inspector namespace and dispatches them.
 */
public class ScreenReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.e("", "Intent received: " + intent.toURI());
		Intent request = new Intent("de.inspector.intents");
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			String uri = "inspect://lock";
			request.setData(Uri.parse(uri));
		} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			String uri = "inspect://unlock";
			request.setData(Uri.parse(uri));
		}
		if (request.getData() != null) {
			context.startService(request);
		}
	}
}
