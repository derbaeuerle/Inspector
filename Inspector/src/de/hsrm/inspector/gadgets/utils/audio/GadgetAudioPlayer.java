package de.hsrm.inspector.gadgets.utils.audio;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import de.hsrm.inspector.gadgets.AudioGadget;

/**
 * Wrapper class for {@link MediaPlayer} object. This object provides a safer
 * control and implements some additional features. Also this
 * {@link MediaPlayer} registers all listeners on itself.
 */
public class GadgetAudioPlayer extends MediaPlayer implements OnPreparedListener, OnCompletionListener,
		OnBufferingUpdateListener, OnSeekCompleteListener, OnErrorListener {

	/**
	 * {@link Enum} class of all available states of {@link GadgetAudioPlayer}.
	 */
	public static enum STATE {
		BUFFERING, PREPARED, PLAYING, PAUSED, STOPPED, COMPLETED, ERROR
	};

	private static final int TIMEOUT = 5000;

	private String mPlayerId;
	private boolean mPrepared = false;;
	private boolean mAutoPlay = false;
	private boolean mStopped = false;
	private int mDuration;
	private int mBuffered;
	private STATE mState = STATE.BUFFERING;
	private int mSeekTo = Integer.MIN_VALUE;
	private float mLeftVolume = 1f, mRightVolume = 1f;
	private Timer mTimoutTimer;
	private AudioGadget mHolder;

	/**
	 * Default constructor.
	 */
	public GadgetAudioPlayer(String id, AudioGadget gadget) {
		super();
		mPlayerId = id;
		mHolder = gadget;
		setOnCompletionListener(this);
		setOnPreparedListener(this);
		setOnBufferingUpdateListener(this);
		setOnErrorListener(this);
	}

	@Override
	public void start() throws IllegalStateException {
		if (!mState.equals(STATE.ERROR)) {
			super.start();
			mState = STATE.PLAYING;
		}
	}

	@Override
	public void pause() throws IllegalStateException {
		super.pause();
		mState = STATE.PAUSED;
	}

	@Override
	public void stop() throws IllegalStateException {
		this.stop(false);
	}

	public void stop(boolean completed) {
		try {
			super.stop();
		} catch (IllegalStateException e) {
		}
		mStopped = true;
		mState = (completed) ? STATE.COMPLETED : STATE.STOPPED;
	}

	@Override
	public void release() {
		try {
			super.release();
			mPrepared = false;
		} catch (IllegalStateException e) {

		}
	}

	@Override
	public void prepare() throws IOException, IllegalStateException {
		if (!mState.equals(STATE.ERROR)) {
			super.prepare();
			mState = STATE.BUFFERING;
		}
	}

	@Override
	public void prepareAsync() throws IllegalStateException {
		if (!mState.equals(STATE.ERROR)) {
			super.prepareAsync();
			mState = STATE.BUFFERING;
		}
	}

	@Override
	public boolean isPlaying() {
		return mState.equals(STATE.PLAYING);
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mPrepared = true;
		mState = STATE.PREPARED;
		mDuration = mp.getDuration();
		if (mSeekTo != Integer.MIN_VALUE) {
			mp.seekTo(mSeekTo);
		} else {
			if (mAutoPlay) {
				try {
					mp.start();
				} catch (IllegalStateException e) {
					stop(false);
				}
			}
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		try {
			if (!isLooping()) {
				stop(true);
				release();
				mHolder.onPlayerDestroy(this);
			}
		} catch (IllegalStateException e) {
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		mBuffered = percent;
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		if (isPrepared()) {
			if (mAutoPlay) {
				mp.start();
			}
		}
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		stop(true);
		release();
		mState = STATE.ERROR;
		mHolder.onPlayerError(this);
		return false;
	}

	@Override
	public void setVolume(float leftVolume, float rightVolume) {
		super.setVolume(leftVolume, rightVolume);
		mLeftVolume = leftVolume;
		mRightVolume = rightVolume;
	}

	/**
	 * Sets volume of left audio channel.
	 * 
	 * @param left
	 *            {@link Float}
	 */
	public void setLeftVolume(float left) {
		mLeftVolume = left;
		super.setVolume(left, mRightVolume);
	}

	/**
	 * Sets volume of right audio channel.
	 * 
	 * @param right
	 *            {@link Float}
	 */
	public void setRightVolume(float right) {
		mRightVolume = right;
		super.setVolume(mLeftVolume, right);
	}

	/**
	 * Returns true if {@link GadgetAudioPlayer} is prepared for playback.
	 * 
	 * @return {@link Boolean}
	 */
	public boolean isPrepared() {
		return mPrepared;
	}

	/**
	 * Returns current state of {@link GadgetAudioPlayer} as {@link Map}.
	 * 
	 * @return {@link Map}
	 */
	public Map<String, Object> getPlayerState() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		if (mPrepared) {
			try {
				map.put("position", this.getCurrentPosition());
			} catch (Exception e) {
			}
		}
		try {
			map.put("loop", this.isLooping());
		} catch (Exception e) {
		}
		map.put("playerid", this.mPlayerId);
		map.put("duration", this.mDuration);
		map.put("state", this.mState.toString());
		map.put("buffered", this.mBuffered);
		map.put("stopped", this.mStopped);
		map.put("autoplay", this.mAutoPlay);
		map.put("prepared", this.mPrepared);
		return map;
	}

	public STATE getState() {
		return mState;
	}

	public String getPlayerId() {
		return mPlayerId;
	}

	/**
	 * Seeks {@link MediaPlayer} to given {@link Integer} in milliseconds.
	 * 
	 * @param seek
	 *            {@link Integer} to seek to.
	 */
	public void setSeek(int seek) {
		mSeekTo = seek;
	}

	/**
	 * Sets {@link #mAutoPlay} to start playback on successful preparation.
	 * 
	 * @param play
	 *            {@link Boolean} to set.
	 */
	public void setAutoplay(boolean play) {
		mAutoPlay = play;
	}

	/**
	 * Returns current value of {@link #mAutoPlay}.
	 * 
	 * @return {@link Boolean}
	 */
	public boolean getAutoplay() {
		return mAutoPlay;
	}

	/**
	 * Starts timeout of {@link GadgetAudioPlayer}.
	 */
	public void startTimeout() {
		if (mTimoutTimer != null) {
			mTimoutTimer.cancel();
		}
		mTimoutTimer = new Timer();
		mTimoutTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if (GadgetAudioPlayer.this.mHolder.useTimeout()) {
					GadgetAudioPlayer.this.stop(true);
					GadgetAudioPlayer.this.release();
					GadgetAudioPlayer.this.mHolder.onPlayerDestroy(GadgetAudioPlayer.this);
				}
			}
		}, TIMEOUT);
	}

	/**
	 * Stops timeout of {@link GadgetAudioPlayer}.
	 */
	public void stopTimeout() {
		if (mTimoutTimer != null) {
			mTimoutTimer.cancel();
			mTimoutTimer = null;
		}
	}

}
