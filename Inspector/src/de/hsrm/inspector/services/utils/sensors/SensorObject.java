package de.hsrm.inspector.services.utils.sensors;

import java.util.Arrays;
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
 * Wrapper class for {@link Sensor} support which implements
 * {@link SensorEventListener} and registers on itself.
 */
public class SensorObject implements SensorEventListener {

	private int SENSOR_TYPE = -1;
	private Sensor mSensor;
	private SensorManager mSensorManager;
	private float[] mLastEvent = null;
	private ConcurrentHashMap<String, float[]> mClients;

	private ValueWaiter mWaiter;

	/**
	 * Constructor of {@link SensorObject}.
	 * 
	 * @param context
	 *            Current application {@link Context}.
	 * @param type
	 *            Type as {@link Integer} of sensor.
	 */
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

	/**
	 * Returns the last received {@link SensorEvent} values. If a calls this
	 * {@link SensorObject} this method only provides new values and waits if
	 * the current values has already been send to the caller.
	 * 
	 * @param id
	 *            Stream id as {@link String}.
	 * @return {@link Float} {@link Arrays}
	 */
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

	/**
	 * Registers {@link SensorEventListener} to configured
	 * {@link SensorObject#mSensor}.
	 */
	public void registerListener() {
		if (mSensor != null) {
			mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
		}
	}

	/**
	 * Unregisters {@link SensorEventListener} from configured
	 * {@link SensorObject#mSensor}.
	 */
	public void unregisterListener() {
		if (mSensor != null) {
			mLastEvent = null;
			mSensorManager.unregisterListener(this, mSensor);
		}
	}

	/**
	 * Sets type of {@link Sensor}.
	 * 
	 * @param type
	 *            {@link Integer}
	 * @throws UnsupportedOperationException
	 */
	public void setSensorType(int type) throws UnsupportedOperationException {
		if (SENSOR_TYPE == -1) {
			SENSOR_TYPE = type;
			mSensor = mSensorManager.getDefaultSensor(SENSOR_TYPE);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * {@link FutureTask} implementation to wait for a new value from
	 * {@link SensorEventListener}.
	 */
	private class ValueWaiter extends FutureTask<float[]> {

		private float[] mOldValue;

		/**
		 * Constructor with {@link Callable} object.
		 * 
		 * @param callable
		 *            {@link Callable} to execute.
		 */
		public ValueWaiter(Callable<float[]> callable) {
			super(callable);
		}

		/**
		 * Sets value of {@link SensorObject} at the time, the
		 * {@link ValueWaiter} gets started.
		 * 
		 * @param old
		 *            {@link Float} array.
		 */
		public void setOldValue(float[] old) {
			mOldValue = old;
		}

		/**
		 * Returns {@link #mOldValue}.
		 * 
		 * @return {@link Float} {@link Arrays}
		 */
		public float[] getOldValue() {
			return mOldValue;
		}

	}

	/**
	 * Implementation of {@link Callable} to execute if {@link SensorObject}
	 * waits for a new {@link SensorObject#mLastEvent}.
	 */
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
				Thread.sleep(50);
			}
		}
	}

}
