package de.hsrm.inspector.gadgets.intf;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import de.hsrm.inspector.exceptions.GadgetException;
import de.hsrm.inspector.gadgets.intf.GadgetObserver.EVENT;
import de.hsrm.inspector.gadgets.utils.TimeoutTimer;
import de.hsrm.inspector.handler.utils.InspectorRequest;

/**
 * Created by dobae on 25.05.13.
 */
public abstract class Gadget {

	private GadgetObserver mObserver;

	private String mIdentifier;
	private Class<Gadget> mClass;
	private boolean mKeepAlive;
	private long mTimeout;
	private TimeoutTimer mTimeoutTimer;

	public Gadget() {
		this("", de.hsrm.inspector.gadgets.intf.Gadget.class);
	}

	/**
	 * Constructor for configuration objects.
	 * 
	 * @param identifier
	 * @param clazz
	 */
	public Gadget(String identifier, Class<Gadget> clazz) {
		super();
		mIdentifier = identifier;
		mClass = clazz;
	}

	/**
	 * Creating instances of this Gadget for runtime.
	 * 
	 * @param context
	 *            {Context}
	 * @return {Gadget}
	 */
	public Gadget createInstance(Context context) throws GadgetException {
		Gadget g = null;
		try {
			g = (Gadget) mClass.newInstance();
			g.setIdentifier(mIdentifier);
			g.setKeepAlive(mKeepAlive);
			g.setTimeout(mTimeout);
			g.onCreate(context);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
		return g;
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
	public void onRegister(Context context) {
	}

	/**
	 * Gets called when a instance of this gadget gets unregistered to runtime
	 * process.
	 * 
	 * @param context
	 *            {Context}
	 */
	public void onUnregister(Context context) {
	}

	/**
	 * Handles a request from browser for this gadget and returns Object which
	 * will be serialized to JSON.
	 * 
	 * @param context
	 *            {Context}
	 * @param iRequest
	 *            {InspectorRequest}
	 * @param request
	 *            {HttpRequest}
	 * @param response
	 *            {HttpResponse}
	 * @param http_context
	 *            {HttpContext}
	 * @return {Object}
	 * @throws Exception
	 */
	public abstract Object gogo(Context context, InspectorRequest iRequest, HttpRequest request, HttpResponse response,
			HttpContext http_context) throws Exception;

	public String getIdentifier() {
		return mIdentifier;
	}

	public Gadget setIdentifier(String identifier) {
		mIdentifier = identifier;
		return this;
	}

	public void setClass(Class<Gadget> clazz) {
		this.mClass = clazz;
	}

	public boolean isKeepAlive() {
		return mKeepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.mKeepAlive = keepAlive;
	}

	public long getTimeout() {
		return mTimeout;
	}

	public void setTimeout(long timeout) {
		this.mTimeout = timeout;
	}

	public void startTimeout() {
		if (mTimeoutTimer != null) {
			mTimeoutTimer.start();
		}
	}

	public void cancelTimeout() {
		if (mTimeoutTimer != null) {
			mTimeoutTimer.cancel();
		}
	}

	public void setObserver(GadgetObserver observer) {
		mObserver = observer;
	}

}
