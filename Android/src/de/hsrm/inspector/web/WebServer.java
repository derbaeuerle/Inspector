package de.hsrm.inspector.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
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
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import de.hsrm.inspector.R;
import de.hsrm.inspector.web.defaults.DefaultHandler;
import de.hsrm.inspector.web.defaults.PatternHandler;

public class WebServer extends Thread {
	public static final String SERVER_NAME = "html5audio";
	public static final int SERVER_PORT = 9018;
	public static final int SERVER_BACKLOG = 50;

	private static final String DEFAULT_PATTERN = "*";

	private ServerSocket mSocket;

	private ServerAccessType mServerAccesType;

	private AtomicBoolean isRunning;
	private Context context = null;

	private BasicHttpProcessor httpproc = null;
	private BasicHttpContext httpContext = null;
	private HttpService httpService = null;
	private HttpRequestHandlerRegistry registry = null;
	private PatternHandler mHandler;

	private Context mContext;

	public WebServer(Context context) {
		super(SERVER_NAME);
		isRunning = new AtomicBoolean(false);
		mServerAccesType = ServerAccessType.LOCALHOST;
		mContext = context;

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

		registry.register(DEFAULT_PATTERN, mHandler);
		httpService.setHandlerResolver(registry);
	}

	@Override
	public void run() {
		super.run();
		try {
			SAXBuilder builder = new SAXBuilder();
			Element root = ((Document) builder.build(mContext.getResources().openRawResource(R.raw.inspector)))
					.getRootElement();
			List<Element> gadgets = root.getChildren(mContext.getString(R.string.configuration_gadgets));

			for (Element gadget : gadgets) {
				try {
					Class c = Class.forName(gadget.getChildText(mContext.getString(R.string.configuration_handler)));
					DefaultHandler h = (DefaultHandler) c.newInstance();
					h.setContext(mContext);

					String urlPattern = gadget.getChildText(mContext.getString(R.string.configuration_url_pattern))
							+ ".*";

					mHandler.registerHandler(urlPattern, h);
				} catch (ClassCastException e) {
					e.printStackTrace();
				}
			}

			if (mSocket == null) {
				switch (mServerAccesType) {
				case GLOBAL:
					mSocket = new ServerSocket(SERVER_PORT);
					break;
				case WIFI:
					mSocket = new ServerSocket(SERVER_PORT, SERVER_BACKLOG, getWifiInetAddress());
					break;
				default:
					mSocket = new ServerSocket(SERVER_PORT, SERVER_BACKLOG, InetAddress.getByName("localhost"));
					break;
				}
			}
			Log.d("SERVER", mSocket.getInetAddress().getHostAddress());
			mSocket.setReuseAddress(true);
			while (isRunning.get()) {
				try {
					final Socket socket = mSocket.accept();
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

	public synchronized void startThread() {
		Log.d("WebServer", "starting ...");
		if (!isRunning.get()) {
			isRunning.set(true);
			super.start();
		}
	}

	public synchronized void stopThread() {
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
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public Context getContext() {
		return context;
	}

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
}
