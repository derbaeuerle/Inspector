package de.hsrm.inspector.gadgets.communication;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import de.hsrm.inspector.gadgets.intf.Gadget;

/**
 * Pool of processed {@link GadgetRequest} objects. Only holds the last
 * {@link GadgetEvent} of a {@link Gadget} to avoid redundant data transfers.
 */
public class ResponsePool {

	private static final String GLOBAL_QUEUE = "GLOBAL_QUEUE";

	private ConcurrentHashMap<String, ConcurrentHashMap<Gadget, GadgetEvent>> mResponses;

	public ResponsePool() {
		mResponses = new ConcurrentHashMap<String, ConcurrentHashMap<Gadget, GadgetEvent>>();
	}

	/**
	 * Add a new {@link GadgetEvent} to the pool.
	 * 
	 * @param caller
	 *            {@link String}
	 * @param response
	 *            {@link GadgetEvent}
	 */
	public synchronized void add(String caller, GadgetEvent response) {
		if (!mResponses.containsKey(GLOBAL_QUEUE)) {
			mResponses.put(GLOBAL_QUEUE, new ConcurrentHashMap<Gadget, GadgetEvent>());
		}
		mResponses.get(GLOBAL_QUEUE).put(response.getGadget(), response);
	}

	/**
	 * Get all {@link GadgetEvent} objects for one web application.
	 * 
	 * @param caller
	 *            {@link String}
	 * @return {@link List} of {@link GadgetEvent}
	 */
	public synchronized List<GadgetEvent> popAll(String caller) {
		ArrayList<GadgetEvent> responses = new ArrayList<GadgetEvent>();
		if (mResponses.containsKey(GLOBAL_QUEUE)) {
			for (Gadget key : mResponses.get(GLOBAL_QUEUE).keySet()) {
				responses.add(mResponses.get(GLOBAL_QUEUE).get(key));
				mResponses.get(GLOBAL_QUEUE).remove(key);
			}
		}
		return responses;
	}

	/**
	 * Returns <code>true</code> if {@link #mResponses} contains a pool for
	 * given parameter and this pool isn't empty.
	 * 
	 * @param caller
	 *            {@link String}
	 * @return {@link Boolean}
	 */
	public synchronized boolean hasItems(String caller) {
		return mResponses.containsKey(GLOBAL_QUEUE) && !mResponses.get(GLOBAL_QUEUE).isEmpty();
	}

	public synchronized int size(String caller) {
		return mResponses.get(GLOBAL_QUEUE).size();
	}
}
