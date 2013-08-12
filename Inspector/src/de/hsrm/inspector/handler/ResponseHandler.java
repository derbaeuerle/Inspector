package de.hsrm.inspector.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.hsrm.inspector.gadgets.pool.GadgetEvent;
import de.hsrm.inspector.gadgets.pool.ResponsePool;
import de.hsrm.inspector.gadgets.pool.SystemEvent;
import de.hsrm.inspector.handler.utils.InspectorRequest;
import de.hsrm.inspector.handler.utils.JsonConverter;
import de.hsrm.inspector.web.HttpServer;

/**
 * {@link HttpRequestHandler} for requests on port
 * {@value HttpServer#STATE_PORT} for state requests.
 */
public class ResponseHandler implements HttpRequestHandler {

	/** Current application {@link Context}. */
	private Context mContext;
	/** {@link ResponsePool} instance. */
	private ResponsePool mResponsePool;
	/** {@link InspectorRequest} of pool request. */
	private InspectorRequest mStateRequest;
	/** Instance of {@link Gson} to serialize response messages. */
	private Gson mGson = new Gson();

	/**
	 * Default constructor of {@link ResponseHandler}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param pool
	 *            {@link ResponsePool}
	 */
	public ResponseHandler(Context context, ResponsePool pool) {
		mContext = context;
		mResponsePool = pool;
	}

	/**
	 * Implementation of
	 * {@link HttpRequestHandler#handle(HttpRequest, HttpResponse, HttpContext)}
	 * to process state request of web-browser api. If {@link #mResponsePool}
	 * has events for given browser instance, they will be responded in
	 * {@link #response(String, InspectorRequest, HttpResponse)}. Else a
	 * {@link FutureTask} will be created with {@link QueueCallable} to wait a a
	 * little time for events.
	 * 
	 * @see org.apache.http.protocol.HttpRequestHandler#handle(org.apache.http.HttpRequest,
	 *      org.apache.http.HttpResponse, org.apache.http.protocol.HttpContext)
	 */
	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException,
			IOException {
		Object obj;

		try {
			mStateRequest = new InspectorRequest(request);

			// Check if there are responses in queue to send to client.
			if (mResponsePool.hasItems(mStateRequest.getBrowserId())) {
				obj = processResponses();
			} else {
				FutureTask<JsonArray> queueWaiter = new FutureTask<JsonArray>(new QueueCallable());
				queueWaiter.run();
				try {
					obj = queueWaiter.get();
				} catch (Exception e) {
					obj = JsonConverter.exceptionToJson(e, mContext);
				}
			}
			response(obj.toString(), mStateRequest, response);
		} catch (Exception e) {
			e.printStackTrace();
			obj = JsonConverter.exceptionToJson(e, mContext);
		}
	}

	/**
	 * Method to generate {@link HttpEntity} for {@link HttpResponse}.
	 * 
	 * @param content
	 *            {@link String}
	 * @param iRequest
	 *            {@link InspectorRequest}
	 * @param response
	 *            {@link HttpResponse}
	 */
	private void response(final String content, InspectorRequest iRequest, HttpResponse response) {
		final String jsonContent = iRequest.getCallback() + "(" + content + ");";
		response.setHeader("Content-Type", "application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods", "*");
		HttpEntity entity = new EntityTemplate(new ContentProducer() {
			public void writeTo(final OutputStream outputStream) throws IOException {
				OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
				writer.write(jsonContent);
				writer.flush();
			}
		});
		response.setEntity(entity);
	}

	/**
	 * Pops all {@link GadgetEvent} objects from {@link #mResponsePool} and
	 * processes them into {@link JsonObject} in a {@link JsonArray}.
	 * 
	 * @return {@link JsonArray}
	 */
	private JsonArray processResponses() {
		JsonArray response = new JsonArray();
		JsonObject tmp;
		for (GadgetEvent res : mResponsePool.popAll(mStateRequest.getBrowserId())) {
			try {
				tmp = new JsonObject();
				if (res instanceof SystemEvent) {
					tmp.addProperty("gadget", ((SystemEvent) res).getName());
					tmp.addProperty("event", ((SystemEvent) res).getResponse().toString());
				} else {
					tmp.addProperty("gadget", res.getGadget().getIdentifier());
					tmp.addProperty("event", res.getEvent().name());
				}
				tmp.add("data", mGson.toJsonTree(res.getResponse()));
				response.add(tmp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return response;
	}

	/**
	 * {@link FutureTask} {@link Callable} to wait until
	 * {@link ResponseHandler#mResponsePool} has items.
	 */
	private class QueueCallable implements Callable<JsonArray> {

		/** Maximal number of checks for events. */
		private final int MAX_CHECKS = 2;
		/** Number of checks already done. */
		private int mChecks = 0;

		/**
		 * Implementatino of {@link Callable#call()} to check
		 * {@link ResponseHandler#mResponsePool} for events for given browser
		 * instance. If {@link #mChecks} reaches {@link #MAX_CHECKS} the
		 * {@link ResponseHandler#processResponses()} will be called and return
		 * an empty {@link JsonArray}.
		 * 
		 * @throws Exception
		 */
		@Override
		public JsonArray call() throws Exception {
			do {
				if (mResponsePool.hasItems(mStateRequest.getBrowserId())) {
					break;
				}
				Thread.sleep(20);
				mChecks++;
			} while (mChecks < MAX_CHECKS);
			return processResponses();
		}
	}

}
