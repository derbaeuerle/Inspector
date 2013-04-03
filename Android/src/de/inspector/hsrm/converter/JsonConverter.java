package de.inspector.hsrm.converter;

import com.google.gson.Gson;

import de.inspector.hsrm.converter.intf.IResponseConverter;

public class JsonConverter implements IResponseConverter {

	private static final String MIME_TYPE = "application/json";

	private Gson mGson;

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
