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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.hsrm.inspector.ExtendedApplication;
import de.hsrm.inspector.R;
import de.hsrm.inspector.constants.GadgetConstants;
import de.hsrm.inspector.exceptions.GadgetException;
import de.hsrm.inspector.exceptions.constants.GadgetExceptionConstants;
import de.hsrm.inspector.gadgets.communication.ResponsePool;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.gadgets.intf.GadgetObserver;
import de.hsrm.inspector.handler.utils.InspectorRequest;
import de.hsrm.inspector.handler.utils.JsonConverter;
import de.hsrm.inspector.services.ServerService;
import de.hsrm.inspector.web.HttpServer;

/**
 * {@link HttpRequestHandler} for {@link HttpServer} to handle every request on
 * configured network port. This {@link PatternHandler} manages all configured
 * {@link Gadget} and dispatches the {@link HttpRequest} to the {@link Gadget}.
 * 
 * @author dobae
 */
public class PatternHandler implements HttpRequestHandler, GadgetObserver {

	private final int PERMISSION_REQUEST;
	private final int PERMISSION_DISABLED;

	private ConcurrentHashMap<String, Gadget> mGadgets;
	private ConcurrentHashMap<String, ArrayList<String>> mPermissions;
	private ResponsePool mResponseQueue;
	private AtomicBoolean mLocked = new AtomicBoolean(false);
	private Context mContext;
	private AtomicInteger mRunningInstances;
	private Gson mGson;

