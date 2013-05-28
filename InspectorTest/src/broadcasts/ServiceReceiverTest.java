package broadcasts;

import android.content.Intent;
import android.test.AndroidTestCase;
import de.inspector.hsrm.broadcasts.ServiceReceiver;

/**
 * Created by dobae on 27.05.13.
 */
public class ServiceReceiverTest extends AndroidTestCase {

    private ServiceReceiver mServiceReceiver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mServiceReceiver = new ServiceReceiver();
    }

    public void testReceiver() {
        Intent i = new Intent("inspector://de.inspector.hsrm.intent");
        mServiceReceiver.onReceive(getContext(), i);
    }

}