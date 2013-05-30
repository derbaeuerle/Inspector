package de.hsrm.inspector;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import android.content.Context;
import android.util.Log;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.handler.PatternHandler;

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
		Map<String, Gadget> defaultConfig = readConfiguration(mContext,
				context.getResources().openRawResource(R.raw.inspector_default));
		if (configuration != null) {
			defaultConfig.putAll(readConfiguration(mContext, mConfigurationFile));
		}
		mHandler.setGadgetConfiguration(defaultConfig);

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
		this.mContext = context;
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

	@SuppressWarnings("unchecked")
	private void createGadget(Context context, Element gadget, Map<String, Gadget> gadgets)
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		try {
			boolean keepAlive = Boolean.valueOf(gadget.getAttributeValue(
					context.getString(R.string.configuration_keep_alive), "false"));
			long timeout = Long.valueOf(gadget.getAttributeValue(context.getString(R.string.configuration_timeout))) * 1000;
			Class<Gadget> gadgetClass = (Class<Gadget>) Class.forName(gadget.getChildText(context
					.getString(R.string.configuration_class)));

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
