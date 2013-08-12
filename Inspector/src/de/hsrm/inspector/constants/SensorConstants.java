package de.hsrm.inspector.constants;

import android.hardware.Sensor;
import de.hsrm.inspector.gadgets.SensorGadget;

/**
 * Constants class for {@link SensorGadget}.
 */
public class SensorConstants {

	/** URL parameter key for sensor rate */
	public static final String PARAM_RATE = "rate";

	/**
	 * Intelligent {@link Enum} for all available {@link Sensor} object. Current
	 * implementation is based on {@link Sensor} available in api level 8.
	 */
	public enum SensorType {
		ACCELERATION(Sensor.TYPE_ACCELEROMETER), GYROSCOPE(Sensor.TYPE_GYROSCOPE), TEMPERATURE(Sensor.TYPE_TEMPERATURE), LIGHT(
				Sensor.TYPE_LIGHT), MAGNETIC(Sensor.TYPE_MAGNETIC_FIELD), ORIENTATION(Sensor.TYPE_ORIENTATION), PRESSURE(
				Sensor.TYPE_PRESSURE), PROXIMITY(Sensor.TYPE_PROXIMITY);

		/** {@link Integer} value of {@link SensorType} */
		private final int type;

		/**
		 * Constructor sets {@link #type} to given value.
		 * 
		 * @param type
		 *            {@link Integer}
		 */
		private SensorType(int type) {
			this.type = type;
		}

		/**
		 * Returns {@link #type} value.
		 * 
		 * @return {@link Integer}
		 */
		public int getType() {
			return this.type;
		}
	}

}
