package de.hsrm.inspector.gadgets;

import android.content.Context;
import de.hsrm.inspector.constants.SensorConstants;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.gadgets.utils.sensors.SensorObject;
import de.hsrm.inspector.handler.utils.InspectorRequest;

/**
 * Implementation of {@link Gadget} for sensor support.
 */
public class SensorGadget extends Gadget {

	private SensorObject mSensorObject;

	@Override
	public void onCreate(Context context) throws Exception {
		super.onCreate(context);
		mSensorObject = new SensorObject(context, this, SensorConstants.SensorType.valueOf(getIdentifier()).getType());
	}

	@Override
	public void onProcessStart() {
		super.onProcessStart();
		mSensorObject.registerListener();
	}

	@Override
	public void onProcessEnd() {
		super.onProcessEnd();
		mSensorObject.unregisterListener();
	}

	@Override
	public void gogo(InspectorRequest iRequest) throws Exception {
	}
}
