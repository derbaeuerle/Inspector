package de.hsrm.inspector.exceptions.constants;

import android.hardware.Sensor;
import de.hsrm.inspector.exceptions.GadgetException;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.gadgets.utils.sensors.SensorObject;

/**
 * Constants class for {@link GadgetException}.
 */
public class GadgetExceptionConstants {

	/** Error code if called {@link Gadget} needs permission of user. */
	public static final int GADGET_NEEDS_PERMISSION = 2;
	/** Error code if called {@link Gadget} is disabled by user. */
	public static final int GADGET_IS_DISABLED = 3;
	/** Error code if called {@link Gadget} is not available. */
	public static final int GADGET_NOT_AVAILABLE = 4;

	/**
	 * Error code if {@link SensorObject} wants to call an unsupported
	 * {@link Sensor}.
	 */
	public static final int SENSOR_NOT_SUPPORTED = 100;

}
