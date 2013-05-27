package de.inspector.colordemo;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.net.Uri;
import de.inspector.colordemo.service.ColorService;
import de.inspector.hsrm.gadgets.OldGadget;

public class ColorOldGadget extends OldGadget {

	@Override
	public Object gogo(HttpRequest request, HttpContext context, Uri requestLine) throws Exception {
		return ((ColorService) getBoundService()).getData();
	}

}
