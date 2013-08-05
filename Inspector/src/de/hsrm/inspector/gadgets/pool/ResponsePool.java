package de.hsrm.inspector.gadgets.pool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.gadgets.pool.GadgetEvent.EVENT_TYPE;

/**
 * Pool of processed {@link GadgetRequest} objects. Only holds the last
 * {@link GadgetEvent} of a {@link Gadget} to avoid redundant data transfers.
 */
public class ResponsePool {

	private ConcurrentHashMap<String, ConcurrentLinkedQueue<GadgetEvent>> mResponsePool;
	private ConcurrentHashMap<String, ConcurrentHashMap<Gadget, GadgetEvent>> mDataEvents;
	private ConcurrentHashMap<String, Set<Gadget>> mBrowserInstances;

	public ResponsePool() {
		mResponsePool = new ConcurrentHashMap<String, ConcurrentLinkedQueue<GadgetEvent>>();
		mDataEvents = new ConcurrentHashMap<String, ConcurrentHashMap<Gadget, GadgetEvent>>();
		mBrowserInstances = new ConcurrentHashMap<String, Set<Gadget>>();
	}

	/**
	 * 
	 * @param id
	 * @param gadget
	 */
	public void addBrowserGadget(String id, Gadget gadget) {
		if (!mBrowserInstances.containsKey(id)) {
			mBrowserInstances.put(id, new HashSet<Gadget>());
		}
		if (!mBrowserInstances.get(id).contains(gadget)) {
			mBrowserInstances.get(id).add(gadget);
		}
	}

	/**
	 * 
	 * @param gadget
	 */
	public void removeGadget(Gadget gadget) {
		synchronized (mBrowserInstances) {
			for (String id : mBrowserInstances.keySet()) {
				// Remove events from browser instance.
				if (mBrowserInstances.get(id).contains(gadget)) {
					synchronized (mDataEvents) {
						if (mDataEvents.containsKey(id)) {
							mDataEvents.get(id).remove(gadget);
						}
					}
					synchronized (mResponsePool) {
						if (mResponsePool.containsKey(id)) {
							Iterator<GadgetEvent> i = mResponsePool.get(id).iterator();
							while (i.hasNext()) {
								GadgetEvent ev = i.next();
								if (ev.getGadget().equals(gadget)) {
									i.remove();
								}
							}
						}
					}
				}
				mBrowserInstances.get(id).remove(gadget);
			}
		}
	}

	/**
	 * Add a new {@link GadgetEvent} to the pool. If {@link GadgetEvent} is
	 * {@link EVENT_TYPE#DATA} only the last added {@link GadgetEvent} will be
	 * published to web application.
	 * 
	 * @param response
	 *            {@link GadgetEvent}
	 */
	public void add(GadgetEvent response) {
		// If response is an SystemEvent, all browser instances should be
		// notified.
		if (response instanceof SystemEvent) {
			for (String id : mBrowserInstances.keySet()) {
				if (!mResponsePool.contains(id)) {
					mResponsePool.put(id, new ConcurrentLinkedQueue<GadgetEvent>());
				}
				mResponsePool.get(id).add((SystemEvent) response);
			}
		} else {
			for (String id : mBrowserInstances.keySet()) {
				// Check each browser id, if instance uses this gadget.
				if (mBrowserInstances.get(id).contains(response.getGadget())) {
					// If event is data event.
					if (response.getEvent().equals(EVENT_TYPE.DATA)) {
						if (!mDataEvents.containsKey(id)) {
							mDataEvents.put(id, new ConcurrentHashMap<Gadget, GadgetEvent>());
						}
						mDataEvents.get(id).put(response.getGadget(), response);
					} else {
						if (!mResponsePool.contains(id)) {
							mResponsePool.put(id, new ConcurrentLinkedQueue<GadgetEvent>());
						}
						mResponsePool.get(id).add(response);
					}
				}
			}
		}
	}

	/**
	 * Add all {@link GadgetEvent} of a {@link List} to the response pool.
	 * 
	 * @param events
	 *            {@link List} of {@link GadgetEvent}
	 */
	public void addAll(List<GadgetEvent> events) {
		for (GadgetEvent e : events) {
			add(e);
		}
	}

	/**
	 * Get all {@link GadgetEvent} objects for one web application.
	 * 
	 * @param id
	 *            {@link String}
	 * @return {@link List} of {@link GadgetEvent}
	 */
	public List<GadgetEvent> popAll(String id) {
		ArrayList<GadgetEvent> responses = new ArrayList<GadgetEvent>();

		synchronized (mResponsePool) {
			if (mResponsePool.containsKey(id)) {
				while (!mResponsePool.get(id).isEmpty()) {
					responses.add(mResponsePool.get(id).poll());
				}
			}
		}
		synchronized (mDataEvents) {
			if (mDataEvents.containsKey(id)) {
				for (GadgetEvent e : mDataEvents.get(id).values()) {
					responses.add(e);
				}
				mDataEvents.get(id).clear();
			}
		}

		return responses;
	}

	/**
	 * Returns <code>true</code> if {@link #mResponses} contains a pool for
	 * given parameter and this pool isn't empty.
	 * 
	 * @param id
	 *            {@link String}
	 * @return {@link Boolean}
	 */
	public boolean hasItems(String id) {
		return (mResponsePool.containsKey(id) && !mResponsePool.get(id).isEmpty())
				|| (mDataEvents.containsKey(id) && !mDataEvents.get(id).isEmpty());
	}

	/**
	 * Returns size of {@link #mDataEvents} and {@link #mResponsePool} for given
	 * browser instance.
	 * 
	 * @param id
	 *            {@link String}
	 * @return {@link Integer}
	 */
	public int size(String id) {
		int size = 0;
		synchronized (mDataEvents) {
			if (mDataEvents.get(id) != null) {
				size += mDataEvents.get(id).size();
			}
		}
		synchronized (mResponsePool) {
			if (mResponsePool.get(id) != null) {
				size += mResponsePool.get(id).size();
			}
		}
		return size;
	}

	/**
	 * Clears all events for given browser instance.
	 * 
	 * @param id
	 *            {@link String}
	 */
	public void clear(String id) {
		synchronized (mDataEvents) {
			if (mDataEvents.containsKey(id)) {
				mDataEvents.get(id).clear();
			}
		}
		synchronized (mResponsePool) {
			if (mResponsePool.containsKey(id)) {
				mResponsePool.get(id).clear();
			}
		}
	}

	/**
	 * Clears entire response pool.
	 */
	public void clearAll() {
		synchronized (mDataEvents) {
			mDataEvents.clear();
		}
		synchronized (mBrowserInstances) {
			mBrowserInstances.clear();
		}
		synchronized (mResponsePool) {
			mResponsePool.clear();
		}
	}
}
