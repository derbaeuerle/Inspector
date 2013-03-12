package de.hsrm.jcommunicator.web.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.net.Uri;
import android.util.Log;

public class MessageHandler implements HttpRequestHandler {

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException,
			IOException {
		Log.d("", "Incoming request: " + request.getRequestLine().getUri());
		response.setHeader("Content-Type", "application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods", "*");

		Uri requestLine = Uri.parse(request.getRequestLine().getUri());
		final String callback = requestLine.getQueryParameter("callback");

		HttpEntity entity = new EntityTemplate(new ContentProducer() {
			public void writeTo(final OutputStream outstream) throws IOException {
				OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
				String resp = callback + "({\n" + "\t\"key1\": \"value1\",\n" + "\t\"key2\": \"value2\"\n" + "})";

				writer.write(resp);
				writer.flush();
			}
		});
		response.setEntity(entity);

		HeaderIterator it = response.headerIterator();
		while (it.hasNext()) {
			Log.d("HEADER", it.next().toString());
		}
	}
}
