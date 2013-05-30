package de.hsrm.inspector.handler;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.google.gson.Gson;
import de.hsrm.inspector.R;
import de.hsrm.inspector.exceptions.GadgetException;
import de.hsrm.inspector.handler.utils.InspectorRequest;
import de.hsrm.inspector.services.intf.Gadget;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class PatternHandler implements HttpRequestHandler {

    private Map<String, Gadget> mGadgetConfiguration;
    private Map<String, Gadget> mGadgetInstances;
    private Context mContext;

    public PatternHandler(Context context) {
        mContext = context;
        mGadgetInstances = new HashMap<String, Gadget>();
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException,
            IOException {

        Log.d("REQUEST:", request.getRequestLine().toString());
        String uri = request.getRequestLine().getUri();
        Uri requestLine = Uri.parse(request.getRequestLine().getUri());
        Object tmpResponseContent = null;
        final Object responseContent;
        Gson gson = new Gson();

        try {
            InspectorRequest iRequest = new InspectorRequest(requestLine);

            if (iRequest.getSegments().contains("destroy")) {
                try {
                    destroyGadget(iRequest.getGadgetIdentifier());
                    tmpResponseContent = iRequest.getCallback() + "(DESTROYED)";
                } catch (Exception e) {
                    tmpResponseContent = iRequest.getCallback() + "(ERROR)";
                }

            } else {
                initGadget(iRequest.getGadgetIdentifier());

                tmpResponseContent = mGadgetInstances.get(iRequest.getGadgetIdentifier()).gogo(mContext, iRequest, request, response, context);
                tmpResponseContent = iRequest.getCallback() + "(" + gson.toJson(tmpResponseContent) + ")";
            }
        } catch (Exception e) {
            e.printStackTrace();
            StringBuilder b = new StringBuilder();
            for (StackTraceElement s : e.getStackTrace()) {
                b.append(s.toString() + "\n");
            }
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(mContext.getString(R.string.exception_message), e.getMessage());
            map.put(mContext.getString(R.string.exception_stacktrace), b.toString());
            tmpResponseContent = gson.toJson(map);
        }
        response.setHeader("Content-Type", mContext.getString(R.string.mime_json));
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "*");
        responseContent = gson.toJson(tmpResponseContent);

        HttpEntity entity = new EntityTemplate(new ContentProducer() {
            public void writeTo(final OutputStream outputStream) throws IOException {
                OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
                writer.write(responseContent.toString());
                writer.flush();
            }
        });
        response.setEntity(entity);

    }

    public void setGadgetConfiguration(Map<String, Gadget> config) {
        this.mGadgetConfiguration = config;
    }

    public void initGadget(String identifier) throws GadgetException {
        if (mGadgetInstances.containsKey(identifier)) {
            return;
        }
        if (!mGadgetConfiguration.containsKey(identifier))
            throw new GadgetException("Unknown gadget identifier: " + identifier);

        try {
            Gadget instance = mGadgetConfiguration.get(identifier).createInstance(mContext);
            instance.onRegister(mContext);
            mGadgetInstances.put(instance.getIdentifier(), instance);
        } catch (GadgetException e) {
            e.printStackTrace();
        }
    }

    public void destroyGadget(String identifier) throws Exception {
        mGadgetInstances.get(identifier).onUnregister(mContext);
        mGadgetInstances.get(identifier).onDestroy(mContext);
        mGadgetInstances.remove(identifier);
    }
}
