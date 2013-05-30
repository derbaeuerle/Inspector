package de.hsrm.inspector.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import de.hsrm.inspector.WebServer;

/**
 * Created by dobae on 25.05.13.
 */
public class ServiceReceiver extends BroadcastReceiver {

	private WebServer mServer;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.e("", "Intent received: " + intent.toURI());
		String command = Uri.parse(intent.toURI()).getHost();
		if (command.equals("init")) {
			// TODO: Inject configuration file.
			mServer = new WebServer(context, null);
			mServer.startThread();
		} else if (command.equals("destroy")) {
			mServer.stopThread();
		}
	}
}
