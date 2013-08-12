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

	/**
	 * Milliseconds to stop {@link MediaPlayer} if no keep-alive messages were
	 * received of {@link #mHolder}.
	 */
	private static final int TIMEOUT = 5000;

	/** Identification name of this {@link GadgetAudioPlayer}. */
	private String mPlayerId;
	/** {@link Boolean} if this {@link MediaPlayer} is prepared. */
	private boolean mPrepared = false;;
	/**
	 * {@link Boolean} if this {@link MediaPlayer} should start on finished
	 * preparation.
	 */
	private boolean mAutoPlay = false;
	/** {@link Boolean} if this {@link MediaPlayer} is stopped. */
	private boolean mStopped = false;
	/** Duration of {@link MediaPlayer} source in milliseconds. */
	private int mDuration;
	/** Percentage buffered of {@link MediaPlayer} source. */
	private int mBuffered;
	/** {@link STATE} of this {@link GadgetAudioPlayer}. */
	private STATE mState = STATE.BUFFERING;
	/** Milliseconds to seek to inside {@link MediaPlayer} source. */
	private int mSeekTo = Integer.MIN_VALUE;
	/** Volume of left or right channel. */
	private float mLeftVolume = 1f, mRightVolume = 1f;
	/** {@link Timer} to realize timeout mechanism. */
	private Timer mTimoutTimer;
	/**
	 * {@link AudioGadget} containing this {@link GadgetAudioPlayer} to notify
	 * events.
	 */
	private AudioGadget mHolder;

	/**
	 * Default constructor.
	 */
	/**
	 * @param id
	 * @param gadget
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

	/**
	 * Implementation of {@link MediaPlayer#start()} to start playback and set
	 * {@link #mState} to {@link STATE#PLAYING} if {@link #mState} isn't
	 * {@link STATE#ERROR}.
	 * 
	 * @see android.media.MediaPlayer#start()
	 */
	@Override
	public void start() throws IllegalStateException {
		if (!mState.equals(STATE.ERROR)) {
			super.start();
			mState = STATE.PLAYING;
		}
	}

	/**
	 * Implementation of {@link MediaPlayer#pause()} to pause playback and set
	 * {@link #mState} to {@link STATE#PAUSED}.
	 * 
	 * @see android.media.MediaPlayer#pause()
	 */
	@Override
	public void pause() throws IllegalStateException {
		super.pause();
		mState = STATE.PAUSED;
	}

	/**
	 * Implementation of {@link MediaPlayer#stop()} only callst
	 * {@link #stop(boolean)} with <code>false</code> as parameter.
	 * 
	 * @see android.media.MediaPlayer#stop()
	 */
	@Override
	public void stop() throws IllegalStateException {
		this.stop(false);
	}

	/**
	 * Stops the playback and sets {@link #mStopped} to <code>true</code>. If
	 * given parameter is <code>true</code> then the playback has been completed
	 * and {@link #mState} is set to {@link STATE#COMPLETED} else
	 * {@link #mState} will be {@link STATE#STOPPED}.
	 * 
	 * @param completed
	 */
	public void stop(boolean completed) {
		try {
			super.stop();
		} catch (IllegalStateException e) {
		}
		mStopped = true;
		mState = (completed) ? STATE.COMPLETED : STATE.STOPPED;
	}

	/**
	 * Implementation of {@link MediaPlayer#release()} to release
	 * {@link MediaPlayer} source and set {@link #mPrepared} to
	 * <code>false</code>.
	 * 
	 * @see android.media.MediaPlayer#release()
	 */
	@Override
	public void release() {
		try {
			super.release();
			mPrepared = false;
		} catch (IllegalStateException e) {

		}
	}

	/**
	 * Implementation of {@link MediaPlayer#prepare()} to prepare
	 * {@link MediaPlayer} source and set {@link #mState} to
	 * {@link STATE#BUFFERING} if {@link #mState} isn't {@link STATE#ERROR}.
	 * 
	 * @see android.media.MediaPlayer#prepare()
	 */
	@Override
	public void prepare() throws IOException, IllegalStateException {
		if (!mState.equals(STATE.ERROR)) {
			super.prepare();
			mState = STATE.BUFFERING;
		}
	}

	/**
	 * Implementation of {@link MediaPlayer#prepareAsync()} to prepare
	 * {@link MediaPlayer} source asynchronously and set {@link #mState} to
	 * {@link STATE#BUFFERING} if {@link #mState} isn't {@link STATE#ERROR}.
	 * 
	 * @see android.media.MediaPlayer#prepareAsync()
	 */
	@Override
	public void prepareAsync() throws IllegalStateException {
		if (!mState.equals(STATE.ERROR)) {
			super.prepareAsync();
			mState = STATE.BUFFERING;
		}
	}

	/**
	 * Implementation of {@link MediaPlayer#isPlaying()} to return if
	 * {@link #mState} is {@link STATE#PLAYING}.
	 * 
	 * @see android.media.MediaPlayer#isPlaying()
	 */
	@Override
	public boolean isPlaying() {
		return mState.equals(STATE.PLAYING);
	}

	/**
	 * Implementation of {@link OnPreparedListener#onPrepared(MediaPlayer)} to
	 * set {@link #mPrepared} to <code>true</code>, {@link #mState} to
	 * {@link STATE#PREPARED} and {@link #mDuration} to value of
	 * {@link MediaPlayer#getDuration()}. If {@link #mSeekTo} isn't
	 * {@link Integer#MIN_VALUE} {@link MediaPlayer#seekTo(int)} will be called
	 * with its value. Else {@link #start()} will be called if
	 * {@link #mAutoPlay} is <code>true</code>.
	 * 
	 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media
	 *      .MediaPlayer)
	 */
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

	/**
	 * Implementation of {@link OnCompletionListener#onCompletion(MediaPlayer)}
	 * to call {@link #stop(boolean)} with <code>false</code> as parameter,
	 * {@link #release()} and call
	 * {@link AudioGadget#onPlayerDestroy(GadgetAudioPlayer)} on
	 * {@link #mHolder} if {@link MediaPlayer#isLooping()} returns
	 * <code>false</code>.
	 * 
	 * @see android.media.MediaPlayer.OnCompletionListener#onCompletion(android.media
	 *      .MediaPlayer)
	 */
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

	/**
	 * Implementation of
	 * {@link OnBufferingUpdateListener#onBufferingUpdate(MediaPlayer, int)}
	 * sets {@link #mBuffered} to given value.
	 * 
	 * @see android.media.MediaPlayer.OnBufferingUpdateListener#onBufferingUpdate
	 *      (android.media.MediaPlayer, int)
	 */
	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		mBuffered = percent;
	}

	/**
	 * Implementation of
	 * {@link OnSeekCompleteListener#onSeekComplete(MediaPlayer)} calls
	 * {@link #start()} if {@link #isPrepared()} and {@link #mAutoPlay} returns
	 * <code>true</code>.
	 * 
	 * @see android.media.MediaPlayer.OnSeekCompleteListener#onSeekComplete(android
	 *      .media.MediaPlayer)
	 */
	@Override
	public void onSeekComplete(MediaPlayer mp) {
		if (isPrepared()) {
			if (mAutoPlay) {
				mp.start();
			}
		}
	}

	/**
	 * Implementation of {@link OnErrorListener#onError(MediaPlayer, int, int)}
	 * to call {@link #stop(boolean)} with true, {@link #release()} and set
	 * {@link #mState} to {@link STATE#ERROR}. Also calls
	 * {@link AudioGadget#onPlayerError(GadgetAudioPlayer)} on {@link #mHolder}
	 * to publish any error.
	 * 
	 * @see android.media.MediaPlayer.OnErrorListener#onError(android.media.MediaPlayer
	 *      , int, int)
	 */
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		stop(true);
		release();
		mState = STATE.ERROR;
		mHolder.onPlayerError(this);
		return false;
	}

	/**
	 * Implementation of {@link MediaPlayer#setVolume(float, float)} to set
	 * volume based on {@link #mLeftVolume} and {@link #mRightVolume}.
	 * 
	 * @see android.media.MediaPlayer#setVolume(float, float)
	 */
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

	/**
	 * Returns {@link #mState}.
	 * 
	 * @return {@link STATE}
	 */
	public STATE getState() {
		return mState;
	}

	/**
	 * Returns {@link #mPlayerId}.
	 * 
	 * @return {@link String}
	 */
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
