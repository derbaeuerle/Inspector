package de.inspector.hsrm.services.intf;

import android.content.Context;
import de.inspector.hsrm.exceptions.GadgetException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

/**
 * Created by dobae on 25.05.13.
 */
public abstract class Gadget {

    private String mIdentifier;
    private Class mClass;
    private String mUniqueIdentifier;
    private boolean mMultiinstance = false;
    private boolean mInitialized = false;

    public Gadget() {
        this("", de.inspector.hsrm.services.intf.Gadget.class, false);
    }

    /**
     * Constructor for configuration objects.
     *
     * @param identifier
     * @param clazz
     * @param multiInstance
     */
    public Gadget(String identifier, Class clazz, boolean multiInstance) {
        super();
        mIdentifier = identifier;
        mMultiinstance = multiInstance;
        mClass = clazz;
    }

    /**
     * Creating instances of this Gadget for runtime.
     *
     * @return {Gadget}
     */
    public Gadget createInstance() throws GadgetException {
        Gadget g = null;
        try {
            if (!mMultiinstance && mInitialized) {
                throw new GadgetException("Gadget already initialized and running!");
            }
            g = (Gadget) mClass.newInstance();
            g.setIdentifier(mIdentifier);
            g.setMultiinstance(mMultiinstance);
            mInitialized = true;
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
     * @param request      {HttpRequest}
     * @param response     {HttpResponse}
     * @param http_context {HttpContext}
     * @return {Object}
     * @throws Exception
     */
    public abstract Object gogo(Context context, HttpRequest request, HttpResponse response, HttpContext http_context) throws Exception;

    public String getUniqueIdentifier() {
        if (mUniqueIdentifier == null) {
            mUniqueIdentifier = "" + (System.currentTimeMillis() % hashCode()) + java.util.UUID.randomUUID();
        }
        return mUniqueIdentifier;
    }

    public boolean isMultiInstance() {
        return mMultiinstance;
    }

    public Gadget setMultiinstance(boolean multiinstance) {
        mMultiinstance = multiinstance;
        return this;
    }

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

}
