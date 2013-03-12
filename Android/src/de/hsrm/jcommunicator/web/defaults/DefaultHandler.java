package de.hsrm.jcommunicator.web.defaults;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

public abstract class DefaultHandler implements HttpRequestHandler {

	protected Context mContext;

	public DefaultHandler() {
		this(null);
	}

	public DefaultHandler(Context context) {
		mContext = context;
	}

	public abstract JSONObject handleRequest(HttpRequest request, HttpContext context, Uri requestLine)
			throws Exception;

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
				final JSONObject returnJSON = handleRequest(request, context, requestLine);

				HttpEntity entity = new EntityTemplate(new ContentProducer() {
					public void writeTo(final OutputStream outstream) throws IOException {
						OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
						String resp = callback + "(" + returnJSON.toString() + ")";
						writer.write(resp);
						writer.flush();
					}
				});
				response.setEntity(entity);
			} catch (Exception ex) {
			}
		} catch (NullPointerException e) {
			throw e;
		}
	}
}
