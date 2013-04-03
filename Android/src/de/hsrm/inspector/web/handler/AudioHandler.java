package de.hsrm.inspector.web.handler;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.app.Service;
import android.net.Uri;
import de.hsrm.inspector.web.defaults.DefaultServiceHandler;

public class AudioHandler extends DefaultServiceHandler {

	@Override
	public Object gogo(HttpRequest request, HttpContext context, Uri requestLine, Service service) throws Exception {
		return requestLine.getLastPathSegment();
	}

	@Override
	public Object gogo(HttpRequest request, HttpContext context, Uri requestLine) throws Exception {
		return requestLine.getLastPathSegment();
	}

}
