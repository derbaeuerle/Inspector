package de.hsrm.inspector.gadgets;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import de.hsrm.inspector.constants.AudioConstants;
import de.hsrm.inspector.exceptions.GadgetException;
import de.hsrm.inspector.gadgets.communication.GadgetEvent;
import de.hsrm.inspector.gadgets.communication.GadgetEvent.EVENT_TYPE;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.gadgets.utils.audio.GadgetAudioPlayer;
import de.hsrm.inspector.handler.utils.InspectorRequest;

/**
 * {@link Gadget} implementation of audio support. This {@link AudioGadget}
 * supports multiple {@link GadgetAudioPlayer} simultaneously.
 */
public class AudioGadget extends Gadget {

	private static final int UPDATE_DELAY = 250;

	private Map<String, GadgetAudioPlayer> mPlayers;
	private ScheduledExecutorService mStateHandler;

	@Override
	public void onCreate(Context context) throws Exception {
		super.onCreate(context);
		mPlayers = new HashMap<String, GadgetAudioPlayer>();
	}

	@SuppressLint("HandlerLeak")
	@Override
	public void onProcessStart() {
		super.onProcessStart();
		mStateHandler = Executors.newScheduledThreadPool(1);
		mStateHandler.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				if (isProcessing()) {
					if (mPlayers.size() > 0) {
						notifyGadgetEvent(new GadgetEvent(AudioGadget.this, getPlayerStates(), EVENT_TYPE.DATA));
					}
				}
			}
		}, 0, UPDATE_DELAY, TimeUnit.MILLISECONDS);
	}

	@Override
	public void onProcessEnd() {
		super.onProcessEnd();
		for (GadgetAudioPlayer mp : mPlayers.values()) {
			try {
				mp.stop();
				mp.release();
			} catch (IllegalStateException e) {
			}
		}
		if (mStateHandler != null && !mStateHandler.isShutdown()) {
			mStateHandler.shutdown();
		}
	}

	@Override
	public void gogo(InspectorRequest iRequest) throws Exception {
		if (!iRequest.hasParameter(AudioConstants.PARAM_PLAYERID))
			throw new GadgetException("No playerid set for audio gadget.");

		GadgetAudioPlayer mp = null;
		String playerId = iRequest.getParameter(AudioConstants.PARAM_PLAYERID).toString();
		if (mPlayers.containsKey(playerId)) {
			mp = mPlayers.get(playerId);
		} else {
			if (iRequest.getParameter(AudioConstants.PARAM_COMMAND).equals(AudioConstants.COMMAND_PLAY)) {
				boolean loop = false;
				if (iRequest.hasParameter(AudioConstants.PARAM_LOOP)) {
					loop = Boolean.parseBoolean(iRequest.getParameter(AudioConstants.PARAM_LOOP).toString());
				}

				mp = new GadgetAudioPlayer(playerId);
				mp.setLooping(loop);
				mp.setAutoplay(true);

				mp.setDataSource(Uri.decode(iRequest.getParameter(AudioConstants.PARAM_AUDIOFILE).toString()));
				mPlayers.put(iRequest.getParameter(AudioConstants.PARAM_PLAYERID).toString(), mp);
			}
		}
		Log.d("AUDIO", (mp != null) ? mp.toString() : "null");
		if (mp != null) {
			doCommand(iRequest, mp);
		}
	}

	/**
	 * Executes parsed command given inside the request URL.
	 * 
	 * @param iRequest
	 *            {@link InspectorRequest}
	 * @param mp
	 *            {@link GadgetAudioPlayer}
	 * @throws Exception
	 */
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

	private HashMap<String, Object> getPlayerStates() {
		HashMap<String, Object> players = new HashMap<String, Object>();
		for (GadgetAudioPlayer p : mPlayers.values()) {
			players.put(p.getPlayerId(), p.getPlayerState());
		}
		return players;
	}

}
