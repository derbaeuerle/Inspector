package de.inspector.hsrm.gadgets;

import android.content.Context;
import android.media.MediaPlayer;
import de.inspector.hsrm.services.intf.Gadget;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

/**
 * Created by dobae on 27.05.13.
 */
public class AudioGadget extends Gadget implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private MediaPlayer mMediaPlayer;
    private boolean mIsPlaying = false;

    @Override
    public void onCreate(Context context) {
        super.onCreate(context);
        mMediaPlayer = new MediaPlayer();
    }

    @Override
    public void onRegister(Context context) {
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
    }

    @Override
    public void onUnregister(Context context) {
        if (mIsPlaying) {
            mMediaPlayer.stop();
        }
    }

    @Override
    public Object gogo(Context context, HttpRequest request, HttpResponse response, HttpContext http_context) throws Exception {
        String s = "";
        mMediaPlayer.setDataSource(s);
        mMediaPlayer.setLooping(false);
        mMediaPlayer.prepareAsync();
        return "OK";
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mMediaPlayer.start();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mIsPlaying = false;
    }
}
