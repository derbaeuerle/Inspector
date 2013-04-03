package de.hsrm.inspector.web.defaults;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

public abstract class DefaultHandler implements HttpRequestHandler {

	protected Context mContext;
	protected Gson mGson;

	public DefaultHandler() {
		this(null);
	}

	public DefaultHandler(Context context) {
		mContext = context;
		mGson = new Gson();
	}

	public void setContext(Context context) {
		mContext = context;
	}

	public abstract Object gogo(HttpRequest request, HttpContext context, Uri requestLine) throws Exception;

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException,
			IOException, NullPointerException {
		Log.d("", "Incoming request: " + request.getRequestLine().getUri());
		response.setHeader("Content-Type", "application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods", "*");

		Uri requestLine = Uri.parse(request.getRequestLine().getUri());

		try {
			final String callback = requestLine.getQueryParameter("callback");
			try {
				String jsonReturn = null;
				try {
					jsonReturn = mGson.toJson(gogo(request, context, requestLine));
				} catch (Exception e) {
					e.printStackTrace();
					StringBuilder b = new StringBuilder();
					for (StackTraceElement s : e.getStackTrace()) {
						b.append(s.toString() + "\n");
					}
					HashMap<String, String> map = new HashMap<String, String>();
					map.put("message", e.getMessage());
					map.put("stacktrace", b.toString());
					jsonReturn = mGson.toJson(map);
				}
				final String json = jsonReturn;

				HttpEntity entity = new EntityTemplate(new ContentProducer() {
					public void writeTo(final OutputStream outstream) throws IOException {
						OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
						String resp = callback + "(" + json + ")";
						Log.d("RESPONSE:", resp);
						writer.write(resp);
						writer.flush();
					}
				});
				response.setEntity(entity);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} catch (NullPointerException e) {
			throw e;
		}
	}
}
