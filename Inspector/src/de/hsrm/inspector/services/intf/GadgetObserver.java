package de.hsrm.inspector.services.intf;

public interface GadgetObserver {

	public enum EVENT {
		DESTROY
	};

	public void notifyGadgetEvent(EVENT event, Gadget gadget);

}
