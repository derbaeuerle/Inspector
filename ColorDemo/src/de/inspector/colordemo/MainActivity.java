package de.inspector.colordemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ToggleButton;
import de.inspector.hsrm.WebServer;

public class MainActivity extends Activity implements OnClickListener {

	// private static final int MSG = 0;
	// private Handler mTempHandler;
	// private TextView mTempView;
	// private RelativeLayout mLayout;
	//
	// private volatile ColorService mColorService;
	// private ServiceConnection mConnection = new ServiceConnection() {
	//
	// public void onServiceConnected(ComponentName className, IBinder binder) {
	// mColorService = (ColorService) ((ServiceBinder) binder).getService();
	// mColorService.registerListeners();
	// mTempHandler.sendEmptyMessage(MSG);
	// }
	//
	// public void onServiceDisconnected(ComponentName className) {
	// mColorService = null;
	// }
	// };
	private WebServer mServer;
	private ToggleButton mToggle;
	private Button mButton;
	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d("", "service Connected!!!!!");
		}

		public void onServiceDisconnected(ComponentName className) {
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// mTempView = (TextView) findViewById(R.id.textView1);
		// mLayout = (RelativeLayout) findViewById(R.id.layout);

		// mTempHandler = new Handler(new TempCallback());
		mToggle = (ToggleButton) findViewById(R.id.toggleButton1);
		mToggle.setOnClickListener(this);
		mButton = (Button) findViewById(R.id.button1);
		// mButton.setOnClickListener(this);
		mServer = new WebServer(getApplicationContext(), getResources().openRawResource(R.raw.inspector_config));
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Intent i = new Intent(getApplicationContext(), ColorService.class);
		// bindService(i, mConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// mTempHandler.removeMessages(MSG);
		// if (mColorService != null) {
		// mColorService.unregisterListeners();
		// }
		// unbindService(mConnection);
	}

	@Override
	public void onClick(View v) {
		if (v.equals(mToggle)) {
			if (mToggle.isChecked()) {
				mServer.startThread();
			} else {
				mServer.stopThread();
			}
		} else if (v.equals(mButton)) {
			Handler h = new Handler(new Handler.Callback() {

				@Override
				public boolean handleMessage(Message msg) {
					HttpClient httpclient = new DefaultHttpClient();
					HttpGet HttpGet = new HttpGet("http://localhost:" + WebServer.SERVER_PORT + "/color?callback=test");
					try {
						HttpResponse response = httpclient.execute(HttpGet);
						HttpEntity ht = response.getEntity();
						BufferedHttpEntity buf = new BufferedHttpEntity(ht);
						InputStream is = buf.getContent();
						BufferedReader r = new BufferedReader(new InputStreamReader(is));
						StringBuilder total = new StringBuilder();
						String line;
						while ((line = r.readLine()) != null) {
							total.append(line);
						}
					} catch (ClientProtocolException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return false;
				}
			});
			h.sendEmptyMessage(0);
		}
	}
	// private class TempCallback implements Handler.Callback {
	//
	// @Override
	// public boolean handleMessage(Message msg) {
	// if (mColorService != null) {
	// String output = "";
	// float[] data = mColorService.getData();
	// for (int i = 0; i < data.length; i++) {
	// output += data[i] + ", ";
	// }
	// int color = Color.rgb((int) (255 * data[0]), (int) (255 * data[1]), (int)
	// (255 * data[2]));
	// if (!output.equals("")) {
	// output.substring(0, output.length() - 2);
	// }
	// mTempView.setText(output);
	// mLayout.setBackgroundColor(color);
	// mTempHandler.sendEmptyMessageDelayed(MSG, 1000);
	// }
	// return false;
	// }
	// }

}
