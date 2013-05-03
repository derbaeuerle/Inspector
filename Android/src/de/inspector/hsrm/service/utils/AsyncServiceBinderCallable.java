package de.inspector.hsrm.service.utils;

import java.util.concurrent.Callable;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.net.Uri;
import de.inspector.hsrm.gadgets.Gadget;

/**
 * Default {@link Callable} class to process request and convert processed data
 * on {@link #call()}.
 * 
 * @author Dominic Baeuerle
 * 
 */
public class AsyncServiceBinderCallable implements Callable<Object> {
	private Gadget mGadget;
	private HttpRequest mRequest;
	private HttpContext mContext;
	private Uri mRequestLine;

	/**
	 * Constructor of {@link AsyncServiceBinderCallable}.
	 * 
	 * @param gadget
	 *            {@link Gadget} to start on {@link #call()}.
	 * @param request
	 *            {@link HttpRequest} of restful request.
	 * @param context
	 *            {@link HttpContext} of restful request.
	 * @param requestLine
	 *            {@link Uri} of restful request.
	 */
	public AsyncServiceBinderCallable(Gadget gadget, HttpRequest request, HttpContext context, Uri requestLine) {
		mGadget = gadget;
		mRequest = request;
		mContext = context;
		mRequestLine = requestLine;
	}

	@Override
	public Object call() throws Exception {
		return mGadget.getConverter().convert(mGadget.gogo(mRequest, mContext, mRequestLine));
	}

	/**
	 * Returns configured {@link Gadget} of this
	 * {@link AsyncServiceBinderCallable}.
	 * 
	 * @return {@link Gadget}
	 */
	public Gadget getGadget() {
		return mGadget;
	}

}