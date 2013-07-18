package de.hsrm.inspector.gadgets.intf;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Service;
import android.content.Context;
import android.content.ServiceConnection;
import android.preference.PreferenceScreen;
import android.util.Log;
import de.hsrm.inspector.gadgets.communication.GadgetEvent;
import de.hsrm.inspector.gadgets.communication.GadgetRequest;
import de.hsrm.inspector.gadgets.communication.ResponsePool;
import de.hsrm.inspector.gadgets.intf.GadgetObserver.EVENT;
import de.hsrm.inspector.gadgets.utils.TimeoutTimer;
import de.hsrm.inspector.handler.utils.InspectorRequest;
import de.hsrm.inspector.services.utils.AsyncServiceBinder;
import de.hsrm.inspector.services.utils.AsyncServiceBinderCallable;
import de.hsrm.inspector.services.utils.ServiceBinder;

/**
 * A {@link Gadget} represents a callable module of inspector.
 */
public abstract class Gadget {

	private GadgetObserver mObserver;

	private ResponsePool mResponsePool;

	private String mIdentifier;
	private String mPreferences;
	private boolean mKeepAlive;
	private long mTimeout;
	private int mPermissionType;
	private TimeoutTimer mTimeoutTimer;
	private AtomicBoolean mRunning, mProcessing;
	private HashMap<Service, ServiceConnection> mServicesBound;
	private ConcurrentLinkedQueue<GadgetRequest> mRequests;
	private GadgetEvent mLastEvent;

	public Gadget() {
		this("");
	}

	/**
	 * Constructor for configuration objects.
	 * 
	 * @param identifier
	 * @param clazz
	 */
	public Gadget(String identifier) {
		super();
		mRequests = new ConcurrentLinkedQueue<GadgetRequest>();
		mServicesBound = new HashMap<Service, ServiceConnection>();
		mRunning = new AtomicBoolean(false);
		mProcessing = new AtomicBoolean(false);
		mIdentifier = identifier;
	}

	/**
	 * Gets called when a instance of this gadget gets created.
	 * 
	 * @param context
	 *            {Context}
	 */
	public void onCreate(Context context) {
		if (mTimeout != 0) {
			mTimeoutTimer = new TimeoutTimer(context, this);
		}
	}

	/**
	 * Gets called when a instance of this gadget gets removed from runtime
	 * process.
	 * 
	 * @param context
	 *            {Context}
	 */
	public void onDestroy(Context context) {
		if (mObserver != null) {
			mObserver.notifyGadgetEvent(EVENT.DESTROY, this);
		}
	}

	/**
	 * Gets called when a instance of this gadget gets registered to runtime
	 * process.
	 * 
	 * @param context
	 *            {Context}
	 */
	public void onProcessStart(Context context) {
		setRunning(true);
	}

	/**
	 * Gets called when a instance of this gadget gets unregistered to runtime
	 * process.
	 * 
	 * @param context
	 *            {Context}
	 */
	public void onProcessEnd(Context context) {
		setRunning(false);
	}

	/**
	 * Gets commands from web application to manage this component. Also there
	 * will be messages to keep the {@link Gadget} alive, which can be
	 * determined over the {@link InspectorRequest#getCommand()} method.
	 * 
	 * @param context
	 *            {Context}
	 * @param iRequest
	 *            {InspectorRequest}
	 * @throws Exception
	 */
	public abstract void gogo(Context context, InspectorRequest iRequest) throws Exception;

