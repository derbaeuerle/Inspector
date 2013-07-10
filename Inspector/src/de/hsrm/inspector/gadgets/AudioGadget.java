package de.hsrm.inspector.gadgets;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import de.hsrm.inspector.constants.AudioConstants;
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
		if (!iRequest.hasParameter(AudioConstants.PARAM_PLAYERID))
			throw new GadgetException("No playerid or command set for audio gadget.");

		GadgetAudioPlayer mp = null;
		if (mPlayers.containsKey(iRequest.getParameter(AudioConstants.PARAM_PLAYERID).toString())) {
			mp = mPlayers.get(iRequest.getParameter(AudioConstants.PARAM_PLAYERID));
		} else {
			if (iRequest.getParameter(AudioConstants.PARAM_COMMAND).equals(AudioConstants.COMMAND_PLAY)) {
				boolean autoplay = false, loop = false;
				if (iRequest.hasParameter(AudioConstants.PARAM_AUTOPLAY)) {
					autoplay = Boolean.parseBoolean(iRequest.getParameter(AudioConstants.PARAM_AUTOPLAY).toString());
				}
				if (iRequest.hasParameter(AudioConstants.PARAM_LOOP)) {
					loop = Boolean.parseBoolean(iRequest.getParameter(AudioConstants.PARAM_LOOP).toString());
				}

				mp = new GadgetAudioPlayer();
				mp.setLooping(loop);
				mp.setAutoplay(autoplay);

				String src = Uri.decode(iRequest.getParameter(AudioConstants.PARAM_AUDIOFILE).toString());
				mp.setDataSource(src);
				mPlayers.put(iRequest.getParameter(AudioConstants.PARAM_PLAYERID).toString(), mp);
			}
		}
		if (mp != null) {
			doCommand(iRequest, mp);
			return mp.getPlayerState();
		}
		return GadgetAudioPlayer.getDefaultState();
	}

	private void doCommand(InspectorRequest iRequest, GadgetAudioPlayer mp) throws Exception {
		if (!iRequest.hasParameter(AudioConstants.PARAM_COMMAND))
			throw new GadgetException("No command set for audio gadget.");

		Object command = iRequest.getParameter(AudioConstants.PARAM_COMMAND);

		if (command.equals(AudioConstants.COMMAND_PLAY)) {
			if (mp.isPrepared()) {
				if (!mp.isPlaying()) {
					mp.start();
				}
			} else {
				try {
					mp.setAutoplay(true);
					mp.prepareAsync();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
			}
		} else if (command.equals(AudioConstants.COMMAND_PAUSE)) {
			if (mp.isPlaying()) {
				mp.pause();
			}
		} else if (command.equals(AudioConstants.COMMAND_STOP)) {
			mp.stop();
			mPlayers.remove(iRequest.getParameter(AudioConstants.PARAM_PLAYERID).toString());
		} else if (command.equals(AudioConstants.COMMAND_SEEK)) {
			if (iRequest.hasParameter(AudioConstants.PARAM_SEEK_TO)) {
				mp.seekTo(Integer.parseInt(iRequest.getParameter(AudioConstants.PARAM_SEEK_TO).toString()));
			}
		} else if (command.equals(AudioConstants.COMMAND_VOLUME)) {
			if (iRequest.hasParameter(AudioConstants.PARAM_VOLUME)) {
				float vol = Integer.parseInt(iRequest.getParameter(AudioConstants.PARAM_VOLUME).toString()) / 100f;
				mp.setVolume(vol, vol);
			}
			if (iRequest.hasParameter(AudioConstants.PARAM_VOLUME_LEFT)) {
				float vol = Integer.parseInt(iRequest.getParameter(AudioConstants.PARAM_VOLUME_LEFT).toString()) / 100f;
				mp.setLeftVolume(vol);
			}
			if (iRequest.hasParameter(AudioConstants.PARAM_VOLUME_RIGHT)) {
				float vol = Integer.parseInt(iRequest.getParameter(AudioConstants.PARAM_VOLUME_RIGHT).toString()) / 100f;
				mp.setRightVolume(vol);
			}
		}
	}
}
