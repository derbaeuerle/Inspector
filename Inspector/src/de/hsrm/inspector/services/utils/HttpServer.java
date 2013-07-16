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
	private long mTimeout = 60;
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
							final DefaultHttpServerConnection serverConnection = new DefaultHttpServerConnection();
							serverConnection.bind(socket, new BasicHttpParams());
							(new Runnable() {

								@Override
								public void run() {
									try {
										httpService.handleRequest(serverConnection, httpContext);
									} catch (IOException e) {
										e.printStackTrace();
									} catch (HttpException e) {
										e.printStackTrace();
									}
								}
							}).run();
							serverConnection.shutdown();
						} catch (IOException e) {
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

	/**
	 * Returns true is {@link HttpServer} is started.
	 * 
	 * @return {@link Boolean}
	 */
	public synchronized boolean isRunning() {
		return isRunning.get();
	}

	/**
	 * Returns {@link #mConfiguration}.
	 * 
	 * @return {@link ConcurrentHashMap}
	 */
	public ConcurrentHashMap<String, Gadget> getConfiguration() {
		return mConfiguration;
	}

	/**
	 * Sets {@link #mConfiguration}.
	 * 
	 * @param map
	 *            {@link ConcurrentHashMap} to set.
	 */
	public void setConfiguration(ConcurrentHashMap<String, Gadget> map) {
		mConfiguration = map;
		mHandler.setGadgetConfiguration(mConfiguration);
	}

	/**
	 * Locks {@link #mHandler}.
	 */
	public void lock() {
		if (mHandler != null) {
			mHandler.lock();
		}
	}

	/**
	 * Unlocks {@link #mHandler}.
	 */
	public void unlock() {
		if (mHandler != null) {
			mHandler.unlock();
		}
	}

	/**
	 * Set {@link #mTimeout} for {@link #mTimeoutTimer}.
	 * 
	 * @param timeout
	 *            {@link Long} in milliseconds.
	 */
	public void setTimeoutTime(long timeout) {
		this.mTimeout = timeout;
	}

	/**
	 * Starts {@link #mTimeoutTimer}.
	 */
	public synchronized void startTimeout() {
		if (mTimeoutTimer != null) {
			mTimeoutTimer.cancel();
		}

		mTimeoutTimer = new Timer();
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				Log.e("SERVER", "Server timed out!");
				HttpServer.this.stopThread();
			}
		};
		mTimeoutTimer.schedule(task, mTimeout);
	}

	/**
	 * Stops {@link #mTimeoutTimer}.
	 */
	public synchronized void stopTimeout() {
		if (mTimeoutTimer != null) {
			mTimeoutTimer.cancel();
		}
	}
}
