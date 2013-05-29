package de.inspector.hsrm.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import de.inspector.hsrm.R;
import de.inspector.hsrm.WebServer;

import java.util.List;

/**
 * Created by dobae on 25.05.13.
 */
public class ServiceReceiver extends BroadcastReceiver {

	private WebServer mServer;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.e("", "Intent received: " + intent.toURI());
		Uri uri = Uri.parse(intent.toURI());
		final List<String> paths = uri.getPathSegments();

		String command = "";
		if (paths.contains("init")) {
			mServer = new WebServer(context, context.getResources().openRawResource(R.raw.inspector));
			mServer.startThread();
		} else if (paths.contains("destroy")) {
			mServer.stopThread();
		}
	}
}
