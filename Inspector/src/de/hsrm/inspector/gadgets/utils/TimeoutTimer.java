package de.hsrm.inspector.gadgets.utils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import de.hsrm.inspector.gadgets.intf.Gadget;

/**
 * {@link Timer} with {@link TimerTask} to support {@link Gadget} timeout.
 */
public class TimeoutTimer {

	private Gadget mGadget;
	private AtomicBoolean mStarted;
	private Timer mTimer;
	private Context mContext;

	/**
	 * Constructor of {@link TimeoutTimer}.
	 * 
	 * @param context
	 *            Current application {@link Context}.
	 * @param gadget
	 *            {@link Gadget} object to time out.
	 */
	public TimeoutTimer(Context context, Gadget gadget) {
		mGadget = gadget;
		mStarted = new AtomicBoolean(false);
		mContext = context;
	}

	/**
	 * Starts the {@link #mTimer} {@link Timer}.
	 */
	public void start() {
		if (!mStarted.get()) {
			mTimer = new Timer();
			mTimer.schedule(new TimeoutTimerTask(), mGadget.getTimeout());
			mStarted.set(true);
		}
	}

	/**
	 * Cancels current {@link #mTimer}.
	 */
	public void cancel() {
		if (mTimer != null) {
			mTimer.cancel();
		}
		mStarted.set(false);
	}

	/**
	 * {@link TimerTask} to unregister and destroy {@link TimeoutTimer#mGadget}.
	 */
	private class TimeoutTimerTask extends TimerTask {

		@Override
		public void run() {
			TimeoutTimer.this.mGadget.onUnregister(TimeoutTimer.this.mContext);
			TimeoutTimer.this.mGadget.onDestroy(TimeoutTimer.this.mContext);
			TimeoutTimer.this.mStarted.set(false);
		}

	}

}
