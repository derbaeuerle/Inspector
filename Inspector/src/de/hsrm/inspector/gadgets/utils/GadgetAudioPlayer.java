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

	public boolean isPrepared() {
		return mPrepared;
	}

	public void stop() {
		super.stop();
		super.release();
		mStopped = true;
		mState = STATE.STOPPED;
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
