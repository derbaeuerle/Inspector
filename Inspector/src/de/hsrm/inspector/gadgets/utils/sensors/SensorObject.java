package de.hsrm.inspector.gadgets.utils.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import de.hsrm.inspector.gadgets.communication.GadgetEvent;
import de.hsrm.inspector.gadgets.intf.Gadget;

/**
 * Wrapper class for {@link Sensor} support which implements
 * {@link SensorEventListener} and registers on itself.
 */
public class SensorObject implements SensorEventListener {

	private int SENSOR_TYPE = -1;
	private Sensor mSensor;
	private SensorManager mSensorManager;
	private Gadget mGadget;
	private long mLastEvent;
	private static final long NOTIFY_FREQUENCY = 30;

	/**
	 * Constructor of {@link SensorObject}.
	 * 
	 * @param context
	 *            Current application {@link Context}.
	 * @param type
	 *            Type as {@link Integer} of sensor.
	 */
	public SensorObject(Context context, Gadget gadget, int type) {
		mGadget = gadget;
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		setSensorType(type);
	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		if (System.currentTimeMillis() - mLastEvent > NOTIFY_FREQUENCY) {
			mGadget.notifyGadgetEvent(new GadgetEvent(mGadget, sensorEvent.values, "state"));
			mLastEvent = System.currentTimeMillis();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {
	}

	/**
	 * Registers {@link SensorEventListener} to configured
	 * {@link SensorObject#mSensor}.
	 */
	public void registerListener() {
		if (mSensor != null) {
			mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	/**
	 * Unregisters {@link SensorEventListener} from configured
	 * {@link SensorObject#mSensor}.
	 */
	public void unregisterListener() {
		if (mSensor != null) {
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

}
