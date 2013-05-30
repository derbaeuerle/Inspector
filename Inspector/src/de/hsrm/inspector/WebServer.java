package de.hsrm.inspector;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import de.hsrm.inspector.R;
import de.hsrm.inspector.handler.PatternHandler;
import de.hsrm.inspector.services.intf.Gadget;

import org.apache.http.HttpException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.*;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebServer Thread to parse inspector's config file and start apache server
 * with pattern handler.
 * 
 * @author Dominic Baeuerle
 */
public class WebServer extends Thread {
	public static final String SERVER_NAME = "html5audio";
	public static final int SERVER_PORT = 9090;
	public static final int SERVER_BACKLOG = 50;
	private static final String DEFAULT_PATTERN = "*";
	private ServerSocket mSocket;
	private AtomicBoolean isRunning;
	private Context context = null;
	private BasicHttpProcessor httpproc = null;
	private BasicHttpContext httpContext = null;
	private HttpService httpService = null;
	private HttpRequestHandlerRegistry registry = null;
	private PatternHandler mHandler;
	private Context mContext;
	private InputStream mConfigurationFile;

	/**
	 * Constructor for web server.
	 * 
	 * @param context
	 *            Android application context.
	 * @param configuration
	 *            InputStream for configuration file.
	 */
	public WebServer(Context context, InputStream configuration) {
		super(SERVER_NAME);
		isRunning = new AtomicBoolean(false);
		mContext = context;
		mConfigurationFile = configuration;

		this.setContext(context);

		httpproc = new BasicHttpProcessor();
		httpContext = new BasicHttpContext();

		httpproc.addInterceptor(new ResponseDate());
		httpproc.addInterceptor(new ResponseServer());
		httpproc.addInterceptor(new ResponseContent());
		httpproc.addInterceptor(new ResponseConnControl());

		httpService = new HttpService(httpproc, new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory());

		registry = new HttpRequestHandlerRegistry();
		mHandler = new PatternHandler(context);
		mHandler.setGadgetConfiguration(readConfiguration(mContext, mConfigurationFile));

		registry.register(DEFAULT_PATTERN, mHandler);
		httpService.setHandlerResolver(registry);
	}

	@Override
	public void run() {
		super.run();
		try {
			mSocket = new ServerSocket(SERVER_PORT, SERVER_BACKLOG, InetAddress.getByName("localhost"));

			Log.d("SERVER", mSocket.getInetAddress().getHostAddress());
			try {
				mSocket.setReuseAddress(true);
				mSocket.setReceiveBufferSize(10);
				while (isRunning.get()) {
					try {
						final Socket socket = mSocket.accept();
						socket.setReuseAddress(true);
						DefaultHttpServerConnection serverConnection = new DefaultHttpServerConnection();
						serverConnection.bind(socket, new BasicHttpParams());
						httpService.handleRequest(serverConnection, httpContext);
						serverConnection.shutdown();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (HttpException e) {
						e.printStackTrace();
					}
				}
				mSocket.close();
				mSocket = null;
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Safe method to start server.
	 */
	public void startThread() {
		Log.d("WebServer", "starting ...");
		if (!isRunning.get()) {
			isRunning.set(true);
			super.start();
		}
	}

	/**
	 * Safe method to stop server.
	 */
	public void stopThread() {
		Log.d("WebServer", "stopping ...");
		if (isRunning.get()) {
			isRunning.set(false);
		}
		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		super.interrupt();
	}

	/**
	 * Setting android application context.
	 * 
	 * @param context
	 *            Android application context.
	 */
	public void setContext(Context context) {
		this.context = context;
	}

	/**
	 * Get wifi address for wifi access.
	 * 
	 * @return InetAddress WiFi address.
	 */
	private InetAddress getWifiInetAddress() {
		WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager.isWifiEnabled()) {
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int ipAddress = wifiInfo.getIpAddress();
			String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
					(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
			try {
				return InetAddress.getByName(ip);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private Map<String, Gadget> readConfiguration(Context context, InputStream configurationFile) {
		try {
			Map<String, Gadget> config = new HashMap<String, Gadget>();
			SAXBuilder builder = new SAXBuilder();
			Element root = builder.build(configurationFile).getRootElement();
			List<Element> gadgets = root.getChildren(context.getString(R.string.configuration_gadgets));

			for (Element gadget : gadgets) {
				createGadget(context, gadget, config);
			}
			return config;
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
		return null;
	}

	private void createGadget(Context context, Element gadget, Map<String, Gadget> gadgets)
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		try {
			boolean keepAlive = Boolean.valueOf(gadget.getAttributeValue(
					context.getString(R.string.configuration_keep_alive), "false"));
			int timeout = Integer.valueOf(gadget.getAttributeValue(context.getString(R.string.configuration_timeout)));
			Class gadgetClass = Class.forName(gadget.getChildText(context.getString(R.string.configuration_class)));

			if (gadget.getChild(context.getString(R.string.configuration_identifiers)) != null) {
				Element identifiers = gadget.getChild(context.getString(R.string.configuration_identifiers));
				for (Element identifier : identifiers.getChildren(context.getString(R.string.configuration_identifier))) {
					Gadget g = (Gadget) gadgetClass.newInstance();
					g.setIdentifier(identifier.getText().toUpperCase());
					g.setKeepAlive(keepAlive);
					g.setTimeout(timeout);
					g.setClass(gadgetClass);
					gadgets.put(g.getIdentifier(), g);
				}
			} else {
				Gadget g = (Gadget) gadgetClass.newInstance();
				g.setIdentifier(gadget.getChildText(context.getString(R.string.configuration_identifier)).toUpperCase());
				g.setKeepAlive(keepAlive);
				g.setTimeout(timeout);
				g.setClass(gadgetClass);
				gadgets.put(g.getIdentifier(), g);
			}
		} catch (ClassCastException e) {
			e.printStackTrace();
		}
	}
}
