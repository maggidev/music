package maggi.privacy;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public class PlayerService extends MediaSessionService {
    private ExoPlayer player;
    private MediaSession mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();

        // Intent que define qual tela abrir ao clicar na notificação
        Intent intent = new Intent(this, PlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        mediaSession = new MediaSession.Builder(this, player)
                .setSessionActivity(pendingIntent) // Vincula a Activity à sessão
                .build();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        mediaSession.release();
        player.release();
        super.onDestroy();
    }
}
