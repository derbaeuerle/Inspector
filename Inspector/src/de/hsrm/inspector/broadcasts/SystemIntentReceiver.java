package de.hsrm.inspector.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import de.hsrm.inspector.gadgets.pool.GadgetEvent.EVENT_TYPE;
import de.hsrm.inspector.gadgets.pool.ResponsePool;
import de.hsrm.inspector.gadgets.pool.SystemEvent;

/**
 * {@link BroadcastReceiver} to receive system {@link Intent} such as
 * {@link Intent#ACTION_SCREEN_OFF} and {@link Intent#ACTION_SCREEN_ON}.
 */
public class SystemIntentReceiver extends BroadcastReceiver {

	/** Instance of {@link ResponsePool}. */
	private ResponsePool mResponsePool;

	/**
	 * Constructor of {@link SystemIntentReceiver}. Sets {@link #mResponsePool}
	 * to given {@link ResponsePool} instance.
	 * 
	 * @param responsePool
	 *            {@link ResponsePool}
	 */
	public SystemIntentReceiver(ResponsePool responsePool) {
		super();
		mResponsePool = responsePool;
	}

	/**
	 * Implementation of {@link BroadcastReceiver#onReceive(Context, Intent)}.
	 * Parses action of given {@link Intent} and adds a new {@link SystemEvent}
	 * to {@link #mResponsePool}.
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 *      android.content.Intent)
	 * 
	 * @param context
	 *            {@link Context}
	 * @param intent
	 *            {@link Intent}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction() != null) {
			String action = intent.getAction().substring(intent.getAction().lastIndexOf(".") + 1);
			mResponsePool.add(new SystemEvent(action, EVENT_TYPE.STATE));
		}
	}

}
