package de.hsrm.audioapi;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import de.inspector.hsrm.WebServer;

public class MainActivity extends Activity implements OnClickListener {

	private WebServer mServer;
	private Button mStopButton, mSendInteger;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mStopButton = (Button) findViewById(R.id.stop_server);
		mStopButton.setOnClickListener(this);
		mSendInteger = (Button) findViewById(R.id.send_integer);
		mSendInteger.setOnClickListener(this);

		mServer = new WebServer(getApplicationContext(), getResources().openRawResource(R.raw.inspector));
		mServer.startThread();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mServer != null) {
			mServer.startThread();
		}
	}

	@Override
	public void onClick(View v) {
		if (v.equals(mStopButton)) {
			mServer.stopThread();
		}
	}
}
