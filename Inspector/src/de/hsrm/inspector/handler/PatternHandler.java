package de.hsrm.inspector.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import de.hsrm.inspector.R;
import de.hsrm.inspector.exceptions.GadgetException;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.gadgets.intf.GadgetObserver;
import de.hsrm.inspector.handler.utils.InspectorRequest;

public class PatternHandler implements HttpRequestHandler, GadgetObserver {

	private ConcurrentHashMap<String, Gadget> mGadgets;
	private ConcurrentHashMap<String, ArrayList<String>> mPermissions;
	private AtomicBoolean mLocked = new AtomicBoolean(false);
	private Context mContext;
	private AtomicInteger mRunningInstances;

	public PatternHandler(Context context) {
		mContext = context;
		mRunningInstances = new AtomicInteger(0);
		mPermissions = new ConcurrentHashMap<String, ArrayList<String>>();
	}

	private void stopServerTimeout() {
		String uri = "inspect://stop-timeout";
		Intent request = new Intent("de.inspector.intents");
		request.setData(Uri.parse(uri));
		mContext.startService(request);
	}

	private void startServerTimeout() {
		String uri = "inspect://start-timeout";
		Intent request = new Intent("de.inspector.intents");
		request.setData(Uri.parse(uri));
		mContext.startService(request);
	}

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException,
			IOException {
		stopServerTimeout();

		Object tmpResponseContent = "";
		final String responseContent;
		Gson gson = new Gson();
		Log.d("REQUEST", request.getRequestLine().toString());
		try {
			InspectorRequest iRequest = new InspectorRequest(request);
			if (!mLocked.get()
					|| (mLocked.get() && mGadgets.containsKey(iRequest.getGadgetIdentifier()) && mGadgets.get(
							iRequest.getGadgetIdentifier()).isKeepAlive())) {
				if (iRequest.getSegments().contains("destroy")) {
					try {
						destroyGadget(iRequest.getGadgetIdentifier());
						tmpResponseContent = "DESTROYED";
					} catch (Exception e) {
						tmpResponseContent = "ERROR";
					}

				} else {
					if (mGadgets.containsKey(iRequest.getGadgetIdentifier())) {
						final Gadget g = mGadgets.get(iRequest.getGadgetIdentifier());

						if (iRequest.hasParameter("permission")
								&& iRequest.getParameter("permission").toString().equals("true")) {
							// mPermissions.get(g.getIdentifier()).add(iRequest.getParameter("hash").toString());
							g.auth();
						}

						if (g.getAuthType() == Integer.parseInt(mContext.getString(R.string.auth_type_permission))
								&& !g.isAuthGranted()) {
							// if(!mPermissions.get(g.getIdentifier()).contains(iRequest.getParameter("hash").toString()))
							// {
							//
							// }
							tmpResponseContent = "{ 'error': { 'message': '" + g.getIdentifier()
									+ " needs permission', 'code': 2 } }";
						} else if (g.getAuthType() == Integer.parseInt(mContext.getString(R.string.auth_type_disabled))) {
							tmpResponseContent = "{ 'error': { 'message': 'Gadget is disabled', 'code': 3 } }";
						} else {
							initGadget(iRequest.getGadgetIdentifier());

							synchronized (g) {
								g.bindServices();
								tmpResponseContent = g.gogo(mContext, iRequest, request, response, context);
								g.unbindServices();
								g.startTimeout();
							}
							tmpResponseContent = gson.toJson(tmpResponseContent);
						}
					}
				}

			} else if (mLocked.get()
					&& (mGadgets.containsKey(iRequest.getGadgetIdentifier()) && !mGadgets.get(
							iRequest.getGadgetIdentifier()).isRunning())) {
				tmpResponseContent = "{ 'error': { 'message': 'Server is locked', 'code': 1 } }";
			}
			tmpResponseContent = iRequest.getCallback() + "(" + tmpResponseContent + ");";
		} catch (Exception e) {
			e.printStackTrace();
			StringBuilder b = new StringBuilder();
			for (StackTraceElement s : e.getStackTrace()) {
				b.append(s.toString() + "\n");
			}
			HashMap<String, String> map = new HashMap<String, String>();
			map.put(mContext.getString(R.string.exception_message), e.getMessage());
			map.put(mContext.getString(R.string.exception_stacktrace), b.toString());
			tmpResponseContent = "{ 'error': " + gson.toJson(map) + "}";
		}
		response.setHeader("Content-Type", "application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods", "*");
		responseContent = tmpResponseContent.toString();
		HttpEntity entity = new EntityTemplate(new ContentProducer() {
			public void writeTo(final OutputStream outputStream) throws IOException {
				OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
				writer.write(responseContent);
				writer.flush();
			}
		});
		Log.d("RESPONSE", tmpResponseContent.toString());
		response.setEntity(entity);
	}

	@Override
	public void notifyGadgetEvent(EVENT event, Gadget gadget) {
		if (mGadgets.containsKey(gadget.getIdentifier())) {
			if (event == EVENT.DESTROY) {
				gadget.removeObserver();
				mRunningInstances.decrementAndGet();
			}
		}
		if (mRunningInstances.get() == 0) {
			startServerTimeout();
		}
	}

	public void setGadgetConfiguration(ConcurrentHashMap<String, Gadget> config) {
		mGadgets = config;
		for (Gadget g : config.values()) {
			mPermissions.put(g.getIdentifier(), new ArrayList<String>());
		}
	}

	public void initGadget(String identifier) throws GadgetException {
		if (mGadgets.containsKey(identifier)) {
			Gadget g = mGadgets.get(identifier);
			if (g.isRunning()) {
				g.cancelTimeout();
				return;
			} else {
				if (mLocked.get()) {
					throw new GadgetException("Unable to create a new gadget instance if server if locked!");
				}
				g.onCreate(mContext);
				g.onRegister(mContext);
				g.setObserver(this);
				mRunningInstances.incrementAndGet();
				return;
			}
		}
		throw new GadgetException("Unknown gadget identifier: " + identifier);
	}

	public void destroyGadget(String identifier) throws Exception {
		Gadget g = mGadgets.get(identifier);
		synchronized (g) {
			g.onUnregister(mContext);
			g.onDestroy(mContext);
			g.removeObserver();
			mRunningInstances.decrementAndGet();
		}
	}

	public void lock() {
		mLocked.set(true);
		for (String key : mGadgets.keySet()) {
			Gadget instance = mGadgets.get(key);
			if (!instance.isKeepAlive()) {
				synchronized (instance) {
					instance.onUnregister(mContext);
					instance.onDestroy(mContext);
					instance.removeObserver();
				}
			}
		}
	}

	public void unlock() {
		mLocked.set(false);
	}
}
