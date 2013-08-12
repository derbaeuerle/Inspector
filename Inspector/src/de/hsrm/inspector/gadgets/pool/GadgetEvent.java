package de.hsrm.inspector.gadgets.pool;

import org.apache.http.HttpResponse;

import com.google.gson.Gson;

import de.hsrm.inspector.gadgets.intf.Gadget;

/**
 * This class represents events based on {@link Gadget} data which will be send
 * to {@link ResponsePool}.
 */
public class GadgetEvent {

	/**
	 * Type of event.
	 */
	public static enum EVENT_TYPE {
		DATA, STATE, ERROR, FEEDBACK, DESTROY
	};

	/** Identifies notifying {@link Gadget}. */
	private Gadget mGadget;
	/** Data to write into {@link HttpResponse} message. */
	private Object mResponse;
	/** {@link EVENT_TYPE} of this {@link GadgetEvent}. */
	private EVENT_TYPE mEvent;

	/**
	 * Constructor of {@link GadgetEvent}.
	 * 
	 * @param gadget
	 *            {@link Gadget}
	 * @param response
	 *            {@link Object}
	 * @param event
	 *            {@link EVENT_TYPE}
	 */
	public GadgetEvent(Gadget gadget, Object response, EVENT_TYPE event) {
		mGadget = gadget;
		mResponse = response;
		mEvent = event;
	}

	/**
	 * Returns {@link #mGadget}.
	 * 
	 * @return {@link Gadget}
	 */
	public Gadget getGadget() {
		return mGadget;
	}

	/**
	 * Returns {@link #mResponse}.
	 * 
	 * @return {@link Object}
	 */
	public Object getResponse() {
		return mResponse;
	}

	/**
	 * Returns {@link #mEvent}.
	 * 
	 * @return {@link EVENT_TYPE}
	 */
	public EVENT_TYPE getEvent() {
		return mEvent;
	}

	/**
	 * Returns all attributes of this {@link GadgetEvent} with
	 * {@link #mResponse} parsed into JSON format.
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
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
