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

import android.app.Application;
import android.util.Log;
import de.hsrm.inspector.ExtendedApplication;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.handler.PatternHandler;
import de.hsrm.inspector.handler.StateHandler;

/**
 * WebServer Thread to parse inspector's config file and start apache server
 * with pattern handler.
 */
public class HttpServer extends Thread {

	public static final String SERVER_NAME = "html5audio";
	public static final int SERVER_PORT = 9090;
	public static final int STATE_PORT = 9191;
	public static final int SERVER_BACKLOG = 50;
	private static final String DEFAULT_PATTERN = "/inspector/*";
	private static final String STATE_PATTERN = "/state/*";
	private ServerSocket mCommandSocket, mStateSocket;

	private final BasicHttpProcessor mHttpProcessor;
	private final HttpParams mHttpParams;
	private final HttpRequestHandlerRegistry mHttpRegistry;
	private final ConnectionReuseStrategy mReuseStrat;
	private PatternHandler mPatternHandler;
	private StateHandler mStateHandler;
	private volatile Thread mCommandWorker, mStateWorker;

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
	public HttpServer(Application app) {
		super(SERVER_NAME);
		mReuseStrat = new DefaultConnectionReuseStrategy();

		mHttpParams = new BasicHttpParams();
		mHttpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60000)
				.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
				.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
				.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true);

		mHttpProcessor = new BasicHttpProcessor();
		mHttpProcessor.addInterceptor(new ResponseDate());
		mHttpProcessor.addInterceptor(new ResponseServer());
		mHttpProcessor.addInterceptor(new ResponseContent());
		mHttpProcessor.addInterceptor(new ResponseConnControl());

		mHttpRegistry = new HttpRequestHandlerRegistry();
		mPatternHandler = new PatternHandler((ExtendedApplication) app);
		mHttpRegistry.register(DEFAULT_PATTERN, mPatternHandler);
		try {
			mStateHandler = new StateHandler((ExtendedApplication) app);
			mHttpRegistry.register(STATE_PATTERN, mStateHandler);
		} catch (ClassCastException e) {
		}
	}

	/**
	 * Safe method to start server.
	 */
	public synchronized void startThread() {
		if (mCommandSocket == null) {
			try {
				mCommandSocket = new ServerSocket();
				mCommandSocket.setReuseAddress(true);
				mCommandSocket.setReceiveBufferSize(10);
				mCommandSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), SERVER_PORT),
						SERVER_BACKLOG);
				RequestWorker worker = new RequestWorker(mCommandSocket);
				mCommandWorker = new Thread(worker);
				worker.setWorkerThread(mCommandWorker);
				mCommandWorker.setDaemon(false);
				mCommandWorker.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (mCommandSocket != null && mStateSocket == null) {
			try {
				mStateSocket = new ServerSocket();
				mStateSocket.setReuseAddress(true);
				mStateSocket.setReceiveBufferSize(10);
				mStateSocket
						.bind(new InetSocketAddress(InetAddress.getByName("localhost"), STATE_PORT), SERVER_BACKLOG);
				RequestWorker worker = new RequestWorker(mStateSocket);
				mStateWorker = new Thread(worker);
				worker.setWorkerThread(mStateWorker);
				mStateWorker.setDaemon(false);
				mStateWorker.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Safe method to stop server.
	 */
	public synchronized void stopThread() {
		if (mCommandSocket == null)
			return;
		try {
			mCommandSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			mCommandSocket = null;
		}
		if (mCommandWorker != null) {
			mCommandWorker.interrupt();
		}
	}

	/**
	 * Returns true is {@link HttpServer} is started.
	 * 
	 * @return {@link Boolean}
	 */
	public synchronized boolean isRunning() {
		return mCommandSocket != null && !mCommandSocket.isClosed();
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
		mPatternHandler.setGadgetConfiguration(mConfiguration);
	}

	/**
	 * Locks {@link #mPatternHandler}.
	 */
	public void lock() {
		if (mPatternHandler != null) {
			mPatternHandler.lock();
		}
	}

	/**
	 * Unlocks {@link #mPatternHandler}.
	 */
	public void unlock() {
		if (mPatternHandler != null) {
			mPatternHandler.unlock();
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

		private ServerSocket mSocket;
		private Thread mWorkerThread;

		public RequestWorker(ServerSocket socket) {
			mSocket = socket;
		}

		public void setWorkerThread(Thread workerThread) {
			mWorkerThread = workerThread;
		}

		@Override
		public void run() {
			try {
				Log.d("Server",
						"Server now running! " + mSocket.getInetAddress().toString() + ":" + mSocket.getLocalPort());
				while ((mSocket != null) && (mWorkerThread == Thread.currentThread()) && !Thread.interrupted()) {
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
			// conn.bind(socket, mHttpParams);
			conn.bind(socket, new BasicHttpParams());

			HttpService httpService = new HttpService(mHttpProcessor, mReuseStrat, new DefaultHttpResponseFactory());
			// httpService.setParams(mHttpParams);
			httpService.setHandlerResolver(mHttpRegistry);

			Thread t = new Thread(new Worker(httpService, conn, mSocket));
			workerThreads.add(t);
			t.setDaemon(true);
			t.start();
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
			private final ServerSocket mSocket;

			public Worker(final HttpService httpservice, final HttpServerConnection conn, ServerSocket socket) {
				this.httpservice = httpservice;
				this.conn = conn;
				this.mSocket = socket;
			}

			@Override
			public void run() {
				HttpContext context = new BasicHttpContext(null);
				try {
					while ((this.mSocket != null) && this.conn.isOpen() && !Thread.interrupted()) {
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
