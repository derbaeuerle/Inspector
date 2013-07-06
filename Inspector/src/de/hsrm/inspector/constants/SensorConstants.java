package de.hsrm.inspector.constants;

import android.hardware.Sensor;

/**
 * Created by dobae on 25.05.13.
 */
public class SensorConstants {

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
