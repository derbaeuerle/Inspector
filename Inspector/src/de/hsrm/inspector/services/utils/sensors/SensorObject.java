package de.hsrm.inspector.services.utils.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by dobae on 25.05.13.
 */
public class SensorObject implements SensorEventListener {

    private int SENSOR_TYPE = -1;
    private Sensor mSensor;
    private SensorManager mSensorManager;
    private SensorEvent mLastEvent;

    public SensorObject(Context context, int type) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        setSensorType(type);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        mLastEvent = sensorEvent;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public float[] getData() {
        return (mLastEvent != null) ? mLastEvent.values : new float[0];
    }

    public void registerListener() {
        if (mSensor != null) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void unregisterListener() {
        if (mSensor != null) {
            mSensorManager.unregisterListener(this, mSensor);
        }
    }

    public void setSensorType(int type) throws UnsupportedOperationException {
        if (SENSOR_TYPE == -1) {
            SENSOR_TYPE = type;
            mSensor = mSensorManager.getDefaultSensor(SENSOR_TYPE);
        } else {
            throw new UnsupportedOperationException();
        }
    }


}
