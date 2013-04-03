package de.hsrm.inspector.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class AudioService extends Service {

	private IBinder mBinder = new AudioBinder();

	public class AudioBinder extends Binder {
		public AudioService getService() {
			return AudioService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}

	public void play() {
		Log.d("AudioService", "play");
	}

	public void pause() {
		Log.d("AudioService", "pause");
	}

	public void stop() {
		Log.d("AudioService", "stop");
	}

	public void setVolume() {
		Log.d("AudioService", "setVolume");
	}

}
