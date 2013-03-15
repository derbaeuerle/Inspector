package de.hsrm.jcommunicator.web.handler;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.net.Uri;
import de.hsrm.jcommunicator.web.defaults.DefaultHandler;

public class SystemReleaseHandler extends DefaultHandler {

	public SystemReleaseHandler(Context context) {
		super(context);
	}

	@Override
	public Object handleRequest(HttpRequest request, HttpContext context, Uri requestLine) {
		return android.os.Build.VERSION.RELEASE;
	}

}
