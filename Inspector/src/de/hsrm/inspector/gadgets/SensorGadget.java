package de.hsrm.inspector.gadgets;

import android.content.Context;
import de.hsrm.inspector.constants.SensorTypes;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.handler.utils.InspectorRequest;
import de.hsrm.inspector.services.utils.sensors.SensorObject;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

/**
 * Created by dobae on 27.05.13.
 */
public class SensorGadget extends Gadget {

    private SensorObject mSensorObject;

    @Override
    public void onCreate(Context context) {
        super.onCreate(context);
        mSensorObject = new SensorObject(context, SensorTypes.valueOf(getIdentifier()).getType());
    }

    @Override
    public void onRegister(Context context) {
        mSensorObject.registerListener();
    }

    @Override
    public void onUnregister(Context context) {
        mSensorObject.unregisterListener();
    }

    @Override
    public Object gogo(Context context, InspectorRequest iRequest, HttpRequest request, HttpResponse response, HttpContext http_context) throws Exception {
        return mSensorObject.getData();
    }
}