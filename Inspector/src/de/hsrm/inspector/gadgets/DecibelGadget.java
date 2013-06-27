package de.hsrm.inspector.gadgets;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.media.MediaRecorder;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.handler.utils.InspectorRequest;

public class DecibelGadget extends Gadget {

	private MediaRecorder mRecorder;
	private String mTempFile = "/dev/null";

	@Override
	public void onCreate(Context context) {
		super.onCreate(context);
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		mRecorder.setOutputFile(mTempFile);
	}

	@Override
	public void onRegister(Context context) {
		super.onRegister(context);
		try {
			mRecorder.prepare();
			mRecorder.start();
		} catch (IOException e) {
		}
	}

	@Override
	public void onUnregister(Context context) {
		super.onUnregister(context);
		mRecorder.stop();
		mRecorder.release();
	}

	@Override
	public void onDestroy(Context context) {
		super.onDestroy(context);
		mRecorder = null;
		try {
			(new File(mTempFile)).delete();
		} catch (Exception e) {

		}
	}

	@Override
	public Object gogo(Context context, InspectorRequest iRequest, HttpRequest request, HttpResponse response,
			HttpContext http_context) throws Exception {
		mRecorder.getMaxAmplitude();
		return mRecorder.getMaxAmplitude();
	}

}
