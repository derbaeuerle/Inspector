package de.hsrm.inspector;

import de.hsrm.inspector.gadgets.communication.ResponsePool;
import android.app.Application;

public class ExtendedApplication extends Application {

	private ResponsePool mResponseQueue;

	public ResponsePool getOrCreateResponseQueue() {
		if (mResponseQueue == null) {
			mResponseQueue = new ResponsePool();
		}
		return mResponseQueue;
	}

}
