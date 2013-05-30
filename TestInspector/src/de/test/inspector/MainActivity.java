package de.test.inspector;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by dobae on 29.05.13.
 */
public class MainActivity extends Activity {

	private Button mSendIntent;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mSendIntent = (Button) findViewById(R.id.sendIntent);
		mSendIntent.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MainActivity.this.sendIntent();
			}
		});
	}

	private void sendIntent() {
		Intent intent = new Intent("de.inspector.intents");
		intent.setData(Uri.parse("inspect://init/"));
		sendBroadcast(intent);
	}
}