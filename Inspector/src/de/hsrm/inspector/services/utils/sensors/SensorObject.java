package de.hsrm.inspector.services.utils.sensors;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by dobae on 25.05.13.
 */
public class SensorObject implements SensorEventListener {

	private int SENSOR_TYPE = -1;
	private Sensor mSensor;
	private SensorManager mSensorManager;
	private float[] mLastEvent = null;
	private ConcurrentHashMap<String, float[]> mClients;

	private ValueWaiter mWaiter;

	public SensorObject(Context context, int type) {
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		setSensorType(type);
		mClients = new ConcurrentHashMap<String, float[]>();

		ValueWaiterCallable callable = new ValueWaiterCallable();
		mWaiter = new ValueWaiter(callable);
	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		mLastEvent = sensorEvent.values;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {
	}

	public float[] getData(String id) {
		try {
			if (id == null) {
				if (mLastEvent == null) {
					mWaiter.setOldValue(null);
					mWaiter.run();
					return mWaiter.get();
				} else {
					return mLastEvent;
				}
			} else {
				if (mClients.containsKey(id)) {
					if (mClients.get(id).toString().equals(mLastEvent.toString())) {
						mWaiter.setOldValue(mClients.get(id));
						mWaiter.run();
						float[] newValue = mWaiter.get();
						mClients.put(id, newValue);
						return mClients.get(id);
					} else {
						mClients.put(id, mLastEvent);
					}
					return mClients.get(id);
				} else {
					if (mLastEvent == null) {
						mWaiter.setOldValue(null);
						mWaiter.run();
						float[] newValue = mWaiter.get();
						mClients.put(id, newValue);
					} else {
						mClients.put(id, mLastEvent);
					}
					return mClients.get(id);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return new float[0];
	}

	public void registerListener() {
		if (mSensor != null) {
			mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
		}
	}

	public void unregisterListener() {
		if (mSensor != null) {
			mLastEvent = null;
			mSensorManager.unregisterListener(this, mSensor);
		}
	}

	public void setSensorType(int type) throws UnsupportedOperationException {
		if (SENSOR_TYPE == -1) {
			SENSOR_TYPE = type;
			mSensor = mSensorManager.getDefaultSensor(SENSOR_TYPE);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	private class ValueWaiter extends FutureTask<float[]> {

		private float[] mOldValue;

		public ValueWaiter(Callable<float[]> callable) {
			super(callable);
		}

		public void setOldValue(float[] old) {
			mOldValue = old;
		}

		public float[] getOldValue() {
			return mOldValue;
		}

	}

	private class ValueWaiterCallable implements Callable<float[]> {

		@Override
		public float[] call() throws Exception {
			while (true) {
				if (mLastEvent != null) {
					if (mWaiter.getOldValue() == null) {
						return mLastEvent;
					} else if (!mLastEvent.toString().equals(mWaiter.getOldValue().toString())) {
						return mLastEvent;
					}
				}
				// Log.d("EventWaiter", "sleep");
				Thread.sleep(50);
			}
		}
	}

}
