package de.inspector.hsrm.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.google.gson.Gson;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import de.inspector.hsrm.gadgets.Gadget;
import de.inspector.hsrm.service.utils.AsyncServiceBinder;
import de.inspector.hsrm.service.utils.AsyncServiceBinderCallable;

public class PatternHandler implements HttpRequestHandler {

	private List<Gadget> mGadgetRegistry;
	private Context mContext;

	public PatternHandler(Context context) {
		mContext = context;
		mGadgetRegistry = new ArrayList<Gadget>();
	}

	public void registerHandler(Gadget gadget) {
		mGadgetRegistry.add(gadget);
	}

	public void registerHandler(String urlPattern, Gadget gadget) {
		mGadgetRegistry.add(gadget);
	}

	public List<Gadget> getRegistry() {
		return mGadgetRegistry;
	}

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException,
			IOException {
		response.setHeader("Content-Type", "application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods", "*");

		Log.d("REQUEST:", request.getRequestLine().toString());
		String uri = request.getRequestLine().getUri();
		Uri requestLine = Uri.parse(request.getRequestLine().getUri());

		final String callback = requestLine.getQueryParameter("callback");
		final Object responseContent;

		for (final Gadget gadget : mGadgetRegistry) {
			if (uri.matches(gadget.getPattern())) {
				Object tmpResponseContent = null;
				try {
					if (gadget.usesService()) {
						AsyncServiceBinderCallable callable = new AsyncServiceBinderCallable(gadget, request, context,
								requestLine);
						AsyncServiceBinder binder = new AsyncServiceBinder(callable, gadget, mContext);
						tmpResponseContent = binder.process();
					} else {
						tmpResponseContent = gadget.getConverter().convert(gadget.gogo(request, context, requestLine));
					}
				} catch (Exception e) {
					e.printStackTrace();

					Gson gson = new Gson();
					StringBuilder b = new StringBuilder();
					for (StackTraceElement s : e.getStackTrace()) {
						b.append(s.toString() + "\n");
					}
					HashMap<String, String> map = new HashMap<String, String>();
					map.put("message", e.getMessage());
					map.put("stacktrace", b.toString());
					tmpResponseContent = gson.toJson(map);
				}
				responseContent = tmpResponseContent;

				HttpEntity entity = new EntityTemplate(new ContentProducer() {
					public void writeTo(final OutputStream outstream) throws IOException {
						OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
						String resp = callback + "(" + responseContent.toString() + ")";
						Log.d("RESPONSE:", resp);
						writer.write(resp);
						writer.flush();
					}
				});
				response.setEntity(entity);
				break;
			}
		}
	}
}
