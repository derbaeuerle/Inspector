package de.hsrm.inspector.gadgets.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;

public class GadgetAudioPlayer extends MediaPlayer implements OnPreparedListener, OnCompletionListener,
		OnBufferingUpdateListener {

	public static enum STATE {
		BUFFERING, PREPARED, PLAYING, PAUSED, STOPPED
	};

	private boolean mPrepared = false;;
	private boolean mAutoPlay = false;
	private boolean mStopped = false;
	private int mBuffered;
	private STATE mState = STATE.BUFFERING;

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
		if (mAutoPlay) {
			mp.start();
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		mPrepared = false;
	}

	public boolean isPrepared() {
		return mPrepared;
	}

	public void prepare(boolean autoPlay) throws IllegalStateException, IOException {
		super.prepare();
		mAutoPlay = autoPlay;
		mState = STATE.BUFFERING;
	}

	public void prepareAsync(boolean autoPlay) throws IllegalStateException {
		super.prepareAsync();
		mAutoPlay = autoPlay;
		mState = STATE.BUFFERING;
	}

	public void stop() {
		super.stop();
		super.release();
		mStopped = true;
	}

	public Map<String, Object> getPlayerState() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		if (!mStopped) {
			map.put("duration", this.getDuration());
			map.put("position", this.getCurrentPosition());
			map.put("state", this.mState.toString());
		}
		map.put("buffered", this.mBuffered);
		map.put("stopped", this.mStopped);
		map.put("autoplay", this.mAutoPlay);
		map.put("prepared", this.mPrepared);
		return map;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		mBuffered = percent;
	}

}
