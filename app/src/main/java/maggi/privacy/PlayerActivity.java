package maggi.privacy;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.ui.PlayerControlView;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

@UnstableApi
public class PlayerActivity extends AppCompatActivity {

    private ImageView maggiThumb;
    private TextView maggiTitle, maggiArtist;
    private PlayerControlView maggiControls;
    
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initViews();
        initMediaController();
    }

    private void initViews() {
        maggiThumb = findViewById(R.id.player_big_thumb);
        maggiTitle = findViewById(R.id.player_big_title);
        maggiArtist = findViewById(R.id.player_big_artist);
        maggiControls = findViewById(R.id.player_controls);
    }

    private void initMediaController() {
        // Conecta ao PlayerService que você já definiu
        SessionToken sessionToken = new SessionToken(this, 
                new ComponentName(this, PlayerService.class));
        
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                // Vincula os controles do XML ao player do serviço
                maggiControls.setPlayer(mediaController);
                
                // Listener para atualizar a UI quando a música mudar
                mediaController.addListener(new Player.Listener() {
                    @Override
                    public void onMediaMetadataChanged(@NonNull MediaMetadata mediaMetadata) {
                        updateUI(mediaMetadata);
                    }
                });

                // Atualização inicial
                if (mediaController.getCurrentMediaItem() != null) {
                    updateUI(mediaController.getMediaMetadata());
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Erro ao conectar ao player", Toast.LENGTH_SHORT).show();
            }
        }, MoreExecutors.directExecutor());
    }

    private void updateUI(MediaMetadata metadata) {
        maggiTitle.setText(metadata.title != null ? metadata.title : "Título desconhecido");
        maggiArtist.setText(metadata.artist != null ? metadata.artist : "Artista desconhecido");

        if (metadata.artworkUri != null) {
            Glide.with(this)
                    .load(metadata.artworkUri)
                    .into(maggiThumb);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
    }
}
