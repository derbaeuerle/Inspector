package de.hsrm.inspector.gadgets;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.net.Uri;
import de.hsrm.inspector.constants.AudioConstants;
import de.hsrm.inspector.exceptions.GadgetException;
import de.hsrm.inspector.gadgets.intf.Gadget;
import de.hsrm.inspector.gadgets.intf.OnKeepAliveListener;
import de.hsrm.inspector.gadgets.pool.GadgetEvent;
import de.hsrm.inspector.gadgets.pool.GadgetEvent.EVENT_TYPE;
import de.hsrm.inspector.gadgets.utils.audio.GadgetAudioPlayer;
import de.hsrm.inspector.gadgets.utils.audio.GadgetAudioPlayer.STATE;
import de.hsrm.inspector.handler.utils.InspectorRequest;

/**
 * {@link Gadget} implementation of audio support. This {@link AudioGadget}
 * supports multiple {@link GadgetAudioPlayer} simultaneously.
 */
public class AudioGadget extends Gadget implements OnKeepAliveListener {

	private static final int UPDATE_DELAY = 250;

	/** All running {@link GadgetAudioPlayer} based on their player id. */
	private ConcurrentHashMap<String, GadgetAudioPlayer> mPlayers;
	/**
	 * {@link ScheduledExecutorService} to publish all states in a defined
	 * interval.
	 */
	private ScheduledExecutorService mStateHandler;
	/**
	 * Latch to use timeout mechanism of {@link GadgetAudioPlayer} or not.
	 */
	private boolean mUseTimeout;
	/**
	 * Identicator for running {@link GadgetAudioPlayer} instances inside
	 * {@link #mPlayers}.
	 */
	private AtomicInteger mRunningInstances;

	@Override
	public void onCreate(Context context) throws Exception {
		super.onCreate(context);
		mPlayers = new ConcurrentHashMap<String, GadgetAudioPlayer>();
		mRunningInstances = new AtomicInteger(0);
	}

	@Override
	public void onProcessStart() throws Exception {
		super.onProcessStart();
		mStateHandler = Executors.newScheduledThreadPool(1);
		mStateHandler.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				if (isProcessing()) {
					if (mPlayers.size() > 0) {
						Map<String, Object> states = getPlayerStates();
						if (!states.isEmpty()) {
							notifyGadgetEvent(new GadgetEvent(AudioGadget.this, states, EVENT_TYPE.DATA));
						}
					}
				}
			}
		}, 0, UPDATE_DELAY, TimeUnit.MILLISECONDS);
	}

	@Override
	public void onProcessEnd() throws Exception {
		super.onProcessEnd();
		for (GadgetAudioPlayer mp : mPlayers.values()) {
			try {
				if (useTimeout()) {
					mp.stop(true);
					mp.release();
				}
			} catch (IllegalStateException e) {
			}
		}
		if (mStateHandler != null && !mStateHandler.isShutdown()) {
			mStateHandler.shutdown();
		}
		mPlayers.clear();
	}

	@Override
	public void onKeepAlive(InspectorRequest iRequest) {
		if (mPlayers != null) {
			for (GadgetAudioPlayer mp : mPlayers.values()) {
				mp.stopTimeout();
				if (useTimeout()) {
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
		if (mPlayers.containsKey(playerId) && !mPlayers.get(playerId).getState().equals(STATE.STOPPED)
				&& !mPlayers.get(playerId).getState().equals(STATE.COMPLETED)) {
			mp = mPlayers.get(playerId);
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
				mPlayers.put(playerId, mp);
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
			if (useTimeout()) {
				mp.startTimeout();
			}
			mRunningInstances.incrementAndGet();
		} else if (command.equals(AudioConstants.COMMAND_PAUSE)) {
			if (mp.isPlaying()) {
				mp.pause();
			}
		} else if (command.equals(AudioConstants.COMMAND_STOP)) {
			mp.stop(false);
			mp.release();
			mp.stopTimeout();
			onPlayerDestroy(mp);
			mRunningInstances.decrementAndGet();
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

	/**
	 * Returns all states if {@link GadgetAudioPlayer} in {@link #mPlayers}.
	 * 
	 * @return {@link HashMap}
	 */
	private HashMap<String, Object> getPlayerStates() {
		HashMap<String, Object> players = new HashMap<String, Object>();
		for (String id : mPlayers.keySet()) {
			players.put(id, mPlayers.get(id).getPlayerState());
		}
		return players;
	}

	/**
	 * Removes {@link GadgetAudioPlayer} from {@link #mPlayers} if it has been
	 * destroyed.
	 * 
	 * @param mp
	 *            {@link GadgetAudioPlayer}
	 */
	public void onPlayerDestroy(GadgetAudioPlayer mp) {
		mRunningInstances.decrementAndGet();
	}

	public void onPlayerError(GadgetAudioPlayer mp) {
		mRunningInstances.decrementAndGet();
	}

	/**
	 * Returns {@link #mUseTimeout}
	 * 
	 * @return {@link Boolean}
	 */
	public boolean useTimeout() {
		return mUseTimeout;
	}
}