	/**
	 * Constructor of {@link PatternHandler} with current application
	 * {@link ExtendedApplication}.
	 * 
	 * @param app
	 *            {@link ExtendedApplication}
	 */
	public PatternHandler(ExtendedApplication app) {
		mResponseQueue = app.getOrCreateResponseQueue();
		mContext = app.getApplicationContext();
		mRunningInstances = new AtomicInteger(0);
		mPermissions = new ConcurrentHashMap<String, ArrayList<String>>();
		mGson = new Gson();

		PERMISSION_REQUEST = Integer.parseInt(mContext.getString(R.string.auth_type_permission));
		PERMISSION_DISABLED = Integer.parseInt(mContext.getString(R.string.auth_type_disabled));
	}

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException,
			IOException {
		stopServerTimeout();

//		Log.e("REQUEST", request.getRequestLine().toString());

		Object tmpResponseContent = "";
		try {
			InspectorRequest iRequest = new InspectorRequest(request);
			try {
				Gadget gadget = checkGadget(iRequest);
				gadget.cancelTimeout();

				checkPermission(iRequest, gadget);

				gadget.startTimeout();
			} catch (Exception e) {
				e.printStackTrace();
				tmpResponseContent = e;
			} finally {
				response(iRequest, tmpResponseContent, response);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks permission setting of requested gadget. If gadget is disabled a
	 * {@link GadgetException} will be thrown with error code
	 * {@link GadgetExceptionConstants#GADGET_IS_DISABLED}. If gadget needs
	 * permission by user, {@link GadgetException} will be thrown with error
	 * code {@link GadgetExceptionConstants#GADGET_NEEDS_PERMISSION}.
	 * 
	 * If user already has permitted this gadget on the calling website or
	 * gadget is enabled by default, all arguments will be passed to
	 * {@link Gadget#gogo(Context, InspectorRequest, HttpRequest, HttpResponse, HttpContext)}
	 * which return value will be returned.
	 * 
	 * @param iRequest
	 *            {@link InspectorRequest}
	 * @param gadget
	 *            {@link Gadget}
	 * @return {@link Object} value of
	 *         {@link Gadget#gogo(Context, InspectorRequest, HttpRequest, HttpResponse, HttpContext)}
	 *         call
	 * @throws Exception
	 */
	private void checkPermission(InspectorRequest iRequest, Gadget gadget) throws Exception {
		if (iRequest.hasParameter("permission") && iRequest.getParameter("permission").toString().equals("true")) {
			mPermissions.get(gadget.getIdentifier()).add(iRequest.getReferer());
//			Log.e("PERM", "Adding " + iRequest.getReferer() + " to " + gadget.getIdentifier());
		}

		if (gadget.getPermissionType() == PERMISSION_REQUEST
				&& !mPermissions.get(gadget.getIdentifier()).contains(iRequest.getReferer())) {
			throw new GadgetException(gadget.getIdentifier() + " needs permission",
					GadgetExceptionConstants.GADGET_NEEDS_PERMISSION);

		} else if (gadget.getPermissionType() == PERMISSION_DISABLED) {
			throw new GadgetException(gadget.getIdentifier() + " is disabled",
					GadgetExceptionConstants.GADGET_IS_DISABLED);

		} else {
			initGadget(gadget);
			checkCommand(iRequest, gadget);
		}
	}

	/**
	 * Checks whether {@link InspectorRequest} contains an static command or
	 * not. If a command was given, the bound method will be executed.
	 * 
	 * @param iRequest
	 *            {@link InspectorRequest}
	 * @param gadget
	 *            {@link Gadget}
	 * @return {@link Object}
	 * @throws Exception
	 */
	private void checkCommand(InspectorRequest iRequest, Gadget gadget) throws Exception {
		synchronized (gadget) {
			gadget.gogo(mContext, iRequest);
		}
	}

	/**
	 * Checks if gadget identifier of request URL is available.
	 * 
	 * @param iRequest
	 *            {@link InspectorRequest}
	 * @throws GadgetException
	 *             Throws {@link GadgetException} with error code
	 *             {@link GadgetExceptionConstants#GADGET_NOT_AVAILABLE} if
	 *             gadget isn't available in {@link #mGadgets}.
	 * @return {@link Gadget}
	 */
	private Gadget checkGadget(InspectorRequest iRequest) throws GadgetException {
		if (!mGadgets.containsKey(iRequest.getGadgetIdentifier())) {
			throw new GadgetException("Gadget " + iRequest.getGadgetIdentifier() + " isn't available",
					GadgetExceptionConstants.GADGET_NOT_AVAILABLE);
		}
		return mGadgets.get(iRequest.getGadgetIdentifier());
	}

	/**
	 * Writes content of argument {@link Object} into argument
	 * {@link HttpResponse}.
	 * 
	 * @param content
	 *            {@link Object}
	 * @param response
	 *            {@link HttpResponse}
	 */
	private void response(InspectorRequest iRequest, Object content, HttpResponse response) {
		final String jsonContent;
		JsonObject obj;
		if (content instanceof Exception) {
			obj = JsonConverter.exceptionToJson((Exception) content, mContext);
		} else {
			obj = new JsonObject();
			obj.addProperty("event", "state");
			obj.add("data", mGson.toJsonTree(content));
			if (iRequest.hasParameter(GadgetConstants.PARAM_STREAM_ID)) {
				HashMap<String, String> streamInfo = new HashMap<String, String>();
				streamInfo.put("streamid", iRequest.getParameter(GadgetConstants.PARAM_STREAM_ID).toString());
				streamInfo.put("gadget", iRequest.getGadgetIdentifier());
				obj.add("stream", mGson.toJsonTree(streamInfo));
			}
		}
		content = obj.toString();
		jsonContent = iRequest.getCallback() + "(" + content.toString() + ");";

		response.setHeader("Content-Type", "application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods", "*");
		HttpEntity entity = new EntityTemplate(new ContentProducer() {
			public void writeTo(final OutputStream outputStream) throws IOException {
				OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
				writer.write(jsonContent.toString());
				writer.flush();
			}
		});
		// Log.d("RESPONSE", jsonContent.toString());
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

	/**
	 * Sets configured gadgets as {@link ConcurrentHashMap}.
	 * 
	 * @param config
	 *            {@link ConcurrentHashMap}
	 */
	public void setGadgetConfiguration(ConcurrentHashMap<String, Gadget> config) {
		mGadgets = config;
		for (Gadget g : config.values()) {
			mPermissions.put(g.getIdentifier(), new ArrayList<String>());
		}
	}

	/**
	 * Initializes gadget on request. If gadget is already initialized its
	 * timeout will be reseted otherwise {@link Gadget#onCreate(Context)} and
	 * {@link Gadget#onProcessStart(Context)} will be called and this
	 * {@link PatternHandler} will be registered as {@link GadgetObserver}.
	 * 
	 * @param gadget
	 *            {@link Gadget}
	 * @throws GadgetException
	 */
	public void initGadget(Gadget gadget) throws GadgetException {
		if (!gadget.isRunning()) {
			gadget.onCreate(mContext);
			gadget.onProcessStart(mContext);
			gadget.setObserver(this);
			gadget.setResponseQueue(mResponseQueue);
			gadget.process();
			mRunningInstances.incrementAndGet();
		}
	}

	/**
	 * Locks the server by destroying all running gadgets with no keep-alive
	 * attribute and setting {@link #mLocked} to <code>true</code>.
	 */
	public void lock() {
		mLocked.set(true);
		for (String key : mGadgets.keySet()) {
			Gadget instance = mGadgets.get(key);
			if (!instance.isKeepAlive()) {
				synchronized (instance) {
					instance.onProcessEnd(mContext);
					instance.onDestroy(mContext);
				}
			}
		}
	}

	/**
	 * Unlocks the server by setting {@link #mLocked} to <code>false</code>.
	 */
	public void unlock() {
		mLocked.set(false);
	}

	/**
	 * Starts timeout task of {@link HttpServer} by sending {@link Intent} to
	 * {@link ServerService}.
	 */
	private void startServerTimeout() {
		String uri = "inspect://" + ServerService.CMD_START_TIMEOUT;
		Intent request = new Intent("de.inspector.intents");
		request.setData(Uri.parse(uri));
		mContext.startService(request);
	}

	/**
	 * Stops timeout task of {@link HttpServer} by sending {@link Intent} to
	 * {@link ServerService}.
	 */
	private void stopServerTimeout() {
		String uri = "inspect://" + ServerService.CMD_STOP_TIMEOUT;
		Intent request = new Intent("de.inspector.intents");
		request.setData(Uri.parse(uri));
		mContext.startService(request);
	}
}
