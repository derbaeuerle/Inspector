package de.hsrm.inspector.gadgets.intf;

import de.hsrm.inspector.handler.GadgetHandler;
import de.hsrm.inspector.handler.utils.InspectorRequest;

/**
 * Listener for keep-alive messages. {@link Gadget} can implement this listener
 * to receive keep-alive messages and do some management.
 */
public interface OnKeepAliveListener {

	/**
	 * Gets called when {@link GadgetHandler} receives a keep-alive message for
	 * implementing {@link Gadget}.
	 * 
	 * @param iRequest
	 *            {@link InspectorRequest}
	 */
	public void onKeepAlive(InspectorRequest iRequest);

}
