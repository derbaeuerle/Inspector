package de.hsrm.inspector.gadgets.communication;

import android.content.Context;

import de.hsrm.inspector.handler.utils.InspectorRequest;

public class GadgetRequest {

	private InspectorRequest mRequest;
	private Context mContext;

	public GadgetRequest(InspectorRequest iRequest, Context context) {
		mRequest = iRequest;
		mContext = context;
	}

	public InspectorRequest getRequest() {
		return mRequest;
	}

	public Context getContext() {
		return mContext;
	}

}
