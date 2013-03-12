package de.hsrm.jcommunicator.web.handler;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import de.hsrm.jcommunicator.web.defaults.DefaultHandler;

public class SystemTimeHandler extends DefaultHandler {

	public SystemTimeHandler(Context context) {
		super(context);
	}

	@Override
	public JSONObject handleRequest(HttpRequest request, HttpContext context, Uri requestLine) {
		JSONObject obj = new JSONObject();
		try {
			obj.put("time", System.currentTimeMillis());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj;
	}

}
