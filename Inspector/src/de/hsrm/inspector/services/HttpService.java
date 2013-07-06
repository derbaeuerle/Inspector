package de.hsrm.inspector.services;

import java.util.concurrent.ConcurrentHashMap;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.util.Log;
import de.hsrm.inspector.R;
import de.hsrm.inspector.activities.SettingsActivity;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.services.utils.HttpServer;
import de.hsrm.inspector.services.utils.ServiceBinder;

public class HttpService extends Service {

	private static HttpServer mServer;
	private ServiceBinder mBinder;

	private final String CMD_INIT = "init";
	private final String CMD_DESTROY = "destroy";
	private final String CMD_SETTINGS = "settings";
	private final String CMD_START_TIMEOUT = "start-timeout";
	private final String CMD_STOP_TIMEOUT = "stop-timeout";
	private final String CMD_LOCK = "lock";
	private final String CMD_UNLOCK = "unlock";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		android.os.Debug.waitForDebugger();
		Log.e("", "Intent received: " + intent.toURI());
		String command = Uri.parse(intent.toURI()).getHost();
		if (command.equals(CMD_INIT)) {
			init();
			start();
		} else if (command.equals(CMD_DESTROY)) {
			stop();
		} else if (command.equals(CMD_SETTINGS)) {
			init();
			mServer.setConfiguration(checkSharedPreferences());
			Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
		} else if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
			lock();
		} else if (intent.getAction().equals("android.intent.action.SCREEN_ON")) {
			unlock();
		} else if (command.equals(CMD_START_TIMEOUT)) {
			if (mServer != null) {
				mServer.startTimeout();
			}
		} else if (command.equals(CMD_STOP_TIMEOUT)) {
			if (mServer != null) {
				mServer.stopTimeout();
			}
		} else if (command.equals(CMD_LOCK)) {
			lock();
		} else if (command.equals(CMD_UNLOCK)) {
			unlock();
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public ServiceBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mBinder = new ServiceBinder() {

			@Override
			public Service getService() {
				return HttpService.this;
			}
		};
	}

	public void init() {
		if (mServer == null) {
			mServer = new HttpServer(getApplicationContext());
		}
	}

	public void start() {
		if (mServer != null) {
			mServer.startThread();
		}
	}

	public void stop() {
		if (mServer != null) {
			mServer.stopThread();
		}
	}

	public void lock() {
		if (mServer != null) {
			mServer.lock();
		}
	}

	public void unlock() {
		if (mServer != null) {
			mServer.unlock();
		}
	}

	public HttpServer getServer() {
		return mServer;
	}

	private ConcurrentHashMap<String, Gadget> checkSharedPreferences() {
		Context context = getApplicationContext();
		ConcurrentHashMap<String, Gadget> config = mServer.getConfiguration();
		SharedPreferences preferences = context.getSharedPreferences(
				context.getString(R.string.configuration_preferences), Context.MODE_PRIVATE);
		synchronized (config) {
			for (Gadget g : config.values()) {
				if (!preferences.contains(g.getIdentifier() + ":"
						+ context.getString(R.string.configuration_initialized))) {
					// Preferences for gadget not initialized.
					Editor e = preferences.edit();
					e.putBoolean(g.getIdentifier() + ":" + context.getString(R.string.configuration_keep_alive),
							g.isKeepAlive());
					e.putBoolean(g.getIdentifier() + ":" + context.getString(R.string.configuration_needs_auth),
							g.needsAuth());
					e.putLong(g.getIdentifier() + ":" + context.getString(R.string.configuration_timeout),
							g.getTimeout());
					e.putBoolean(g.getIdentifier() + ":" + context.getString(R.string.configuration_initialized), true);
					e.commit();
				} else {
					g.setKeepAlive(preferences.getBoolean(
							g.getIdentifier() + ":" + context.getString(R.string.configuration_keep_alive),
							g.isKeepAlive()));
					g.setTimeout(preferences.getLong(
							g.getIdentifier() + ":" + context.getString(R.string.configuration_timeout), g.getTimeout()));
				}
			}
		}
		return config;
	}

}
