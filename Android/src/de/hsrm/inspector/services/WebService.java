package de.hsrm.inspector.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import de.hsrm.inspector.services.helper.ServiceBinder;
import de.hsrm.inspector.web.WebServer;

public class WebService extends Service {

	private WebServer mServer;
	private IBinder mBinder = new WebBinder();

	public class WebBinder extends ServiceBinder {
		@Override
		public Service getService() {
			return WebService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mServer = new WebServer(getApplicationContext());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}

	public void startServer() {
		mServer.startThread();
	}

	public void stopServer() {
		mServer.stopThread();
	}

	public String getJsonData() {
		try {
			HttpClient cl = new DefaultHttpClient();
			HttpGet request = new HttpGet("http://localhost:" + WebServer.SERVER_PORT + "/audio/test");
			HttpResponse response = cl.execute(request);
			InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			StringBuilder b = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				b.append(line + "\n");
			}
			in.close();
			reader.close();
			Log.d("RESPONSE", b.toString());
			return b.toString();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

}
