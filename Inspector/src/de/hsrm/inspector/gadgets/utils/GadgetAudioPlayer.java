package de.hsrm.inspector.gadgets.utils;

import java.io.IOException;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;

public class GadgetAudioPlayer extends MediaPlayer implements OnPreparedListener, OnCompletionListener {

	private boolean mPrepared;
	private boolean mAutoPlay;

	@Override
	public void onPrepared(MediaPlayer mp) {
		mPrepared = true;
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
	}

	public void prepareAsync(boolean autoPlay) throws IllegalStateException {
		super.prepareAsync();
		mAutoPlay = autoPlay;
	}

}
