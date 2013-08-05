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

	/**
	 * Constructor of {@link TimeoutTimer}.
	 * 
	 * @param context
	 *            Current application {@link Context}.
	 * @param gadget
	 *            {@link Gadget} object to time out.
	 */
	public TimeoutTimer(Gadget gadget) {
		mGadget = gadget;
		mStarted = new AtomicBoolean(false);
	}

	/**
	 * Starts the {@link #mTimer} {@link Timer}.
	 */
	public void start() {
		if (!mStarted.get() && mGadget.getTimeout() > 0) {
			mTimer = new Timer();
			mTimer.schedule(new TimeoutTimerTask(), mGadget.getTimeout() * 1000);
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
			try {
				TimeoutTimer.this.mGadget.onProcessEnd();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				TimeoutTimer.this.mGadget.onDestroy();
			} catch (Exception e) {
				e.printStackTrace();
			}
			TimeoutTimer.this.mStarted.set(false);
		}
	}

}
