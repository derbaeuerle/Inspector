package de.hsrm.inspector.gadgets;

import android.content.Context;
import android.util.Log;
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
	public void onCreate(Context context) {
		super.onCreate(context);
		mSensorObject = new SensorObject(context, this, SensorConstants.SensorType.valueOf(getIdentifier()).getType());
	}

	@Override
	public void onProcessStart(Context context) {
		super.onProcessStart(context);
		mSensorObject.registerListener();
	}

	@Override
	public void onProcessEnd(Context context) {
		super.onProcessEnd(context);
		mSensorObject.unregisterListener();
	}

	@Override
	public void gogo(Context context, InspectorRequest iRequest) throws Exception {
		Log.d("SENSORGADGET", "gogo: " + iRequest.getSegments().toString());
	}
}
