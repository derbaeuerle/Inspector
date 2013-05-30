package de.hsrm.inspector.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import de.hsrm.inspector.R;
import de.hsrm.inspector.WebServer;

import java.util.List;

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
			mServer = new WebServer(context, context.getResources().openRawResource(R.raw.inspector));
			mServer.startThread();
		} else if (command.equals("destroy")) {
			mServer.stopThread();
		}
	}
}
