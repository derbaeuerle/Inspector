package de.hsrm.audioapi.gadgets;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.net.Uri;
import de.inspector.hsrm.gadgets.OldGadget;

public class IntegerOldGadget extends OldGadget {

	@Override
	public Object gogo(HttpRequest request, HttpContext context, Uri requestLine) throws Exception {
		return requestLine.getLastPathSegment();
	}

}
