package de.hsrm.inspector.gadgets.pool;

import com.google.gson.Gson;

import de.hsrm.inspector.gadgets.intf.Gadget;

public class GadgetEvent {

	public static enum EVENT_TYPE {
		DATA, STATE, ERROR, FEEDBACK, DESTROY
	};

	private Gadget mGadget;
	private Object mResponse;
	private EVENT_TYPE mEvent;

	public GadgetEvent(Gadget gadget, Object response, EVENT_TYPE event) {
		mGadget = gadget;
		mResponse = response;
		mEvent = event;
	}

	public Gadget getGadget() {
		return mGadget;
	}

	public Object getResponse() {
		return mResponse;
	}

	public EVENT_TYPE getEvent() {
		return mEvent;
	}

	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("gadget: ");
		b.append(mGadget.getIdentifier());
		b.append(", ");
		b.append("type: ");
		b.append(mEvent.name());
		b.append(", ");
		b.append("response: ");
		b.append((new Gson()).toJson(mResponse));
		return b.toString();
	}

}
