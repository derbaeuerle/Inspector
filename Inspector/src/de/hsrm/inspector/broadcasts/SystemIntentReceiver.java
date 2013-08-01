package de.hsrm.inspector.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import de.hsrm.inspector.gadgets.pool.GadgetEvent.EVENT_TYPE;
import de.hsrm.inspector.gadgets.pool.ResponsePool;
import de.hsrm.inspector.gadgets.pool.SystemEvent;

/**
 * @author dobae
 * 
 */
public class SystemIntentReceiver extends BroadcastReceiver {

	private ResponsePool mResponsePool;

	public SystemIntentReceiver(ResponsePool responsePool) {
		super();
		mResponsePool = responsePool;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction() != null) {
			String action = intent.getAction().substring(intent.getAction().lastIndexOf(".") + 1);
			mResponsePool.add(new SystemEvent(action, EVENT_TYPE.STATE));
		}
	}

}
