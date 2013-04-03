package de.hsrm.inspector.web.handler;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.net.Uri;
import de.hsrm.inspector.web.defaults.DefaultHandler;

public class SystemReleaseHandler extends DefaultHandler {

	public SystemReleaseHandler(Context context) {
		super(context);
	}

	@Override
	public Object gogo(HttpRequest request, HttpContext context, Uri requestLine) throws Exception {
		return android.os.Build.VERSION.RELEASE;
	}

}
