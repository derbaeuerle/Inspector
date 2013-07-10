package de.hsrm.inspector.gadgets.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;

public class GadgetAudioPlayer extends MediaPlayer implements OnPreparedListener, OnCompletionListener,
		OnBufferingUpdateListener, OnSeekCompleteListener {

	public static enum STATE {
		BUFFERING, PREPARED, PLAYING, PAUSED, STOPPED
	};

	private boolean mPrepared = false;;
	private boolean mAutoPlay = false;
	private boolean mStopped = false;
	private int mDuration;
	private int mBuffered;
	private STATE mState = STATE.BUFFERING;
	private int mSeekTo = Integer.MIN_VALUE;
	private float mLeftVolume = 1f, mRightVolume = 1f;

	public GadgetAudioPlayer() {
		super();
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

	public void setLeftVolume(float left) {
		mLeftVolume = left;
		super.setVolume(left, mRightVolume);
	}

	public void setRightVolume(float right) {
		mRightVolume = right;
		super.setVolume(mLeftVolume, right);
	}

	public boolean isPrepared() {
		return mPrepared;
	}

	public void stop() {
		super.stop();
		super.release();
		mStopped = true;
		mState = STATE.STOPPED;
	}

	public static Map<String, Object> getDefaultState() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("duration", 0);
		map.put("state", STATE.STOPPED);
		map.put("buffered", false);
		map.put("stopped", true);
		map.put("autoplay", false);
		map.put("prepared", false);
		return map;
	}

	public Map<String, Object> getPlayerState() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		if (mPrepared) {
			try {
				map.put("position", this.getCurrentPosition());
			} catch (Exception e) {

			}
		}
		map.put("duration", this.mDuration);
		map.put("state", this.mState.toString());
		map.put("buffered", this.mBuffered);
		map.put("stopped", this.mStopped);
		map.put("autoplay", this.mAutoPlay);
		map.put("prepared", this.mPrepared);
		return map;
	}

	public void setSeek(int seek) {
		mSeekTo = seek;
	}

	public void setAutoplay(boolean play) {
		mAutoPlay = play;
	}

	public boolean getAutoplay() {
		return mAutoPlay;
	}

}
