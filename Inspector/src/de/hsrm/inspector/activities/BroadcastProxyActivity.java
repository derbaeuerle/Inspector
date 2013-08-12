package de.hsrm.inspector.activities;

import de.hsrm.inspector.services.ServerService;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * {@link Activity} as proxy object. This activity gets registered on
 * {@link Intent#CATEGORY_BROWSABLE} {@link Intent} to receive intents from
 * local browser.
 */
public class BroadcastProxyActivity extends Activity {

	/**
	 * Parses calling {@link Intent} and dispatches new {@link Intent} to
	 * {@link ServerService}.
	 * 
	 * @see android.app.Activity#onResume()
	 */
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