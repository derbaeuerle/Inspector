package de.hsrm.audioapi.gadgets;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.net.Uri;
import de.hsrm.audioapi.service.AudioService;
import de.inspector.hsrm.gadgets.OldGadget;

public class AudioOldGadget extends OldGadget {

	@Override
	public Object gogo(HttpRequest request, HttpContext context, Uri requestLine) throws Exception {
		String url = requestLine.toString();
		if (url.contains("/play?")) {
			((AudioService) getBoundService()).play(request);
		} else if (url.contains("/pause?")) {
			((AudioService) getBoundService()).pause(request);
		} else if (url.contains("/stop?")) {
			((AudioService) getBoundService()).stop(request);
		}
		return requestLine.getLastPathSegment();
	}

}
