package de.hsrm.inspector.services.utils;

import android.app.Service;
import android.content.ServiceConnection;

import java.util.concurrent.Callable;

/**
 * Default {@link Callable} class to process request and convert processed data
 * on {@link #call()}.
 *
 * @author Dominic Baeuerle
 */
public abstract class AsyncServiceBinderCallable implements Callable<Object> {

    private Service mService;
    private ServiceConnection mConnection;

    @Override
    public Object call() throws Exception {
        return onCall();
    }

    public abstract Object onCall();

    public Service getService() {
        return mService;
    }

    public void setService(Service s) {
        mService = s;
    }

    public ServiceConnection getConnection() {
        return mConnection;
    }

    public void setServiceConnection(ServiceConnection c) {
        mConnection = c;
    }

}