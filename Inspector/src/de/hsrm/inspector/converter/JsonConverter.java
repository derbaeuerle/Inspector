package de.hsrm.inspector.converter;

import com.google.gson.Gson;

import de.hsrm.inspector.converter.intf.IResponseConverter;

/**
 * Default {@link IResponseConverter} to convert response data into JSON object.
 * 
 * @author Dominic Baeuerle
 * 
 */
public class JsonConverter implements IResponseConverter {

	/** Static mime type string. */
	private static final String MIME_TYPE = "application/json";

	private Gson mGson;

	/**
	 * Default constructor for converter.
	 */
	public JsonConverter() {
		mGson = new Gson();
	}

	@Override
	public Object convert(Object data) {
		return mGson.toJson(data);
	}

	@Override
	public String getMimeType() {
		return MIME_TYPE;
	}

}
