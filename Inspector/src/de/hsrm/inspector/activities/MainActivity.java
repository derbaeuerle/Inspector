package de.hsrm.inspector.activities;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import de.hsrm.inspector.R;

/**
 * Dummy main {@link Activity} in debug mode. If {@link #DEBUG} is set to
 * <code>false</code> {@link #onCreate(Bundle)} will start
 * {@link SettingsActivity}.
 */
public class MainActivity extends Activity implements OnClickListener {

	/**
	 * Static value if {@link Application} is in debug mode.
	 */
	private static final boolean DEBUG = true;

	/**
	 * {@link Button} objects of {@link Activity}.
	 */
	private Button mSendIntent, mStopServer, mOpenSettings;

	/**
	 * Gets all {@link Button} instances and starts {@link SettingsActivity} if
	 * {@link #DEBUG} is set to <code>false</code>.
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
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

	/**
	 * Sends an {@link Intent} based on calling {@link Button}.
	 * 
	 * @param v
	 *            {@link View}
	 */
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

	/**
	 * Calls {@link #send(View)} method.
	 * 
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 * @param v
	 *            {@link View}
	 */
	@Override
	public void onClick(View v) {
		send(v);
	}
}