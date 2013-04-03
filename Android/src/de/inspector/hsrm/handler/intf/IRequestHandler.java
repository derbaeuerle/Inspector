package de.inspector.hsrm.handler.intf;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.net.Uri;

public interface IRequestHandler {

	public Object gogo(HttpRequest request, HttpContext context, Uri requestLine);

}
