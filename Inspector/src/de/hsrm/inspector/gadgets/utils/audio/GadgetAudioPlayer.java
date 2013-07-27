package de.hsrm.inspector.gadgets.utils.audio;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;

/**
 * Wrapper class for {@link MediaPlayer} object. This object provides a safer
 * control and implements some additional features. Also this
 * {@link MediaPlayer} registers all listeners on itself.
 */
public class GadgetAudioPlayer extends MediaPlayer implements OnPreparedListener, OnCompletionListener,
		OnBufferingUpdateListener, OnSeekCompleteListener {

	/**
	 * {@link Enum} class of all available states of {@link GadgetAudioPlayer}.
	 */
	public static enum STATE {
		BUFFERING, PREPARED, PLAYING, PAUSED, STOPPED
	};

	private String mPlayerId;
	private boolean mPrepared = false;;
	private boolean mAutoPlay = false;
	private boolean mStopped = false;
	private int mDuration;
	private int mBuffered;
	private STATE mState = STATE.BUFFERING;
	private int mSeekTo = Integer.MIN_VALUE;
	private float mLeftVolume = 1f, mRightVolume = 1f;

	/**
	 * Default constructor.
	 */
	public GadgetAudioPlayer(String id) {
		super();
		mPlayerId = id;
		setOnCompletionListener(this);
		setOnPreparedListener(this);
		setOnBufferingUpdateListener(this);
	}

	@Override
	public void start() throws IllegalStateException {
		super.start();
		mState = STATE.PLAYING;
	}

	@Override
	public void pause() throws IllegalStateException {
		super.pause();
		mState = STATE.PAUSED;
	}

	@Override
	public void prepare() throws IOException, IllegalStateException {
		super.prepare();
		mState = STATE.BUFFERING;
	}

	@Override
	public void prepareAsync() throws IllegalStateException {
		super.prepareAsync();
		mState = STATE.BUFFERING;
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
				mp.start();
			}
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		mPrepared = false;
		if (!isLooping()) {
			stop();
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
	 * Stops playback and releases the source.
	 */
	public void stop() {
		try {
			super.stop();
		} catch (IllegalStateException e) {
		}
		try {
			super.release();
		} catch (IllegalStateException e) {

		}
		mStopped = true;
		mState = STATE.STOPPED;
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
		map.put("playerid", this.mPlayerId);
		map.put("duration", this.mDuration);
		map.put("state", this.mState.toString());
		map.put("buffered", this.mBuffered);
		map.put("stopped", this.mStopped);
		map.put("autoplay", this.mAutoPlay);
		map.put("prepared", this.mPrepared);
		return map;
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

}
