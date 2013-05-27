package de.inspector.hsrm.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import de.inspector.hsrm.R;
import de.inspector.hsrm.gadgets.GadgetStatus;
import de.inspector.hsrm.services.GadgetManager;
import de.inspector.hsrm.services.intf.Gadget;
import de.inspector.hsrm.services.utils.AsyncServiceBinder;
import de.inspector.hsrm.services.utils.AsyncServiceBinderCallable;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by dobae on 25.05.13.
 */
public class ServiceReceiver extends BroadcastReceiver {

    private ServiceConnection mConnection;
    private GadgetManager mManager;
    private Map<String, Gadget> mGadgetConfiguration;
    private GadgetStatus mStatus = GadgetStatus.PENDING;

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri uri = Uri.parse(intent.toURI());
        final List<String> paths = uri.getPathSegments();

        if (mStatus != GadgetStatus.OK) {
            bindManager(context);
        }
        if (mGadgetConfiguration == null) {
            readConfiguration(context, context.getResources().openRawResource(R.raw.inspector));
            mManager.setGadgetConfiguration(mGadgetConfiguration);
        }
        String command = "";
        if (paths.contains("init")) {
            for (int i = paths.indexOf("init") + 1; i < paths.size(); i++) {
                mManager.initGadget(paths.get(i));
            }
        } else if (paths.contains("register")) {
            for (int i = paths.indexOf("register") + 1; i < paths.size(); i++) {
                mManager.register(paths.get(i));
            }
        } else if (paths.contains("unregister")) {
            for (int i = paths.indexOf("unregister") + 1; i < paths.size(); i++) {
                mManager.unregister(paths.get(i));
            }
        } else if (paths.contains("destroy")) {
            for (int i = paths.indexOf("init") + 1; i < paths.size(); i++) {
                mManager.destroy(paths.get(i));
            }
        } else if (paths.contains("close")) {
            mManager.destroyAll();
            context.unbindService(mConnection);
        }
    }

    private void bindManager(Context context) {
        AsyncServiceBinderCallable callable = new AsyncServiceBinderCallable() {
            @Override
            public Object onCall() {
                ServiceReceiver receiver = ServiceReceiver.this;
                receiver.mManager = (GadgetManager) getService();
                receiver.mConnection = getConnection();
                return GadgetStatus.OK;
            }
        };

        AsyncServiceBinder binder = new AsyncServiceBinder(callable, context);
        try {
            mStatus = (GadgetStatus) binder.process(GadgetManager.class);
        } catch (Exception e) {
            mStatus = GadgetStatus.ERROR;
        }
    }

    private void readConfiguration(Context context, InputStream configurationFile) {
        try {
            SAXBuilder builder = new SAXBuilder();
            Element root = builder.build(configurationFile).getRootElement();
            List<Element> gadgets = root.getChildren(context.getString(R.string.configuration_gadgets));

            for (Element gadget : gadgets) {
                createGadget(context, gadget);
            }
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

    private void createGadget(Context context, Element gadget) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        try {
            Class<?> c = Class.forName(gadget.getChildText(context.getString(R.string.configuration_gadgets)));

            boolean multi = Boolean.valueOf(gadget.getAttributeValue(context.getString(R.string.configuration_multiinstance), "false"));
            Class gadgetClass = Class.forName(gadget.getChildText(context.getString(R.string.configuration_class)));

            if (gadget.getChild(context.getString(R.string.configuration_identifiers)) != null) {
                Element identifiers = gadget.getChild(context.getString(R.string.configuration_identifiers));
                for (Element identifier : identifiers.getChildren(context.getString(R.string.configuration_identifier))) {
                    Gadget g = (Gadget) c.newInstance();
                    g.setIdentifier(identifier.getText());
                    g.setMultiinstance(multi);
                    g.setClass(gadgetClass);
                    mGadgetConfiguration.put(g.getIdentifier(), g);
                }
            } else {
                Gadget g = (Gadget) c.newInstance();
                g.setIdentifier(gadget.getChildText(context.getString(R.string.configuration_identifier)));
                g.setMultiinstance(multi);
                g.setClass(gadgetClass);
                mGadgetConfiguration.put(g.getIdentifier(), g);
            }
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }
}
