package de.inspector.hsrm.services.intf;

import android.content.Context;
import de.inspector.hsrm.exceptions.GadgetException;
import de.inspector.hsrm.handler.utils.InspectorRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

/**
 * Created by dobae on 25.05.13.
 */
public abstract class Gadget {

    private String mIdentifier;
    private Class mClass;
    private boolean mKeepAlive;
    private int mTimeout;

    public Gadget() {
        this("", de.inspector.hsrm.services.intf.Gadget.class);
    }

    /**
     * Constructor for configuration objects.
     *
     * @param identifier
     * @param clazz
     */
    public Gadget(String identifier, Class clazz) {
        super();
        mIdentifier = identifier;
        mClass = clazz;
    }

    /**
     * Creating instances of this Gadget for runtime.
     *
     * @param context {Context}
     * @return {Gadget}
     */
    public Gadget createInstance(Context context) throws GadgetException {
        Gadget g = null;
        try {
            g = (Gadget) mClass.newInstance();
            g.setIdentifier(mIdentifier);
            g.onCreate(context);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return g;
    }

    /**
     * Gets called when a instance of this gadget gets created.
     *
     * @param context {Context}
     */
    public void onCreate(Context context) {

    }

    /**
     * Gets called when a instance of this gadget gets removed from runtime process.
     *
     * @param context {Context}
     */
    public void onDestroy(Context context) {

    }

    /**
     * Gets called when a instance of this gadget gets registered to runtime process.
     *
     * @param context {Context}
     */
    public void onRegister(Context context) {

    }

    /**
     * Gets called when a instance of this gadget gets unregistered to runtime process.
     *
     * @param context {Context}
     */
    public void onUnregister(Context context) {
    }

    /**
     * Handles a request from browser for this gadget and returns Object which will be serialized to JSON.∞
     *
     * @param context      {Context}
     * @param iRequest     {InspectorRequest}
     * @param request      {HttpRequest}
     * @param response     {HttpResponse}
     * @param http_context {HttpContext}
     * @return {Object}
     * @throws Exception
     */
    public abstract Object gogo(Context context, InspectorRequest iRequest, HttpRequest request, HttpResponse response, HttpContext http_context) throws Exception;

    public String getIdentifier() {
        return mIdentifier;
    }

    public Gadget setIdentifier(String identifier) {
        mIdentifier = identifier;
        return this;
    }

    public void setClass(Class clazz) {
        this.mClass = clazz;
    }

    public boolean isKeepAlive() {
        return mKeepAlive;
    }

    public void setKeepAlive(boolean mKeepAlive) {
        this.mKeepAlive = mKeepAlive;
    }

    public int getTimeout() {
        return mTimeout;
    }

    public void setTimeout(int mTimeout) {
        this.mTimeout = mTimeout;
    }

}