	/**
	 * 
	 */
	public void process() {
		if (mProcessing.get()) {
			return;
		}
		mProcessing.set(true);
		(new Runnable() {

			@Override
			public void run() {

				while (isRunning() && mProcessing.get()) {
					if (mLastEvent != null) {
						synchronized (mLastEvent) {
							mResponsePool.add("", mLastEvent);
							mLastEvent = null;
						}
					}
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).run();
	}

	/**
	 * Adds an {@link GadgetRequest} to the request queue {@link #mRequests}.
	 * 
	 * @param request
	 *            {@link GadgetRequest}
	 */
	public synchronized void addRequest(GadgetRequest request) {
		mRequests.add(request);
	}

	/**
	 * Set {@link #mLastEvent} of this {@link Gadget} to publish the last event
	 * to the {@link ResponsePool}. Currently only the last event will be
	 * published.
	 * 
	 * @param event
	 *            {@link Gadget}
	 */
	public void notifyGadgetEvent(GadgetEvent event) {
		if (mLastEvent == null) {
			mLastEvent = event;
		}
	}

	/**
	 * Binds a {@link Service} to this {@link Gadget} and returns the bound
	 * service. To bind a service correctly the service needs to use the
	 * {@link ServiceBinder}.
	 * 
	 * @param service
	 *            {@link Class} of the service to bind.
	 * @param context
	 *            Current application {@link Context}.
	 * @return {@link Service}
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws ClassNotFoundException
	 */
	public Service bindService(Class<Service> service, Context context) throws ExecutionException,
			InterruptedException, ClassNotFoundException {
		AsyncServiceBinderCallable callable = new AsyncServiceBinderCallable() {

			@Override
			public Object onCall() {
				return getService();
			}
		};
		AsyncServiceBinder binder = new AsyncServiceBinder(callable, context);
		Service s = (Service) binder.process(service);
		mServicesBound.put(s, callable.getConnection());
		return s;
	}

	/**
	 * Unbind a specific service from this {@link Gadget}. Returning
	 * <code>true</code> if service is unbound, else <code>false</code>.
	 * 
	 * @param service
	 *            {@link Service} to unbind.
	 * @param context
	 *            Current application {@link Context}
	 * @return {@link Boolean}
	 */
	public boolean unbindService(Service service, Context context) {
		if (mServicesBound.containsKey(service)) {
			context.unbindService(mServicesBound.get(service));
			mServicesBound.remove(service);
			return true;
		}
		return false;
	}

	/**
	 * Unbind all bound services from this {@link Gadget}. If an error occurs in
	 * one unbind process, the return value will be <code>false</code>.
	 * 
	 * @param context
	 *            Current application {@link Context}
	 * @return {@link Boolean}
	 */
	public boolean unbindAllServices(Context context) {
		try {
			for (Service s : mServicesBound.keySet()) {
				unbindService(s, context);
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Returns identification {@link String} of {@link Gadget}.
	 * 
	 * @return {@link String}
	 */
	public String getIdentifier() {
		return mIdentifier;
	}

	/**
	 * Sets {@link #mIdentifier} to given {@link String}.
	 * 
	 * @param identifier
	 *            {@link String} to set.
	 * @return {@link Gadget}
	 */
	public Gadget setIdentifier(String identifier) {
		mIdentifier = identifier;
		return this;
	}

	/**
	 * Returns true if {@link Gadget} is already an active module.
	 * 
	 * @return {@link Boolean}
	 */
	public boolean isRunning() {
		return mRunning.get();
	}

	/**
	 * Sets {@link #mRunning}.
	 * 
	 * @param running
	 *            {@link Boolean} to set.
	 */
	public void setRunning(boolean running) {
		mRunning.set(running);
	}

	/**
	 * Returns true if {@link Gadget} is configured as keep-alive module.
	 * 
	 * @return {@link Boolean}
	 */
	public boolean isKeepAlive() {
		return mKeepAlive;
	}

	/**
	 * Sets {@link #mKeepAlive}.
	 * 
	 * @param keepAlive
	 *            {@link Boolean} to set.
	 */
	public void setKeepAlive(boolean keepAlive) {
		this.mKeepAlive = keepAlive;
	}

	/**
	 * Returns configured permission type for this {@link Gadget}. All values
	 * are stored inside resource files.
	 * 
	 * @return {@link Integer}
	 */
	public int getPermissionType() {
		return mPermissionType;
	}

	/**
	 * Sets {@link #mPermissionType}.
	 * 
	 * @param pType
	 *            {@link Integer} to set.
	 */
	public void setPermissionType(int pType) {
		this.mPermissionType = pType;
	}

	/**
	 * Sets {@link #mPreferences}. This attribute describes the name of
	 * {@link PreferenceScreen} file inside the resource xml folder.
	 * 
	 * @param prefs
	 *            {@link String} to set.
	 */
	public void setPreferences(String prefs) {
		this.mPreferences = prefs;
	}

	/**
	 * Returns configured {@link PreferenceScreen} file name.
	 * 
	 * @return {@link String}
	 */
	public String getPreferences() {
		return this.mPreferences;
	}

	/**
	 * Returns configured timeout of this {@link Gadget} in milliseconds.
	 * 
	 * @return {@link Long}
	 */
	public long getTimeout() {
		return mTimeout;
	}

	/**
	 * Sets {@link #mTimeout} in milliseconds of this {@link Gadget}.
	 * 
	 * @param timeout
	 *            {@link Long} to set.
	 */
	public void setTimeout(long timeout) {
		this.mTimeout = timeout;
	}

	/**
	 * Starts the {@link #mTimeoutTimer}.
	 */
	public void startTimeout() {
		Log.d("GADGET", getIdentifier() + ": start timeout");
		if (mTimeoutTimer != null) {
			mTimeoutTimer.start();
		}
	}

	/**
	 * Stops the {@link #mTimeoutTimer}.
	 */
	public void cancelTimeout() {
		if (mTimeoutTimer != null) {
			Log.d("GADGET", getIdentifier() + ": cancel timeout");
			mTimeoutTimer.cancel();
		}
	}

	/**
	 * Sets {@link #mObserver} to given {@link GadgetObserver}.
	 * 
	 * @param observer
	 *            {@link GadgetObserver} to set.
	 */
	public void setObserver(GadgetObserver observer) {
		mObserver = observer;
	}

	/**
	 * Removes set {@link GadgetObserver}.
	 */
	public void removeObserver() {
		mObserver = null;
	}

	/**
	 * Turn {@link #mProcessing} to <code>false</code> to stop handle the
	 * {@link #mLastEvent}.
	 * 
	 * @param processing
	 *            {@link Boolean}
	 */
	public void setProcessing(boolean processing) {
		mProcessing.set(processing);
	}

	/**
	 * Setter for {@link #mResponsePool}.
	 * 
	 * @param queue
	 *            {@link ResponsePool}
	 */
	public void setResponseQueue(ResponsePool queue) {
		if (mResponsePool != null) {
			return;
		}
		mResponsePool = queue;
	}

}
