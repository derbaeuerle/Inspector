package de.inspector.hsrm.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import de.inspector.hsrm.exceptions.GadgetException;
import de.inspector.hsrm.services.intf.Gadget;
import de.inspector.hsrm.services.utils.ServiceBinder;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by dobae on 25.05.13.
 */
public class GadgetManager extends Service {

    private ServiceBinder mBinder;
    private Map<String, Gadget> mGadgetConfiguration;
    private Map<String, AtomicBoolean> mGadgetsRunning;
    private Map<String, Gadget> mGadgetInstances;

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        mBinder = new ServiceBinder() {
            @Override
            public Service getService() {
                return GadgetManager.this;
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setGadgetConfiguration(Map<String, Gadget> config) {
        this.mGadgetConfiguration = config;
    }

    public void initGadget(String identifier) {
        if (mGadgetConfiguration.containsKey(identifier)) {
            Gadget g = mGadgetConfiguration.get(identifier);
            if (g.isMultiInstance() || !mGadgetConfiguration.containsKey(identifier)) {
                try {
                    Gadget instance = g.createInstance();
                    mGadgetInstances.put(instance.getUniqueIdentifier(), instance);
                } catch (GadgetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void register(String identifier) {
        setGadgetRunning(identifier, true);
        mGadgetInstances.get(identifier).onRegister(getApplicationContext());
    }

    public void unregister(String identifier) {
        setGadgetRunning(identifier, false);
        mGadgetInstances.get(identifier).onUnregister(getApplicationContext());
    }

    public void unregisterAllGadgets() {
        for (Map.Entry<String, Gadget> r : mGadgetInstances.entrySet()) {
            r.getValue().onUnregister(getApplicationContext());
        }
    }

    private void setGadgetRunning(String id, boolean value) {
        if (!mGadgetsRunning.containsKey(id)) {
            mGadgetsRunning.put(id, new AtomicBoolean());
        }
        mGadgetsRunning.get(id).set(value);
    }
}
