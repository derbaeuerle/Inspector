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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.hsrm.inspector.R;
import de.hsrm.inspector.constants.GadgetConstants;
import de.hsrm.inspector.exceptions.GadgetException;
import de.hsrm.inspector.exceptions.constants.GadgetExceptionConstants;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.gadgets.intf.GadgetObserver;
import de.hsrm.inspector.handler.utils.InspectorRequest;
import de.hsrm.inspector.services.ServerService;
import de.hsrm.inspector.services.utils.HttpServer;

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
	private AtomicBoolean mLocked = new AtomicBoolean(false);
	private Context mContext;
	private AtomicInteger mRunningInstances;
	private Gson mGson;

	/**
	 * Constructor of {@link PatternHandler} with current application
	 * {@link Context}.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public PatternHandler(Context context) {
		mContext = context;
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

		// Log.e("REQUEST", request.getRequestLine().toString());

		Object tmpResponseContent = "";
		try {
			InspectorRequest iRequest = new InspectorRequest(request);
			try {
				checkGadget(iRequest);
				tmpResponseContent = checkLocking(iRequest, request, response, context);
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
	 * Checking server for locking settings and decides what to do. If server is
	 * not locked or server is locked an gadget is keep-alive or running,
	 * request will be dispatched.
	 * 
	 * @param iRequest
	 *            {@link InspectorRequest}
	 * @param request
	 *            {@link HttpRequest}
	 * @param response
	 *            {@link HttpResponse}
	 * @param context
	 *            {@link HttpContext}
	 * @return {@link Object} value of
	 *         {@link #checkPermission(InspectorRequest, Gadget, HttpRequest, HttpResponse, HttpContext)}
	 *         call
	 * @throws Exception
	 *             Throws Exception on failure inside process or if request
	 *             couldn't be dispatched. Error code of {@link GadgetException}
	 *             will be {@link GadgetExceptionConstants#SERVER_IS_LOCKED}.
	 */
	private Object checkLocking(InspectorRequest iRequest, HttpRequest request, HttpResponse response,
			HttpContext context) throws Exception {
		Gadget gadget = mGadgets.get(iRequest.getGadgetIdentifier());
		if (!mLocked.get() || (mLocked.get() && gadget.isKeepAlive() && gadget.isRunning())) {
			return checkPermission(iRequest, gadget, request, response, context);
		}
		throw new GadgetException("Server is locked", GadgetExceptionConstants.SERVER_IS_LOCKED);
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
	 * @param request
	 *            {@link HttpRequest}
	 * @param response
	 *            {@link HttpResponse}
	 * @param context
	 *            {@link HttpContext}
	 * @return {@link Object} value of
	 *         {@link Gadget#gogo(Context, InspectorRequest, HttpRequest, HttpResponse, HttpContext)}
	 *         call
	 * @throws Exception
	 */
	private Object checkPermission(InspectorRequest iRequest, Gadget gadget, HttpRequest request,
			HttpResponse response, HttpContext context) throws Exception {
		if (iRequest.hasParameter("permission") && iRequest.getParameter("permission").toString().equals("true")) {
			mPermissions.get(gadget.getIdentifier()).add(iRequest.getReferer());
			Log.e("PERM", "Adding " + iRequest.getReferer() + " to " + gadget.getIdentifier());
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

			synchronized (gadget) {
				Object tmpResponseContent = gadget.gogo(mContext, iRequest);
				gadget.startTimeout();
				return tmpResponseContent;
			}
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
	 */
	private void checkGadget(InspectorRequest iRequest) throws GadgetException {
		if (!mGadgets.containsKey(iRequest.getGadgetIdentifier())) {
			throw new GadgetException("Gadget " + iRequest.getGadgetIdentifier() + " isn't available",
					GadgetExceptionConstants.GADGET_NOT_AVAILABLE);
		}
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
		JsonObject obj = new JsonObject();
		if (content instanceof Exception) {
			obj.add("error", exceptionToJson((Exception) content));
		} else {
			obj = mGson.toJsonTree(content).getAsJsonObject();
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
		Log.d("RESPONSE", jsonContent.toString());
		response.setEntity(entity);
	}

	/**
	 * Translates java exceptions into JSON strings.
	 * 
	 * @param e
	 *            {@link Exception}
	 * @return {@link JsonElement}
	 */
	private JsonElement exceptionToJson(Exception e) {
		StringBuilder b = new StringBuilder();
		for (StackTraceElement s : e.getStackTrace()) {
			b.append(s.toString() + "\n");
		}
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(mContext.getString(R.string.exception_message), e.getMessage());
		map.put(mContext.getString(R.string.exception_stacktrace), b.toString());
		if (e instanceof GadgetException) {
			map.put(mContext.getString(R.string.exception_error_code), ((GadgetException) e).getErrorCode() + "");
		}
		return mGson.toJsonTree(map);
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
	 * {@link Gadget#onRegister(Context)} will be called and this
	 * {@link PatternHandler} will be registered as {@link GadgetObserver}.
	 * 
	 * If Server is locked and a new instance shall be created, a
	 * {@link GadgetException} will be thrown with error code
	 * {@link GadgetExceptionConstants#SERVER_IS_LOCKED}.
	 * 
	 * @param gadget
	 *            {@link Gadget}
	 * @throws GadgetException
	 */
	public void initGadget(Gadget gadget) throws GadgetException {
		if (gadget.isRunning()) {
			gadget.cancelTimeout();
		} else {
			if (mLocked.get()) {
				throw new GadgetException("Unable to create a new gadget instance if server if locked!",
						GadgetExceptionConstants.SERVER_IS_LOCKED);
			}
			gadget.onCreate(mContext);
			gadget.onRegister(mContext);
			gadget.setObserver(this);
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
					instance.onUnregister(mContext);
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
