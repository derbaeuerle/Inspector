package de.hsrm.inspector.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
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

	private final BasicHttpProcessor httpproc;
	private final HttpParams httpParams;
	private final HttpRequestHandlerRegistry registry;
	private final ConnectionReuseStrategy reuseStrat;
	private final PatternHandler mHandler;
	private volatile Thread mWorker;

	private ConcurrentHashMap<String, Gadget> mConfiguration;
	private long mTimeout = 60;
	private Timer mTimeoutTimer;

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
		reuseStrat = new DefaultConnectionReuseStrategy();

		httpParams = new BasicHttpParams();
		httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60000)
				.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
				.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
				.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true);

		httpproc = new BasicHttpProcessor();
		httpproc.addInterceptor(new ResponseDate());
		httpproc.addInterceptor(new ResponseServer());
		httpproc.addInterceptor(new ResponseContent());
		httpproc.addInterceptor(new ResponseConnControl());

		registry = new HttpRequestHandlerRegistry();
		mHandler = new PatternHandler(context);
		registry.register(DEFAULT_PATTERN, mHandler);
	}

	/**
	 * Safe method to start server.
	 */
	public synchronized void startThread() {
		if (mSocket == null && !mSocket.isClosed()) {
			try {
				mSocket = new ServerSocket();
				mSocket.setReuseAddress(true);
				mSocket.setReceiveBufferSize(10);
				mSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), SERVER_PORT), SERVER_BACKLOG);
				mWorker = new Thread(new RequestWorker());
				mWorker.setDaemon(false);
				mWorker.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Safe method to stop server.
	 */
	public synchronized void stopThread() {
		if (mSocket == null)
			return;
		try {
			mSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			mSocket = null;
		}
		if (mWorker != null) {
			mWorker.interrupt();
		}
	}

	/**
	 * Returns true is {@link HttpServer} is started.
	 * 
	 * @return {@link Boolean}
	 */
	public synchronized boolean isRunning() {
		return mSocket != null && !mSocket.isClosed();
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

	/**
	 * The {@link RequestWorker} accepts multiple HTTP-Requests and encapsulate
	 * them into {@link Worker}.
	 */
	public class RequestWorker implements Runnable {

		private Set<Thread> workerThreads = Collections.synchronizedSet(new HashSet<Thread>());

		@Override
		public void run() {
			try {
				Log.d("Server", "Server now running!");
				while ((mSocket != null) && (mWorker == Thread.currentThread()) && !Thread.interrupted()) {
					try {
						accept();
					} catch (Exception e) {
						e.printStackTrace();
						break;
					}
				}
			} finally {
				cleanup();
			}
		}

		/**
		 * Accepting new HTTP-Connections, creating {@link Worker} threads and
		 * start them.
		 * 
		 * @throws IOException
		 */
		protected void accept() throws IOException {
			Socket socket = mSocket.accept();
			DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
			conn.bind(socket, httpParams);

			HttpService httpService = new HttpService(httpproc, reuseStrat, new DefaultHttpResponseFactory());
			httpService.setParams(httpParams);
			httpService.setHandlerResolver(registry);

			Thread t = new Thread(new Worker(httpService, conn));
			workerThreads.add(t);
			t.setDaemon(true);
			t.start();
			Log.d("Server", "Current worker: " + workerThreads.size());
		}

		/**
		 * Cleans running {@link Worker} inside {@link #workerThreads} to shut
		 * down the {@link HttpServer}.
		 */
		protected void cleanup() {
			Thread[] threads = workerThreads.toArray(new Thread[0]);
			for (int i = 0; i < threads.length; i++) {
				if (threads[i] != null)
					threads[i].interrupt();
			}
		}

		/**
		 * Worker thread to process the incoming HTTP-Requests.
		 */
		public class Worker implements Runnable {

			private final HttpService httpservice;
			private final HttpServerConnection conn;

			public Worker(final HttpService httpservice, final HttpServerConnection conn) {
				this.httpservice = httpservice;
				this.conn = conn;
			}

			@Override
			public void run() {
				HttpContext context = new BasicHttpContext(null);
				try {
					while ((mSocket != null) && this.conn.isOpen() && !Thread.interrupted()) {
						this.httpservice.handleRequest(this.conn, context);
					}
				} catch (IOException ex) {
				} catch (HttpException ex) {
				} finally {
					workerThreads.remove(Thread.currentThread());
					try {
						this.conn.shutdown();
					} catch (IOException ignore) {
					}
				}
			}

		}

	}
}
