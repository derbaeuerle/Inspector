package de.hsrm.inspector.gadgets;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
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
import de.hsrm.inspector.gadgets.intf.OnKeepAliveListener;
import de.hsrm.inspector.gadgets.utils.audio.GadgetAudioPlayer;
import de.hsrm.inspector.handler.utils.InspectorRequest;

/**
 * {@link Gadget} implementation of audio support. This {@link AudioGadget}
 * supports multiple {@link GadgetAudioPlayer} simultaneously.
 */
public class AudioGadget extends Gadget implements OnKeepAliveListener {

	private static final int UPDATE_DELAY = 250;

	private ConcurrentHashMap<String, Map<String, GadgetAudioPlayer>> mPlayers;
	private ScheduledExecutorService mStateHandler;
	private boolean mUseTimeout;

	@Override
	public void onCreate(Context context) throws Exception {
		super.onCreate(context);
		mPlayers = new ConcurrentHashMap<String, Map<String, GadgetAudioPlayer>>();
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
		for (Map<String, GadgetAudioPlayer> instances : mPlayers.values()) {
			for (GadgetAudioPlayer mp : instances.values()) {
				try {
					if (useTimeout()) {
						mp.stop();
						mp.release();
					}
				} catch (IllegalStateException e) {
				}
			}
		}
		if (mStateHandler != null && !mStateHandler.isShutdown()) {
			mStateHandler.shutdown();
		}
		mPlayers.clear();
	}

	@Override
	public void onKeepAlive(InspectorRequest iRequest) {
		// TODO: change to timeout of browser instance.
		if (mPlayers != null) {
			// There may be instances for this browser instance!
			if (mPlayers.containsKey(iRequest.getBrowserId())) {
				// Restart timeout for players on this instance.
				for (GadgetAudioPlayer mp : mPlayers.get(iRequest.getBrowserId()).values()) {
					mp.stopTimeout();
					mp.startTimeout();
				}
			}
		}
	}

	@Override
	public void gogo(InspectorRequest iRequest) throws Exception {
		if (!iRequest.hasParameter(AudioConstants.PARAM_PLAYERID))
			throw new GadgetException("No playerid set for audio gadget.");

		GadgetAudioPlayer mp = null;
		String playerId = iRequest.getParameter(AudioConstants.PARAM_PLAYERID).toString();
		if (!mPlayers.containsKey(iRequest.getBrowserId())) {
			mPlayers.put(iRequest.getBrowserId(), new HashMap<String, GadgetAudioPlayer>());
		}
		if (mPlayers.get(iRequest.getBrowserId()).containsKey(playerId)) {
			mp = mPlayers.get(iRequest.getBrowserId()).get(playerId);
		} else {
			if (iRequest.getParameter(AudioConstants.PARAM_COMMAND).equals(AudioConstants.COMMAND_PLAY)) {
				boolean loop = false;
				if (iRequest.hasParameter(AudioConstants.PARAM_LOOP)) {
					loop = Boolean.parseBoolean(iRequest.getParameter(AudioConstants.PARAM_LOOP).toString());
				}

				mp = new GadgetAudioPlayer(playerId, this);
				mp.setLooping(loop);
				mp.setAutoplay(true);

				mp.setDataSource(Uri.decode(iRequest.getParameter(AudioConstants.PARAM_AUDIOFILE).toString()));
				mPlayers.get(iRequest.getBrowserId()).put(playerId, mp);
			}
		}
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
				try {
					if (!mp.isPlaying()) {
						mp.start();
					}
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
			} else {
				try {
					mp.setAutoplay(true);
					mp.prepareAsync();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
			}
			mp.startTimeout();
		} else if (command.equals(AudioConstants.COMMAND_PAUSE)) {
			if (mp.isPlaying()) {
				mp.pause();
			}
		} else if (command.equals(AudioConstants.COMMAND_STOP)) {
			mp.stop();
			mp.release();
			mp.stopTimeout();
			onPlayerTimeout(mp);
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
		for (Map<String, GadgetAudioPlayer> instances : mPlayers.values()) {
			for (GadgetAudioPlayer p : instances.values()) {
				players.put(p.getPlayerId(), p.getPlayerState());
			}
		}
		return players;
	}

	public void onPlayerTimeout(GadgetAudioPlayer mp) {
		int size = 0;
		for (Map<String, GadgetAudioPlayer> i : mPlayers.values()) {
			while (i.containsValue(mp)) {
				for (Entry<String, GadgetAudioPlayer> entry : i.entrySet()) {
					if (i.get(entry.getKey()).equals(mp)) {
						i.remove(entry.getKey());
						break;
					}
				}
			}
			size += i.size();
		}
		if (size == 0) {
			notifyGadgetEvent(new GadgetEvent(this, null, EVENT_TYPE.DESTROY));
		}
	}

	public boolean useTimeout() {
		return mUseTimeout;
	}
}
