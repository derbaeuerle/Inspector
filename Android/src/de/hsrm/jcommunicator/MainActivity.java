package de.hsrm.jcommunicator;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import de.hsrm.jcommunicator.services.WebService;

public class MainActivity extends Activity implements OnClickListener {

	private Button mStart, mSend, mStop;
	private TextView mStatus;

	private boolean mServiceConnected = false;
	private volatile WebService mService;
	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			mService = ((WebService.WebBinder) binder).getService();
			mServiceConnected = true;
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
			mServiceConnected = false;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mStart = (Button) findViewById(R.id.start_service);
		mSend = (Button) findViewById(R.id.send_request);
		mStop = (Button) findViewById(R.id.stop_service);
		mStart.setOnClickListener(this);
		mSend.setOnClickListener(this);
		mStop.setOnClickListener(this);

		mStatus = (TextView) findViewById(R.id.status);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Intent i = new Intent(this, WebService.class);
		bindService(i, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// if (mServiceConnected) {
		// mService.stopServer();
		// }
		unbindService(mConnection);
	}

	@Override
	public void onClick(View v) {
		if (v.equals(mStart)) {
			if (mServiceConnected) {
				mService.startServer();
			}
		} else if (v.equals(mSend)) {
			if (mServiceConnected) {
				mStatus.setText(mService.getJsonData());
			}
		} else if (v.equals(mStop)) {
			if (mServiceConnected) {
				mService.stopServer();
			}
		}
	}

}
