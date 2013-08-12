package de.hsrm.inspector.gadgets.utils.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import de.hsrm.inspector.exceptions.GadgetException;
import de.hsrm.inspector.exceptions.constants.GadgetExceptionConstants;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.gadgets.pool.GadgetEvent;
import de.hsrm.inspector.gadgets.pool.GadgetEvent.EVENT_TYPE;

/**
 * Wrapper class for {@link Sensor} support which implements
 * {@link SensorEventListener} and registers on itself.
 */
public class SensorObject implements SensorEventListener {

	/** Integer value of sensor based on {@link SensorManager} attributes. */
	private int SENSOR_TYPE = -1;
	/** {@link Sensor} object to be used. */
	private Sensor mSensor;
	/** {@link SensorManager} instance. */
	private SensorManager mSensorManager;
	/** Parent {@link Gadget} object. */
	private Gadget mGadget;
	/** Timestamp of last event notification. */
	private long mLastEvent;
	/** Rate in milliseconds to notify new {@link GadgetEvent}. */
	private long mRate = 30;

	/**
	 * Constructor of {@link SensorObject}.
	 * 
	 * @param context
	 *            Current application {@link Context}.
	 * @param type
	 *            Type as {@link Integer} of sensor.
	 */
	public SensorObject(Context context, Gadget gadget, int type) throws UnsupportedOperationException, GadgetException {
		mGadget = gadget;
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		setSensorType(type);
	}

	/**
	 * Notifies {@link #mGadget} if current {@link #mLastEvent} is older than
	 * {@link #mRate}. {@link SensorEventListener#onSensorChanged(SensorEvent)}
	 */
	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		if (System.currentTimeMillis() - mLastEvent > mRate) {
			mGadget.notifyGadgetEvent(new GadgetEvent(mGadget, sensorEvent.values, EVENT_TYPE.DATA));
			mLastEvent = System.currentTimeMillis();
		}
	}

	/**
	 * {@link SensorEventListener#onAccuracyChanged(Sensor, int)}
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {
	}

	/**
	 * Registers {@link SensorEventListener} to configured
	 * {@link SensorObject#mSensor}.
	 * 
	 * @throws {@link GadgetException}
	 */
	public void registerListener() throws GadgetException {
		if (mSensor != null) {
			if (!mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL)) {
				throw new GadgetException("Your device doesn't support this sensor!",
						GadgetExceptionConstants.SENSOR_NOT_SUPPORTED);
			}
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
	 * @throws {@link GadgetException}
	 */
	public void setSensorType(int type) throws UnsupportedOperationException, GadgetException {
		if (SENSOR_TYPE == -1) {
			SENSOR_TYPE = type;
			mSensor = mSensorManager.getDefaultSensor(SENSOR_TYPE);
		}
		if (mSensor == null) {
			throw new GadgetException("Your device doesn't support this sensor!",
					GadgetExceptionConstants.SENSOR_NOT_SUPPORTED);
		}
	}

	/**
	 * Sets {@link #mRate} in milliseconds.
	 * 
	 * @param rate
	 *            {@link Integer}
	 */
	public void setRate(int rate) {
		mRate = rate;
	}

}
