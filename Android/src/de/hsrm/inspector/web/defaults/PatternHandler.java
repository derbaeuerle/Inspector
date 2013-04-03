package de.hsrm.inspector.web.defaults;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import de.hsrm.inspector.Gadget;
import de.hsrm.inspector.services.helper.ServiceBinder;

public class PatternHandler extends DefaultHandler {

	private List<Gadget> mGadgetRegistry;
	private Future<Object> mServiceBinder;

	public PatternHandler(Context context) {
		super(context);
		mGadgetRegistry = new ArrayList<Gadget>();
	}

	@Override
	public Object gogo(HttpRequest request, HttpContext context, Uri requestLine) throws Exception {
		Log.d("REQUEST:", request.getRequestLine().toString());
		String uri = requestLine.toString();
		for (final Gadget gadget : mGadgetRegistry) {
			if (uri.matches(gadget.getPattern())) {
				if (gadget.usesService()) {
				}
				return gadget.gogo(request, context, requestLine);
			}
		}
		return "{}";
	}

	public void registerHandler(String urlPattern, DefaultHandler handler) {
		mGadgetRegistry.add(new Gadget(urlPattern, handler, mContext));
	}

	public void registerHandler(String urlPattern, DefaultHandler handler, String serviceClass) {
		Gadget g = new Gadget(urlPattern, handler, mContext);
		g.setServiceClass(serviceClass);
		mGadgetRegistry.add(g);
	}

	public void registerHandler(String urlPattern, Gadget gadget) {
		mGadgetRegistry.add(gadget);
	}

	public List<Gadget> getRegistry() {
		return mGadgetRegistry;
	}

}
