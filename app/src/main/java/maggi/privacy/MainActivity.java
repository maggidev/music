package maggi.privacy;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private YouTubeService youtubeService;
    private TrackAdapter searchAdapter;
    private TrackAdapter libraryAdapter;
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    private View miniPlayer;
    private ImageView miniPlayerThumb;
    private TextView miniPlayerTitle, miniPlayerArtist;
    private MaterialButton miniPlayerPlayPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        youtubeService = new YouTubeService();
        initUI();
        initMediaController();
    }

    private void initUI() {
        RecyclerView rvLibrary = findViewById(R.id.rv_library);
        rvLibrary.setLayoutManager(new LinearLayoutManager(this));
        libraryAdapter = new TrackAdapter(this::playTrack);
        rvLibrary.setAdapter(libraryAdapter);

        findViewById(R.id.btn_search_trigger).setOnClickListener(v -> showSearchBottomSheet());

        miniPlayer = findViewById(R.id.mini_player);
        miniPlayerThumb = findViewById(R.id.mini_player_thumb);
        miniPlayerTitle = findViewById(R.id.mini_player_title);
        miniPlayerArtist = findViewById(R.id.mini_player_artist);
        miniPlayerPlayPause = findViewById(R.id.mini_player_play_pause);

        miniPlayerPlayPause.setOnClickListener(v -> {
            if (mediaController != null) {
                if (mediaController.isPlaying()) {
                    mediaController.pause();
                } else {
                    mediaController.play();
                }
            }
        });
        
        // Abrir a PlayerActivity ao clicar no mini player
        miniPlayer.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, PlayerActivity.class);
            startActivity(intent);
        });
    }

    private void initMediaController() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, PlayerService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                updateMiniPlayerUI();
                mediaController.addListener(new Player.Listener() {
                    @Override
                    public void onMediaMetadataChanged(@NonNull MediaMetadata mediaMetadata) {
                        updateMiniPlayerUI();
                    }

                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        miniPlayerPlayPause.setIconResource(isPlaying ? 
                            android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    private void updateMiniPlayerUI() {
        if (mediaController != null && mediaController.getCurrentMediaItem() != null) {
            MediaMetadata metadata = mediaController.getMediaMetadata();
            miniPlayer.setVisibility(View.VISIBLE);
            miniPlayerTitle.setText(metadata.title);
            miniPlayerArtist.setText(metadata.artist);
            if (metadata.artworkUri != null) {
                Glide.with(this).load(metadata.artworkUri).into(miniPlayerThumb);
            }
        }
    }

    private void showSearchBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_search, null);
        dialog.setContentView(view);

        RecyclerView rvResults = view.findViewById(R.id.rv_results);
        ProgressBar progress = view.findViewById(R.id.progress_bar);
        EditText searchBar = view.findViewById(R.id.search_bar_music);

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new TrackAdapter(track -> {
            playTrack(track);
            dialog.dismiss();
        });
        rvResults.setAdapter(searchAdapter);

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable searchRunnable = () -> {
            String query = searchBar.getText().toString().trim();
            if (query.length() > 2) {
                progress.setVisibility(View.VISIBLE);
                youtubeService.search(query, new YouTubeService.SearchCallback() {
                    @Override
                    public void onSuccess(List<Track> tracks) {
                        runOnUiThread(() -> {
                            progress.setVisibility(View.GONE);
                            searchAdapter.submitList(tracks);
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            progress.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Erro na busca", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        };

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(searchRunnable);
                if (s.length() > 2) handler.postDelayed(searchRunnable, 600);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }

    private void playTrack(Track track) {
        if (mediaController != null) {
            // Adiciona na biblioteca local (UI)
            List<Track> currentLib = new ArrayList<>(libraryAdapter.getCurrentList());
            if (currentLib.stream().noneMatch(t -> t.getVideoId().equals(track.getVideoId()))) {
                currentLib.add(0, track);
                libraryAdapter.submitList(currentLib);
            }

            // Metadados para a notificação e UI
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .setTitle(track.getTitle())
                    .setArtist(track.getArtist())
                    .setArtworkUri(Uri.parse(track.getThumbnailUrl()))
                    .build();

            // EXTRAÇÃO REAL DO ÁUDIO
            YouTubeExtractor.getAudioUrl(track.getVideoId(), new YouTubeExtractor.ExtractorCallback() {
                @Override
                public void onSuccess(String audioUrl) {
                    runOnUiThread(() -> {
                        MediaItem mediaItem = new MediaItem.Builder()
                                .setMediaId(track.getVideoId())
                                .setMediaMetadata(metadata)
                                .setUri(Uri.parse(audioUrl))
                                .build();

                        mediaController.setMediaItem(mediaItem);
                        mediaController.prepare();
                        mediaController.play();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, 
                        "Erro ao extrair áudio do YouTube", Toast.LENGTH_SHORT).show());
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
        super.onDestroy();
    }
}
