package de.hsrm.inspector.gadgets.pool;

import com.google.gson.Gson;

/**
 * Specific {@link GadgetEvent} class for system events. Currently there are
 * only two system events (screen on/off) to be published to the browser.
 */
public class SystemEvent extends GadgetEvent {

	public SystemEvent(Object response, EVENT_TYPE event) {
		super(null, response, event);
	}

	/**
	 * Returns <em>SYSTEM</em> as identifier for client.
	 * 
	 * @return {@link String}
	 */
	public String getName() {
		return "SYSTEM";
	}

	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("system: ");
		b.append("type: ");
		b.append(getEvent().name());
		b.append(", ");
		b.append("response: ");
		b.append((new Gson()).toJson(getResponse()));
		return b.toString();
	}

}
