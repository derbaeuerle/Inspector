package de.hsrm.inspector.gadgets.intf;

public interface GadgetObserver {

	public enum EVENT {
		DESTROY
	};

	public void notifyGadgetEvent(EVENT event, Gadget gadget);

}
