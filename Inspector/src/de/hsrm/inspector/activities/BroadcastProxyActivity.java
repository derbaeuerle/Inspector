package de.hsrm.inspector.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * {@link Activity} as proxy object. This activity gets registered on
 * {@link Intent#CATEGORY_BROWSABLE} {@link Intent} to receive intents from
 * local browser. Also a {@link ScreenReceiver} is initialized and bound to
 * {@link Intent#ACTION_SCREEN_OFF} and {@link Intent#ACTION_SCREEN_ON} intents.
 */
public class BroadcastProxyActivity extends Activity {

	@Override
	protected void onResume() {
		super.onResume();
		Intent i = getIntent();
		Uri data = i.getData();
		if (data != null) {
			Log.d("PROXY", "received: " + i.getDataString());
			String uri = data.toString();
			uri = uri.replace("inspector://", "inspect://");
			Intent request = new Intent("de.inspector.intents");
			request.setData(Uri.parse(uri));
			startService(request);
		}
		finish();
	}

}