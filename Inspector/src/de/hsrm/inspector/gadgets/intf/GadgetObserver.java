package de.hsrm.inspector.gadgets.intf;

import de.hsrm.inspector.gadgets.pool.GadgetEvent;

/**
 * Observer pattern interface for observers on {@link Gadget} objects.
 */
public interface GadgetObserver {

	/**
	 * Notify every registered {@link GadgetObserver} of {@link EVENT} and
	 * notifying {@link Gadget}.
	 * 
	 * @param event
	 *            {@link GadgetEvent}
	 */
	public void onGadgetEvent(GadgetEvent event);

}
