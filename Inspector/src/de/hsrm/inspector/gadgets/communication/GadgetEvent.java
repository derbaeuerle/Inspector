package de.hsrm.inspector.gadgets.communication;

import de.hsrm.inspector.gadgets.intf.Gadget;

public class GadgetEvent {

	private Gadget mGadget;
	private Object mResponse;
	private String mEvent;

	public GadgetEvent(Gadget gadget, Object response, String event) {
		mGadget = gadget;
		mResponse = response;
		mEvent = event.toLowerCase();
	}

	public Gadget getGadget() {
		return mGadget;
	}

	public Object getResponse() {
		return mResponse;
	}

	public String getEvent() {
		return mEvent;
	}

}
