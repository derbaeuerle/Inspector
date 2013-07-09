package de.hsrm.inspector.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import de.hsrm.inspector.R;
import de.hsrm.inspector.activities.SettingsActivity;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.services.utils.HttpServer;

public class HttpService extends IntentService {

	private static HttpServer mServer;

	private final String CMD_INIT = "init";
	private final String CMD_DESTROY = "destroy";
	private final String CMD_SETTINGS = "settings";
	private final String CMD_REFRESH = "refresh";
	private final String CMD_START_TIMEOUT = "start-timeout";
	private final String CMD_STOP_TIMEOUT = "stop-timeout";
	private final String CMD_LOCK = "lock";
	private final String CMD_UNLOCK = "unlock";

	public static final String EXTRA_PREFERENCES = "preferences";

	public HttpService() {
		super("HttpService");
		android.os.Debug.waitForDebugger();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		android.os.Debug.waitForDebugger();
		if (intent != null) {
			onHandleIntent(intent);
		}
		return START_STICKY;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
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
			Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			HashSet<String> prefs = new HashSet<String>();
			for (Gadget g : mServer.getConfiguration().values()) {
				if (g.getPreferences() != null) {
					prefs.add(g.getPreferences());
				}
			}
			String[] preferences = prefs.toArray(new String[0]);
			Arrays.sort(preferences);
			i.putExtra(EXTRA_PREFERENCES, preferences);

			startActivity(i);
		} else if (command.equals(CMD_REFRESH)) {
			init();
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
	}

	private void init() {
		if (mServer == null) {
			mServer = new HttpServer(getApplicationContext());
		}
		mServer.setConfiguration(configure());
	}

	private void start() {
		if (mServer != null) {
			mServer.startThread();
		}
	}

	private void stop() {
		if (mServer != null) {
			mServer.stopThread();
		}
	}

	private void lock() {
		if (mServer != null) {
			mServer.lock();
		}
	}

	private void unlock() {
		if (mServer != null) {
			mServer.unlock();
		}
	}

	private ConcurrentHashMap<String, Gadget> configure() {
		ConcurrentHashMap<String, Gadget> config = mServer.getConfiguration();
		if (config == null) {
			config = new ConcurrentHashMap<String, Gadget>();
			for (Gadget g : config.values()) {
				if (g.getPreferences() != null) {
					PreferenceManager.setDefaultValues(getApplicationContext(),
							getResources().getIdentifier(g.getPreferences(), "xml", getPackageName()), true);
				}
			}
		}

		readprefs(config);
		return config;
	}

	/**
	 * Creates runtime prefs based on inspector.xml and {@link Sharedprefs}.
	 * 
	 * @param config
	 * @return
	 */
	private void readprefs(ConcurrentHashMap<String, Gadget> config) {
		Context c = getApplicationContext();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		if (config.size() == 0) {
			readGadgets(config);
		}
		for (Gadget g : config.values()) {
			String id = g.getIdentifier().toLowerCase();
			g.setKeepAlive(prefs.getBoolean(id + ":" + c.getString(R.string.configuration_keep_alive), g.isKeepAlive()));
			g.setTimeout(Long.parseLong(prefs.getString(id + ":" + c.getString(R.string.configuration_timeout),
					"" + g.getTimeout())));
			g.setAuthType(Integer.parseInt(prefs.getString(id + ":" + c.getString(R.string.configuration_permission),
					c.getString(R.string.auth_type_granted))));
		}
	}

	/**
	 * Creates a gadget object from inspector.xml gadget-element.
	 * 
	 * @param gadget
	 * @param gadgets
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@SuppressWarnings("unchecked")
	private void createGadget(Element gadget, ConcurrentHashMap<String, Gadget> gadgets) throws ClassNotFoundException,
			IllegalAccessException, InstantiationException {
		try {
			Class<Gadget> gadgetClass = (Class<Gadget>) Class.forName(gadget.getChildText(getApplicationContext()
					.getString(R.string.configuration_class)));

			if (gadget.getChild(getApplicationContext().getString(R.string.configuration_identifiers)) != null) {
				Element identifiers = gadget.getChild(getApplicationContext().getString(
						R.string.configuration_identifiers));
				for (Element identifier : identifiers.getChildren(getApplicationContext().getString(
						R.string.configuration_identifier))) {
					Gadget g = (Gadget) gadgetClass.newInstance();
					g.setIdentifier(identifier.getText().toUpperCase());
					if (gadget.getChild(getString(R.string.configuration_preference_screen)) != null) {
						g.setPreferences(gadget.getChildText(getString(R.string.configuration_preference_screen)));
					}
					if (gadgets.contains(g.getIdentifier())) {
						gadgets.replace(g.getIdentifier(), g);
					} else {
						gadgets.put(g.getIdentifier(), g);
					}
				}
			} else {
				Gadget g = (Gadget) gadgetClass.newInstance();
				g.setIdentifier(gadget.getChildText(
						getApplicationContext().getString(R.string.configuration_identifier)).toUpperCase());
				if (gadget.getChild(getString(R.string.configuration_preference_screen)) != null) {
					g.setPreferences(gadget.getChildText(getString(R.string.configuration_preference_screen)));
				}
				if (gadgets.contains(g.getIdentifier())) {
					gadgets.replace(g.getIdentifier(), g);
				} else {
					gadgets.put(g.getIdentifier(), g);
				}
			}
		} catch (ClassCastException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads inspector.xml configuration for all supported gadget identifiers
	 * and their linked classes.
	 * 
	 * @param config
	 */
	private void readGadgets(ConcurrentHashMap<String, Gadget> config) {
		try {
			SAXBuilder builder = new SAXBuilder();
			InputStream file = getApplicationContext().getResources().openRawResource(R.raw.inspector_default);
			Element root = builder.build(file).getRootElement();
			List<Element> gadgets = root.getChildren(getApplicationContext().getString(R.string.configuration_gadgets));

			for (Element gadget : gadgets) {
				createGadget(gadget, config);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
	}
}
