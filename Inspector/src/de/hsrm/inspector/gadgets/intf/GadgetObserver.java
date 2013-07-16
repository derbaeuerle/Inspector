package de.hsrm.inspector.gadgets.intf;

/**
 * Observer pattern interface for observers on {@link Gadget} objects.
 */
public interface GadgetObserver {

	/**
	 * Possible events of which a {@link GadgetObserver} can be notified.
	 */
	public enum EVENT {
		DESTROY
	};

	/**
	 * Notify every registered {@link GadgetObserver} of {@link EVENT} and
	 * notifying {@link Gadget}.
	 * 
	 * @param event
	 *            {@link EVENT}
	 * @param gadget
	 *            {@link Gadget}
	 */
	public void notifyGadgetEvent(EVENT event, Gadget gadget);

}
