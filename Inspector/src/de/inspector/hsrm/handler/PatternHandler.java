package de.inspector.hsrm.handler;

import android.content.Context;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.util.Log;
import de.inspector.hsrm.gadgets.OldGadget;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PatternHandler implements HttpRequestHandler {

    private List<OldGadget> mOldGadgetRegistry;
    private Context mContext;

    public PatternHandler(Context context) {
        mContext = context;
        mOldGadgetRegistry = new ArrayList<OldGadget>();
    }

    public void registerHandler(OldGadget oldGadget) {
        mOldGadgetRegistry.add(oldGadget);
    }

    public List<OldGadget> getRegistry() {
        return mOldGadgetRegistry;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException,
            IOException {

        Log.d("REQUEST:", request.getRequestLine().toString());
        String uri = request.getRequestLine().getUri();
        Uri requestLine = Uri.parse(request.getRequestLine().getUri());

        UrlQuerySanitizer query = new UrlQuerySanitizer(requestLine.toString());
        final String callback = query.getValue("callback");

        final Object responseContent;

        /*for (final OldGadget oldGadget : mOldGadgetRegistry) {
            if (uri.matches(oldGadget.getPattern())) {
                Object tmpResponseContent = null;
                try {
                    if (oldGadget.usesService()) {
                        AsyncServiceBinderCallable callable = new AsyncServiceBinderCallable(oldGadget, request, context,
                                requestLine);
                        AsyncServiceBinder binder = new AsyncServiceBinder(callable, oldGadget, mContext);
                        tmpResponseContent = binder.process();
                    } else {
                        tmpResponseContent = oldGadget.getConverter().convert(oldGadget.gogo(request, context, requestLine));
                    }
                    response.setHeader("Content-Type", oldGadget.getConverter().getMimeType());
                } catch (Exception e) {
                    e.printStackTrace();

                    Gson gson = new Gson();
                    StringBuilder b = new StringBuilder();
                    for (StackTraceElement s : e.getStackTrace()) {
                        b.append(s.toString() + "\n");
                    }
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put(mContext.getString(R.string.exception_message), e.getMessage());
                    map.put(mContext.getString(R.string.exception_stacktrace), b.toString());
                    tmpResponseContent = gson.toJson(map);
                    response.setHeader("Content-Type", mContext.getString(R.string.mime_json));
                }
                response.addHeader("Access-Control-Allow-Origin", "*");
                response.addHeader("Access-Control-Allow-Methods", "*");
                responseContent = tmpResponseContent;

                HttpEntity entity = new EntityTemplate(new ContentProducer() {
                    public void writeTo(final OutputStream outstream) throws IOException {
                        OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
                        String resp = callback + "(" + responseContent.toString() + ")";
                        Log.d("", "returning: " + resp);
                        Log.d("RESPONSE:", resp);
                        writer.write(resp);
                        writer.flush();
                    }
                });
                response.setEntity(entity);
                break;
            }
        }*/
    }
}
