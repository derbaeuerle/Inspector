package de.inspector.hsrm.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.UrlQuerySanitizer;
import de.inspector.hsrm.services.utils.ServiceBinder;
import de.inspector.hsrm.services.constants.AudioServiceConstants;
import org.apache.http.HttpRequest;

import java.util.HashMap;
import java.util.Map;

public class AudioService extends Service implements MediaPlayer.OnPreparedListener {

    private ServiceBinder mBinder;
    private Map<String, MediaPlayer> mMediaPlayers;

    @Override
    public ServiceBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaPlayers = new HashMap<String, MediaPlayer>();
        mBinder = new ServiceBinder() {

            @Override
            public Service getService() {
                return AudioService.this;
            }
        };
    }

    public void play(HttpRequest request) throws Exception {
        getMediaPlayer(request);
    }

    public void pause(HttpRequest request) throws Exception {
        MediaPlayer player = getMediaPlayer(request);
        player.pause();
    }

    public void stop(HttpRequest request) throws Exception {
        stopPlayer(request);
    }

    private void stopPlayer(HttpRequest request) {
        UrlQuerySanitizer query = new UrlQuerySanitizer(request.getRequestLine().toString());
        String playerId = query.getValue(AudioServiceConstants.PARAM_PLAYERID);
        if (mMediaPlayers.containsKey(playerId)) {
            mMediaPlayers.get(playerId).stop();
            mMediaPlayers.remove(playerId);
        }
    }

    private MediaPlayer getMediaPlayer(HttpRequest request) throws Exception {
        UrlQuerySanitizer query = new UrlQuerySanitizer(request.getRequestLine().toString());
        String playerId = query.getValue(AudioServiceConstants.PARAM_PLAYERID);
        if (!mMediaPlayers.containsKey(playerId)) {
            MediaPlayer player = new MediaPlayer();
            player.setOnPreparedListener(this);
            player.setDataSource(query.getValue(AudioServiceConstants.PARAM_AUDIOFILE));
            if (Boolean.parseBoolean(query.getValue(AudioServiceConstants.PARAM_AUTOPREPARE))) {
                player.prepareAsync();
            } else {
            }
            mMediaPlayers.put(playerId, player);
        } else {
            mMediaPlayers.get(playerId).start();
        }
        return mMediaPlayers.get(playerId);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }
}
