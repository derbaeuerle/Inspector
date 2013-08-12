package de.hsrm.inspector.services.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Future task to bind a {@link Service} by default android
 * {@link Context#bindService(Intent, ServiceConnection, int)} method
 * synchronously and call {@link de.hsrm.inspector.gadgets.OldGadget} when
 * service is bound.
 * 
 * @author Dominic Baeuerle
 */
public class AsyncServiceBinder extends FutureTask<Object> {

	/** Current application {@link Context}. */
	private Context mApplicationContext;
	/** {@link AtomicBoolean} to identify if {@link Service} is bound. */
	private AtomicBoolean mServiceBound = new AtomicBoolean(false);
	/** {@link Service} to bind. */
	private Service mService;
	/** {@link AsyncServiceBinderCallable} to call. */
	private AsyncServiceBinderCallable mCallable;
	/**
	 * {@link ServiceConnection}
	 */
	private ServiceConnection mConnection = new ServiceConnection() {

		/**
		 * Sets {@link #mService} to bound {@link Service},
		 * {@link #mServiceBound} to <code>true</code> and set
		 * {@link ServiceConnection} and {@link Service} in {@link #mCallable}.
		 * After setting all attributes {@link FutureTask#run()} will be called.
		 * 
		 * @param className
		 *            {@link ComponentName}
		 * @param binder
		 *            {@link IBinder}
		 */
		public void onServiceConnected(ComponentName className, IBinder binder) {
			if (binder instanceof ServiceBinder) {
				mService = ((ServiceBinder) binder).getService();
				mServiceBound.set(true);

				mCallable.setService(mService);
				mCallable.setServiceConnection(mConnection);
				run();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mServiceBound.set(false);
			mService = null;
		}
	};

	/**
	 * Constructor for future task.
	 * 
	 * @param callable
	 *            {@link AsyncServiceBinderCallable} to call when {@link #run()}
	 *            is called.
	 * @param applicationContext
	 *            Current android applicatione {@link Context}.
	 */
	public AsyncServiceBinder(AsyncServiceBinderCallable callable, Context applicationContext) {
		super(callable);
		mCallable = callable;
		mApplicationContext = applicationContext;
	}

	/**
	 * Starts the binding of required service.
	 * 
	 * @return {@link Object}
	 * @throws ClassNotFoundException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@SuppressWarnings("rawtypes")
	public Object process(Class service) throws ClassNotFoundException, InterruptedException, ExecutionException {
		Intent i = new Intent(mApplicationContext, service);
		mApplicationContext.bindService(i, mConnection, Context.BIND_AUTO_CREATE);
		return get();
	}

}