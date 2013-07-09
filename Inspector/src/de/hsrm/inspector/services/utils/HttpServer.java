package de.hsrm.inspector.services.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
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
import android.util.Log;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.handler.PatternHandler;

/**
 * WebServer Thread to parse inspector's config file and start apache server
 * with pattern handler.
 * 
 * @author Dominic Baeuerle
 */
public class HttpServer extends Thread {

	public static final String SERVER_NAME = "html5audio";
	public static final int SERVER_PORT = 9090;
	public static final int SERVER_BACKLOG = 50;
	private static final String DEFAULT_PATTERN = "/inspector/*";
	private ServerSocket mSocket;
	private AtomicBoolean isRunning, isStarted;
	private BasicHttpProcessor httpproc = null;
	private BasicHttpContext httpContext = null;
	private HttpService httpService = null;
	private HttpRequestHandlerRegistry registry = null;
	private PatternHandler mHandler;
	private ConcurrentHashMap<String, Gadget> mConfiguration;
	private long mTimeout = 0;
	private Timer mTimeoutTimer;

	// private InputStream mConfigurationFile;

	/**
	 * Constructor for web server.
	 * 
	 * @param context
	 *            Android application context.
	 * @param configuration
	 *            InputStream for configuration file.
	 */
	public HttpServer(Context context) {
		super(SERVER_NAME);
		isRunning = new AtomicBoolean(false);
		isStarted = new AtomicBoolean(false);
		// mConfigurationFile = configuration;

		httpproc = new BasicHttpProcessor();
		httpContext = new BasicHttpContext();

		httpproc.addInterceptor(new ResponseDate());
		httpproc.addInterceptor(new ResponseServer());
		httpproc.addInterceptor(new ResponseContent());
		httpproc.addInterceptor(new ResponseConnControl());

		httpService = new HttpService(httpproc, new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory());

		registry = new HttpRequestHandlerRegistry();
		// mConfiguration = readConfiguration(mContext,
		// context.getResources().openRawResource(R.raw.inspector_default));
		// if (configuration != null) {
		// defaultConfig.putAll(readConfiguration(mContext,
		// mConfigurationFile));
		// }
		mHandler = new PatternHandler(context);
		// mHandler.setGadgetConfiguration(mConfiguration);

		registry.register(DEFAULT_PATTERN, mHandler);
		httpService.setHandlerResolver(registry);
	}

	@Override
	public void run() {
		try {
			while (isStarted.get()) {
				if (isRunning.get()) {
					mSocket = new ServerSocket();
					mSocket.setReuseAddress(true);
					mSocket.setReceiveBufferSize(10);
					mSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), SERVER_PORT), SERVER_BACKLOG);
					Log.d("SERVER", mSocket.getInetAddress().getHostAddress());
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
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Safe method to start server.
	 */
	public synchronized void startThread() {
		if (!isRunning.get()) {
			isRunning.set(true);
			try {
				if (!isStarted.get()) {
					isStarted.set(true);
					super.start();
				}
			} catch (IllegalThreadStateException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Safe method to stop server.
	 */
	public synchronized void stopThread() {
		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (isRunning.get()) {
			isRunning.set(false);
		}
	}

	public synchronized boolean isRunning() {
		return isRunning.get();
	}

	public ConcurrentHashMap<String, Gadget> getConfiguration() {
		return mConfiguration;
	}

	public void setConfiguration(ConcurrentHashMap<String, Gadget> map) {
		mConfiguration = map;
		mHandler.setGadgetConfiguration(mConfiguration);
	}

	public void lock() {
		if (mHandler != null) {
			mHandler.lock();
		}
	}

	public void unlock() {
		if (mHandler != null) {
			mHandler.unlock();
		}
	}

	public synchronized void startTimeout() {
		if (mTimeoutTimer != null) {
			mTimeoutTimer.cancel();
		}

		mTimeoutTimer = new Timer();
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				HttpServer.this.stopThread();
			}
		};
		mTimeoutTimer.schedule(task, mTimeout);
	}

	public synchronized void stopTimeout() {
		if (mTimeoutTimer != null) {
			mTimeoutTimer.cancel();
		}
	}
}
