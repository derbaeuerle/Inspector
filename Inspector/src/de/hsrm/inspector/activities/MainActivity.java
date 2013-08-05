package de.hsrm.inspector.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import de.hsrm.inspector.R;

/**
 * Created by dobae on 29.05.13.
 */
public class MainActivity extends Activity implements OnClickListener {

	private static final boolean DEBUG = true;

	private Button mSendIntent, mStopServer, mOpenSettings;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mSendIntent = (Button) findViewById(R.id.startServer);
		mSendIntent.setOnClickListener(this);

		mStopServer = (Button) findViewById(R.id.stopServer);
		mStopServer.setOnClickListener(this);

		mOpenSettings = (Button) findViewById(R.id.openSettings);
		mOpenSettings.setOnClickListener(this);
		if (!DEBUG) {
			mOpenSettings.performClick();
			finish();
		}
	}

	private void send(View v) {
		Intent i = new Intent();
		i.setAction("android.intent.action.VIEW");
		i.addCategory("android.intent.category.BROWSABLE");
		if (v.equals(mSendIntent)) {
			i.setData(Uri.parse("inspector://init/"));
			startActivity(i);
		} else if (v.equals(mOpenSettings)) {
			i.setData(Uri.parse("inspector://settings/"));
			startActivity(i);
		} else if (v.equals(mStopServer)) {
			i.setData(Uri.parse("inspector://destroy/"));
			startActivity(i);
		}
	}

	@Override
	public void onClick(View v) {
		send(v);
	}
}