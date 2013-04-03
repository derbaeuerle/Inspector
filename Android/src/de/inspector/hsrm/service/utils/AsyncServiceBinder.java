package de.inspector.hsrm.service.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import de.inspector.hsrm.gadgets.Gadget;

public class AsyncServiceBinder extends FutureTask<Object> {

	private Context mApplicationContext;
	private AtomicBoolean mServiceBound = new AtomicBoolean(false);
	private Service mService;
	private AsyncServiceBinderCallable mCallable;
	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			if (binder instanceof ServiceBinder) {
				mService = ((ServiceBinder) binder).getService();
				mCallable.getGadget().setBoundService(mService);
				mServiceBound.set(true);

				AsyncServiceBinder.this.run();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mServiceBound.set(false);
			mService = null;
		}
	};

	public AsyncServiceBinder(AsyncServiceBinderCallable callable, Gadget gadget, Context applicationContext) {
		super(callable);
		mCallable = callable;
		mApplicationContext = applicationContext;
	}

	public Object process() throws ClassNotFoundException, InterruptedException, ExecutionException {
		Intent i = new Intent(mApplicationContext, Class.forName(mCallable.getGadget().getService()));
		mApplicationContext.bindService(i, mConnection, Context.BIND_AUTO_CREATE);
		return get();
	}

}