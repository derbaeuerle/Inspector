package de.hsrm.inspector.handler.utils;

import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpRequest;

import android.net.Uri;
import android.net.UrlQuerySanitizer;

/**
 * Created by dobae on 28.05.13.
 */
public class InspectorRequest {

	private UrlQuerySanitizer mQuery;
	private List<String> mSegments;
	private String mCallback;

	/**
	 * @param requestLine
	 *            Something like "http://localhost:80/<identifier>/<parameters>
	 * @throws Exception
	 */
	public InspectorRequest(HttpRequest request) throws Exception {
		Uri requestLine = Uri.parse(request.getRequestLine().getUri());
		mQuery = new UrlQuerySanitizer(requestLine.toString());
		if (mQuery.hasParameter("callback")) {
			mCallback = mQuery.getValue("callback");
		}
		String segs = requestLine.toString().substring(0, requestLine.toString().indexOf("?") - 1);
		mSegments = Uri.parse(segs).getPathSegments();
		mSegments.removeAll(Arrays.asList("", null));

		if (mSegments.size() < 1)
			throw new Exception("");
	}

	public boolean hasParameter(String key) {
		return mQuery.getParameterSet().contains(key);
	}

	public List<UrlQuerySanitizer.ParameterValuePair> getParameters() {
		return mQuery.getParameterList();
	}

	public Object getParameter(String key) {
		return mQuery.getValue(key);
	}

	public String getGadgetIdentifier() {
		return mSegments.get(1).toUpperCase();
	}

	public List<String> getSegments() {
		return mSegments.subList(1, mSegments.size());
	}

	public String getCallback() {
		return mCallback;
	}

}
