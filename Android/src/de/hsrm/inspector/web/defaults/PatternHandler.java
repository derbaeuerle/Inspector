package de.hsrm.inspector.web.defaults;

import java.util.HashMap;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class PatternHandler extends DefaultHandler {

	private HashMap<String, DefaultHandler> mHandlerRegistry;

	public PatternHandler(Context context) {
		super(context);
		mHandlerRegistry = new HashMap<String, DefaultHandler>();
	}

	@Override
	public Object handleRequest(HttpRequest request, HttpContext context, Uri requestLine) throws Exception {
		Log.d("REQUEST:", request.getRequestLine().toString());
		for (String pattern : mHandlerRegistry.keySet()) {
			
			return mHandlerRegistry.get(pattern).handleRequest(request, context, requestLine);
		}
		return "{}";
	}

	public void registerHandler(String urlPattern, DefaultHandler handler) {
		mHandlerRegistry.put(urlPattern, handler);
	}

	public HashMap<String, DefaultHandler> getRegistry() {
		return mHandlerRegistry;
	}

}
