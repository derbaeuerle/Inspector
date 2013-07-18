package de.hsrm.inspector.handler.utils;

import java.util.HashMap;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.hsrm.inspector.R;
import de.hsrm.inspector.exceptions.GadgetException;

public class JsonConverter {

	private static final Gson mGson = new Gson();

	/**
	 * Translates java exceptions into JSON object.
	 * 
	 * @param e
	 *            {@link Exception}
	 * @return {@link JsonObject}
	 */
	public static JsonObject exceptionToJson(Exception e, Context context) {
		JsonObject errorObject = new JsonObject();
		errorObject.addProperty("event", "error");

		StringBuilder b = new StringBuilder();
		for (StackTraceElement s : e.getStackTrace()) {
			b.append(s.toString() + "\n");
		}
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(context.getString(R.string.exception_message), e.getMessage());
		map.put(context.getString(R.string.exception_stacktrace), b.toString());
		if (e instanceof GadgetException) {
			map.put(context.getString(R.string.exception_error_code), ((GadgetException) e).getErrorCode() + "");
		}
		errorObject.add("data", mGson.toJsonTree(map));
		return errorObject;
	}

}
