package de.hsrm.inspector.gadgets.pool;

import com.google.gson.Gson;

/**
 * Specific {@link GadgetEvent} class for system events. Currently there are
 * only two system events (screen on/off) to be published to the browser.
 */
public class SystemEvent extends GadgetEvent {

	/**
	 * Constructor of {@link SystemEvent} calls <b>super</b> of
	 * {@link GadgetEvent}.
	 * 
	 * @param response
	 *            {@link Object}
	 * @param event
	 *            {@link EVENT_TYPE}
	 */
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

	/**
	 * Returns all attributes of this {@link SystemEvent} and
	 * {@link #getResponse()} parsed into JSON format.
	 * 
	 * @return {@link String}
	 */
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
