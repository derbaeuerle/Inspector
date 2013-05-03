package de.inspector.colordemo.service;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import de.inspector.hsrm.service.utils.ServiceBinder;

public class ColorService extends Service implements SensorEventListener {

	private ServiceBinder mBinder;
	private SensorManager mSensorManager;
	private Sensor mSensor;
	private float[] mData = { 0f, 0f, 0f };

	@Override
	public ServiceBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mBinder = new ServiceBinder() {

			@Override
			public Service getService() {
				return ColorService.this;
			}
		};

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
	}

	public void registerListeners() {
		mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
	}

	public void unregisterListeners() {
		mSensorManager.unregisterListener(this, mSensor);
	}

	public float[] getData() {
		for (int i = 0; i < mData.length; i++) {
			if (mData[i] <= 0) {
				mData[i] *= -1;
			} else {
				mData[i] += mSensor.getMaximumRange();
			}
			mData[i] = mData[i] / (mSensor.getMaximumRange() * 2);
		}
		return mData;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		mData = event.values;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

}
