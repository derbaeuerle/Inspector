package de.hsrm.inspector.constants;

import de.hsrm.inspector.gadgets.SensorGadget;
import android.hardware.Sensor;
import android.provider.SyncStateContract.Constants;

/**
 * {@link Constants} class for {@link SensorGadget}.
 */
public class SensorConstants {

	/**
	 * Intelligent {@link Enum} for all available {@link Sensor} object. Current
	 * implementation is based on {@link Sensor} available in api level 8.
	 */
	public enum SensorType {
		ACCELERATION(Sensor.TYPE_ACCELEROMETER), GYROSCOPE(Sensor.TYPE_GYROSCOPE), TEMPERATURE(Sensor.TYPE_TEMPERATURE), LIGHT(
				Sensor.TYPE_LIGHT), MAGNETIC(Sensor.TYPE_MAGNETIC_FIELD), ORIENTATION(Sensor.TYPE_ORIENTATION), PRESSURE(
				Sensor.TYPE_PRESSURE), PROXIMITY(Sensor.TYPE_PROXIMITY);
		private final int type;

		private SensorType(int type) {
			this.type = type;
		}

		public int getType() {
			return this.type;
		}
	}

}
