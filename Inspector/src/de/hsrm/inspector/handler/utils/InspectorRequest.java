package de.hsrm.inspector.handler.utils;

import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpRequest;

import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.services.utils.HttpServer;

import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.net.UrlQuerySanitizer.ParameterValuePair;

/**
 * Utility object for {@link HttpRequest} of {@link HttpServer}.
 */
public class InspectorRequest {

	private static final String REFERER_KEY = "Referer";

	private UrlQuerySanitizer mQuery;
	private List<String> mSegments;
	private String mCallback;
	private String mReferer;

	/**
	 * Constructor of {@link InspectorRequest}.
	 * 
	 * @param requestLine
	 *            Something like "http://localhost:80/<identifier>/<parameters>"
	 * @throws Exception
	 */
	public InspectorRequest(HttpRequest request) throws Exception {
		Uri requestLine = Uri.parse(request.getRequestLine().getUri());
		mQuery = new UrlQuerySanitizer(requestLine.toString());
		if (mQuery.hasParameter("callback")) {
			mCallback = mQuery.getValue("callback");
		}

		mReferer = request.getFirstHeader(REFERER_KEY).toString();

		String segs = requestLine.toString().substring(0, requestLine.toString().indexOf("?") - 1);
		mSegments = Uri.parse(segs).getPathSegments();
		mSegments.removeAll(Arrays.asList("", null));

		if (mSegments.size() < 1)
			throw new Exception("");
	}

	/**
	 * Checks if {@link HttpRequest} has a parameter with given name.
	 * 
	 * @param key
	 *            {@link String} name of parameter.
	 * @return {@link Boolean}
	 */
	public boolean hasParameter(String key) {
		return mQuery.getParameterSet().contains(key);
	}

	/**
	 * Returns all URL parameters of {@link HttpRequest} as {@link List} of
	 * {@link ParameterValuePair}.
	 * 
	 * @return {@link List} of {@link ParameterValuePair}
	 */
	public List<UrlQuerySanitizer.ParameterValuePair> getParameters() {
		return mQuery.getParameterList();
	}

	/**
	 * Returns value of URL parameter from given {@link String} key as
	 * {@link Object}.
	 * 
	 * @param key
	 *            {@link String} key of URL parameter name.
	 * @return {@link Object}
	 */
	public Object getParameter(String key) {
		return mQuery.getValue(key);
	}

	/**
	 * Returns {@link Gadget} identifier as {@link String} parsed from
	 * {@link HttpRequest}.
	 * 
	 * @return {@link String}
	 */
	public String getGadgetIdentifier() {
		return mSegments.get(1).toUpperCase();
	}

	/**
	 * Returns all segments of URL.
	 * 
	 * @return {@link List} of {@link String}
	 */
	public List<String> getSegments() {
		return mSegments.subList(1, mSegments.size());
	}

	/**
	 * Returns name of JSONP callback function parsed from {@link HttpRequest}.
	 * 
	 * @return {@link String}
	 */
	public String getCallback() {
		return mCallback;
	}

	/**
	 * Returns referer of {@link HttpRequest}.
	 * 
	 * @return {@link String}
	 */
	public String getReferer() {
		return mReferer;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(mSegments.toString() + "\n");
		b.append(mReferer + "\n");
		for (ParameterValuePair pair : mQuery.getParameterList()) {
			b.append(pair.mParameter + ": " + pair.mValue + "\n");
		}
		return b.toString();
	}

}
