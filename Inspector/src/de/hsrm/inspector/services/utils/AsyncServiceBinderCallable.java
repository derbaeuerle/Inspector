package de.hsrm.inspector.services.utils;

import android.app.Service;
import android.content.ServiceConnection;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Default {@link Callable} class to process request and convert processed data
 * on {@link #call()}.
 * 
 * @author Dominic Baeuerle
 */
public abstract class AsyncServiceBinderCallable implements Callable<Object> {

	/** Bound {@link Service}. */
	private Service mService;
	/** {@link ServiceConnection} of bound {@link Service}. */
	private ServiceConnection mConnection;

	/**
	 * Returns return value of {@link #onCall()}.
	 * 
	 * @return {@link Object}
	 */
	@Override
	public Object call() throws Exception {
		return onCall();
	}

	/**
	 * Abstract method which will be called if {@link FutureTask} is finished
	 * running.
	 * 
	 * @return {@link Object}
	 */
	public abstract Object onCall();

	/**
	 * Returns {@link #mService}.
	 * 
	 * @return {@link Service}
	 */
	public Service getService() {
		return mService;
	}

	/**
	 * Sets {@link #mService} to given {@link Service}.
	 * 
	 * @param s
	 *            {@link Service}.
	 */
	public void setService(Service s) {
		mService = s;
	}

	/**
	 * Returns {@link #mConnection}.
	 * 
	 * @return {@link ServiceConnection}
	 */
	public ServiceConnection getConnection() {
		return mConnection;
	}

	/**
	 * Sets {@link #mConnection} to given {@link ServiceConnection}.
	 * 
	 * @param c
	 *            {@link ServiceConnection}
	 */
	public void setServiceConnection(ServiceConnection c) {
		mConnection = c;
	}

}