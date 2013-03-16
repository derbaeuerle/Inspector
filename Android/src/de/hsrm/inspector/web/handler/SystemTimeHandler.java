package de.hsrm.inspector.web.handler;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import de.hsrm.inspector.web.defaults.DefaultHandler;

public class SystemTimeHandler extends DefaultHandler {

	public SystemTimeHandler(Context context) {
		super(context);
	}

	@Override
	public Object handleRequest(HttpRequest request, HttpContext context, Uri requestLine) {
		long time = System.currentTimeMillis();
		Log.d("", "time: " + time);
		return time;
	}

}
