package de.hsrm.inspector.web.defaults;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.app.Service;
import android.net.Uri;

public abstract class DefaultServiceHandler extends DefaultHandler {

	private class ServiceHandler implements Runnable {

		@Override
		public void run() {
		}

	}

	public abstract Object gogo(HttpRequest request, HttpContext context, Uri requestLine, Service service)
			throws Exception;

}
