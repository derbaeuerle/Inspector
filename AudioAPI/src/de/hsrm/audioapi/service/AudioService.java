package de.hsrm.audioapi.service;
import android.app.Service;
import android.content.Intent;
import android.util.Log;
import de.inspector.hsrm.service.utils.ServiceBinder;

public class AudioService extends Service {

	private ServiceBinder mBinder;

	@Override
	public ServiceBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("AUDIO SERVICE", "onCreate");
		mBinder = new ServiceBinder() {

			@Override
			public Service getService() {
				return AudioService.this;
			}
		};
	}

	public void play() {
		Log.d("AUDIO SERVICE", "play");
	}

	public void pause() {
		Log.d("AUDIO SERVICE", "pause");
	}

	public void stop() {
		Log.d("AUDIO SERVICE", "stop");
	}

}
