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

	private int SENSOR_TYPE = -1;
	private Sensor mSensor;
	private SensorManager mSensorManager;
	private Gadget mGadget;
	private long mLastEvent;
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

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		if (System.currentTimeMillis() - mLastEvent > mRate) {
			mGadget.notifyGadgetEvent(new GadgetEvent(mGadget, sensorEvent.values, EVENT_TYPE.DATA));
			mLastEvent = System.currentTimeMillis();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {
		// mGadget.notifyGadgetEvent(new GadgetEvent(mGadget, i,
		// EVENT_TYPE.FEEDBACK));
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

	public void setRate(int rate) {
		mRate = rate;
	}

}
