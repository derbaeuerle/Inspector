package de.inspector.hsrm.service.utils;

import java.util.concurrent.Callable;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.net.Uri;
import de.inspector.hsrm.gadgets.Gadget;

public class AsyncServiceBinderCallable implements Callable<Object> {
	private Gadget mGadget;
	private HttpRequest mRequest;
	private HttpContext mContext;
	private Uri mRequestLine;

	public AsyncServiceBinderCallable(Gadget gadget, HttpRequest request, HttpContext context, Uri requestLine) {
		mGadget = gadget;
		mRequest = request;
		mContext = context;
		mRequestLine = requestLine;
	}

	@Override
	public Object call() throws Exception {
		return mGadget.gogo(mRequest, mContext, mRequestLine);
	}

	public Gadget getGadget() {
		return mGadget;
	}

}