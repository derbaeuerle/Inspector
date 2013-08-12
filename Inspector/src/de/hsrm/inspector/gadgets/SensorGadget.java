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

	/** {@link SensorObject} to use. */
	private SensorObject mSensorObject;

	/**
	 * Implementation of {@link Gadget#onCreate(Context)} to initialize
	 * {@link #mSensorObject}.
	 * 
	 * @see de.hsrm.inspector.gadgets.intf.Gadget#onCreate(android.content.Context)
	 */
	@Override
	public void onCreate(Context context) throws Exception {
		super.onCreate(context);
		mSensorObject = new SensorObject(context, this, SensorConstants.SensorType.valueOf(getIdentifier()).getType());
	}

	/**
	 * Implementation of {@link Gadget#onProcessStart()} to call
	 * {@link SensorObject#registerListener()} on {@link #mSensorObject}.
	 * 
	 * @see de.hsrm.inspector.gadgets.intf.Gadget#onProcessStart()
	 */
	@Override
	public void onProcessStart() throws Exception {
		super.onProcessStart();
		mSensorObject.registerListener();
	}

	/**
	 * Implementation of {@link Gadget#onProcessEnd()} to call
	 * {@link SensorObject#unregisterListener()} on {@link #mSensorObject}.
	 * 
	 * @see de.hsrm.inspector.gadgets.intf.Gadget#onProcessEnd()
	 */
	@Override
	public void onProcessEnd() throws Exception {
		super.onProcessEnd();
		mSensorObject.unregisterListener();
	}

	/**
	 * Implementation of {@link Gadget#gogo(InspectorRequest)}. If
	 * {@link InspectorRequest} has {@link SensorConstants#PARAM_RATE} set,
	 * {@link #mSensorObject} will be configured to this rate.
	 * 
	 * @see de.hsrm.inspector.gadgets.intf.Gadget#gogo(de.hsrm.inspector.handler.
	 *      utils.InspectorRequest)
	 */
	@Override
	public void gogo(InspectorRequest iRequest) throws Exception {
		if (iRequest.hasParameter(SensorConstants.PARAM_RATE)) {
			try {
				if (mSensorObject != null) {
					mSensorObject.setRate(Integer
							.parseInt(iRequest.getParameter(SensorConstants.PARAM_RATE).toString()));
				}
			} catch (ClassCastException e) {

			}
		}
	}
}
