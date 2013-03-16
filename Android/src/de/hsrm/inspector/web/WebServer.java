package de.hsrm.inspector.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
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

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import de.hsrm.inspector.web.handler.ExceptionHandler;
import de.hsrm.inspector.web.handler.MessageHandler;
import de.hsrm.inspector.web.handler.SystemReleaseHandler;
import de.hsrm.inspector.web.handler.SystemTimeHandler;

public class WebServer extends Thread {
	public static final String SERVER_NAME = "html5audio";
	public static final int SERVER_PORT = 9018;
	public static final int SERVER_BACKLOG = 50;

	private static final String TIME_PATTERN = "/time*";
	private static final String RELEASE_PATTERN = "/release*";
	private static final String EXCEPTION_PATTERN = "/exception*";
	private static final String DEFAULT_PATTERN = "*";

	private ServerSocket mSocket;

	private ServerAccessType mServerAccesType;

	private AtomicBoolean isRunning;
	private Context context = null;

	private BasicHttpProcessor httpproc = null;
	private BasicHttpContext httpContext = null;
	private HttpService httpService = null;
	private HttpRequestHandlerRegistry registry = null;

	private Context mContext;

	public WebServer(Context context) {
		super(SERVER_NAME);
		isRunning = new AtomicBoolean(false);
		mServerAccesType = ServerAccessType.GLOBAL;
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

		registry.register(TIME_PATTERN, new SystemTimeHandler(context));
		registry.register(RELEASE_PATTERN, new SystemReleaseHandler(context));
		registry.register(EXCEPTION_PATTERN, new ExceptionHandler());
		registry.register(DEFAULT_PATTERN, new MessageHandler());
		httpService.setHandlerResolver(registry);
	}

	@Override
	public void run() {
		super.run();
		try {
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
