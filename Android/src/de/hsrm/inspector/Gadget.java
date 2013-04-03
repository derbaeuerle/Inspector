package de.hsrm.inspector;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import de.hsrm.inspector.services.helper.ServiceBinder;
import de.hsrm.inspector.web.defaults.DefaultHandler;
import de.hsrm.inspector.web.defaults.DefaultServiceHandler;

public class Gadget {

	private Context mContext;

	private String mPattern, mUsesService;

	private DefaultHandler mHandler;
	private Service mService;
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			if (binder instanceof ServiceBinder) {
				mService = ((ServiceBinder) binder).getService();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};

	public Gadget() {
		this("", null, null);
	}

	public Gadget(Context context) {
		this("", null, context);
	}

	public Gadget(String pattern, DefaultHandler handler, Context context) {
		mContext = context;
		mPattern = pattern;
		mHandler = handler;
	}

	/**
	 * @see DefaultHandler#gogo(HttpRequest, HttpContext, Uri)
	 */
	public Object gogo(HttpRequest request, HttpContext context, Uri requestLine) throws Exception {
		if (usesService()) {
			Intent i = new Intent(mContext, Class.forName(mUsesService));
			mContext.bindService(i, mServiceConnection, Context.BIND_AUTO_CREATE);
			return ((DefaultServiceHandler) mHandler).gogo(request, context, requestLine, mService);
		}
		return mHandler.gogo(request, context, requestLine);
	}

	public String getServiceClass() {
		return mUsesService;
	}

	public void setServiceClass(String serviceClass) {
		mUsesService = serviceClass;
	}

	public void bindService(Service service) {
		mService = service;
	}

	public void unbindService(Service service) {
		mService = null;
	}

	public boolean checkServiceConnections() {
		return mService != null;
	}

	public boolean usesService() {
		return mUsesService != null;
	}

	public String getPattern() {
		return mPattern;
	}

	public void setPattern(String mPattern) {
		this.mPattern = mPattern;
	}

	public DefaultHandler getHandler() {
		return mHandler;
	}

	public void setHandler(DefaultHandler mHandler) {
		this.mHandler = mHandler;
	}

}
