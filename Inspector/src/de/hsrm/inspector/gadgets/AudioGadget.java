package de.hsrm.inspector.gadgets;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import de.hsrm.inspector.constants.AudioServiceConstants;
import de.hsrm.inspector.exceptions.GadgetException;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.gadgets.utils.GadgetAudioPlayer;
import de.hsrm.inspector.handler.utils.InspectorRequest;

/**
 * Created by dobae on 27.05.13.
 */
public class AudioGadget extends Gadget {

	private Map<String, GadgetAudioPlayer> mPlayers;

	@Override
	public void onCreate(Context context) {
		super.onCreate(context);
		mPlayers = new HashMap<String, GadgetAudioPlayer>();
	}

	@Override
	public void onUnregister(Context context) {
		super.onUnregister(context);
		for (MediaPlayer mp : mPlayers.values()) {
			if (mp.isPlaying()) {
				mp.stop();
			}
			mp.release();
		}
	}

	@Override
	public Object gogo(Context context, InspectorRequest iRequest, HttpRequest request, HttpResponse response,
			HttpContext http_context) throws Exception {
		if (!iRequest.hasParameter(AudioServiceConstants.PARAM_PLAYERID))
			throw new GadgetException("No playerid or command set for audio gadget.");

		GadgetAudioPlayer mp = null;
		if (mPlayers.containsKey(iRequest.getParameter(AudioServiceConstants.PARAM_PLAYERID).toString())) {
			mp = mPlayers.get(iRequest.getParameter(AudioServiceConstants.PARAM_PLAYERID));
		} else {
			if (iRequest.getParameter(AudioServiceConstants.PARAM_COMMAND).equals(AudioServiceConstants.COMMAND_PLAY)) {
				mp = new GadgetAudioPlayer();
				String src = Uri.decode(iRequest.getParameter(AudioServiceConstants.PARAM_AUDIOFILE).toString());
				mp.setDataSource(src);
				mPlayers.put(iRequest.getParameter(AudioServiceConstants.PARAM_PLAYERID).toString(), mp);
			}
		}
		if (mp != null) {
			doCommand(iRequest, mp);
			return mp.getPlayerState();
		}
		return "";
	}

	private void doCommand(InspectorRequest iRequest, GadgetAudioPlayer mp) throws GadgetException {
		if (!iRequest.hasParameter(AudioServiceConstants.PARAM_COMMAND))
			throw new GadgetException("No command set for audio gadget.");

		if (iRequest.getParameter(AudioServiceConstants.PARAM_COMMAND).equals(AudioServiceConstants.COMMAND_PLAY)) {
			if (mp.isPrepared()) {
				if (!mp.isPlaying()) {
					mp.start();
				}
			} else {
				mp.prepareAsync(true);
			}
		} else if (iRequest.getParameter(AudioServiceConstants.PARAM_COMMAND).equals(
				AudioServiceConstants.COMMAND_PAUSE)) {
			if (mp.isPlaying()) {
				mp.pause();
			}
		} else if (iRequest.getParameter(AudioServiceConstants.PARAM_COMMAND)
				.equals(AudioServiceConstants.COMMAND_STOP)) {
			mp.stop();
			mPlayers.remove(iRequest.getParameter(AudioServiceConstants.PARAM_PLAYERID).toString());
		}
	}

}
