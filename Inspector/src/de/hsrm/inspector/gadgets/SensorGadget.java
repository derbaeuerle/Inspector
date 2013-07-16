package de.hsrm.inspector.gadgets;

import android.content.Context;
import android.util.Log;
import de.hsrm.inspector.constants.GadgetConstants;
import de.hsrm.inspector.constants.SensorConstants;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.handler.utils.InspectorRequest;
import de.hsrm.inspector.services.utils.sensors.SensorObject;

/**
 * Implementation of {@link Gadget} for sensor support.
 */
public class SensorGadget extends Gadget {

	private SensorObject mSensorObject;

	@Override
	public void onCreate(Context context) {
		super.onCreate(context);
		mSensorObject = new SensorObject(context, SensorConstants.SensorType.valueOf(getIdentifier()).getType());
	}

	@Override
	public void onRegister(Context context) {
		super.onRegister(context);
		mSensorObject.registerListener();
	}

	@Override
	public void onUnregister(Context context) {
		super.onUnregister(context);
		mSensorObject.unregisterListener();
	}

	@Override
	public Object gogo(Context context, InspectorRequest iRequest) throws Exception {
		if (iRequest.hasParameter(GadgetConstants.PARAM_STREAM_ID)) {
			Object o = mSensorObject.getData(iRequest.getParameter(GadgetConstants.PARAM_STREAM_ID).toString());
			Log.d("", iRequest.getParameter(GadgetConstants.PARAM_STREAM_ID).toString() + ": " + o.toString());
			return o;
		}
		return mSensorObject.getData(null);
	}
}
