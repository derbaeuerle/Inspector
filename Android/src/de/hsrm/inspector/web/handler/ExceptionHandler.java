package de.hsrm.inspector.web.handler;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.net.Uri;
import de.hsrm.inspector.web.defaults.DefaultHandler;

public class ExceptionHandler extends DefaultHandler {

	@Override
	public Object gogo(HttpRequest request, HttpContext context, Uri requestLine) throws Exception {
		throw new Exception("This handler always throws an exception!");
	}

}
