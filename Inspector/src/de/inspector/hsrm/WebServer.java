package de.inspector.hsrm;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import de.inspector.hsrm.handler.PatternHandler;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.protocol.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
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
    private ServerAccessType mServerAccesType;
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
     * @param context       Android application context.
     * @param configuration InputStream for configuration file.
     */
    public WebServer(Context context, InputStream configuration) {
        super(SERVER_NAME);
        isRunning = new AtomicBoolean(false);
        mServerAccesType = ServerAccessType.LOCALHOST;
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

        registry.register(DEFAULT_PATTERN, mHandler);
        httpService.setHandlerResolver(registry);
    }

    @Override
    public void run() {
        super.run();
        /*try {
            SAXBuilder builder = new SAXBuilder();
            Element root = ((Document) builder.build(mConfigurationFile)).getRootElement();
            List<Element> gadgets = root.getChildren(mContext.getString(R.string.configuration_gadgets));

            for (Element gadget : gadgets) {
                try {
                    Class<?> c = Class.forName(gadget.getChildText(mContext.getString(R.string.configuration_multiinstance)));
                    Gadget g = (Gadget) c.newInstance();

                    g.setPattern(gadget.getChildText(mContext.getString(R.string.configuration_identifier)) + ".*");
                    Element service = gadget.getChild(mContext.getString(R.string.configuration_service));
                    if (service != null) {
                        g.setService(service.getText());
                    }
                    Element converter = gadget.getChild(mContext.getString(R.string.configuration_class));
                    if (converter != null) {
                        c = Class.forName(converter.getText());
                        g.setConverter((IResponseConverter) c.newInstance());
                    } else {
                        g.setConverter(new JsonConverter());
                    }
                    mHandler.registerHandler(g);
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
        }*/
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
     * Retrieve current android application context.
     *
     * @return Android application context.
     */
    public Context getContext() {
        return context;
    }

    /**
     * Setting android application context.
     *
     * @param context Android application context.
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
}
