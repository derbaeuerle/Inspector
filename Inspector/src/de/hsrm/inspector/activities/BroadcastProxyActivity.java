package de.hsrm.inspector.activities;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import de.hsrm.inspector.broadcasts.ScreenReceiver;

/**
 * Created by dobae on 29.05.13.
 */
public class BroadcastProxyActivity extends Activity {

	private static AtomicBoolean mInitialized = new AtomicBoolean(false);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!mInitialized.get()) {
			try {
				// Binding ScreenReceiver to screen events.
				IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
				filter.addAction(Intent.ACTION_SCREEN_OFF);
				BroadcastReceiver mReceiver = new ScreenReceiver();
				registerReceiver(mReceiver, filter);
				mInitialized.set(true);
			} catch (Exception e) {

			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Intent i = getIntent();
		Log.d("PROXY", "received: " + i.getDataString());
		Uri data = i.getData();
		if (data != null) {
			String uri = data.toString();
			uri = uri.replace("inspector://", "inspect://");
			Intent request = new Intent("de.inspector.intents");
			request.setData(Uri.parse(uri));
			startService(request);
		}
		finish();
	}

}