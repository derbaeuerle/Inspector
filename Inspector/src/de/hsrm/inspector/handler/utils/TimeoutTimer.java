package de.hsrm.inspector.handler.utils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import de.hsrm.inspector.services.intf.Gadget;

public class TimeoutTimer {

	private Gadget mGadget;
	private AtomicBoolean mStarted;
	private Timer mTimer;
	private Context mContext;

	public TimeoutTimer(Context context, Gadget gadget) {
		mGadget = gadget;
		mStarted = new AtomicBoolean(false);
		mContext = context;
	}

	public void start() {
		if (!mStarted.get()) {
			mTimer = new Timer();
			mTimer.schedule(new TimeoutTimerTask(), mGadget.getTimeout());
			mStarted.set(true);
		}
	}

	public void cancel() {
		if (mTimer != null) {
			mTimer.cancel();
		}
		mStarted.set(false);
	}

	private class TimeoutTimerTask extends TimerTask {

		@Override
		public void run() {
			TimeoutTimer.this.mGadget.onUnregister(TimeoutTimer.this.mContext);
			TimeoutTimer.this.mGadget.onDestroy(TimeoutTimer.this.mContext);
			TimeoutTimer.this.mStarted.set(false);
		}

	}

}
