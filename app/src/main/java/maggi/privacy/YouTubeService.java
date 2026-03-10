package maggi.privacy;

import android.os.Environment;
import android.util.Log;
import okhttp3.*;
import org.json.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class YouTubeService {
    private static final String TAG = "YouTubeService";
    private static final String SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?alt=json&key=AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3";
    private final OkHttpClient client = new OkHttpClient();

    public interface SearchCallback {
        void onSuccess(List<Track> tracks);
        void onError(Exception e);
    }

    public void search(String query, SearchCallback callback) {
        try {
            JSONObject context = new JSONObject();
            JSONObject clientObj = new JSONObject();
            clientObj.put("clientName", "WEB_REMIX");
            clientObj.put("clientVersion", "1.20240101.01.00");
            clientObj.put("hl", "pt-BR");
            clientObj.put("gl", "BR");
            context.put("client", clientObj);

            JSONObject payload = new JSONObject();
            payload.put("context", context);
            payload.put("query", query);

            RequestBody body = RequestBody.create(payload.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(SEARCH_URL)
                    .post(body)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Origin", "https://music.youtube.com")
                    .addHeader("Referer", "https://music.youtube.com/search")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError(new Exception("Erro HTTP: " + response.code()));
                        return;
                    }
                    try {
                        String data = response.body().string();
                        saveJsonToStorage(data);
                        List<Track> tracks = parseSearchJson(data);
                        callback.onSuccess(tracks);
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao processar JSON", e);
                        callback.onError(e);
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e);
                }
            });
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    private void saveJsonToStorage(String json) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, "youtube_response.json");
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(json.getBytes());
            stream.close();
        } catch (Exception ignored) {}
    }

    private List<Track> parseSearchJson(String json) throws Exception {
        List<Track> list = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        
        // O JSON do YouTube Music é altamente aninhado. 
        // Vamos usar uma abordagem de busca profunda para encontrar todos os musicResponsiveListItemRenderer
        findItemsRecursively(root, list);
        
        return list;
    }

    private void findItemsRecursively(Object obj, List<Track> list) {
        if (obj instanceof JSONObject) {
            JSONObject json = (JSONObject) obj;
            if (json.has("musicResponsiveListItemRenderer")) {
                try {
                    Track track = parseItem(json.getJSONObject("musicResponsiveListItemRenderer"));
                    if (track != null) {
                        // Evitar duplicados por videoId
                        boolean exists = false;
                        for (Track t : list) {
                            if (t.getVideoId().equals(track.getVideoId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) list.add(track);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao parsear item", e);
                }
            } else {
                for (java.util.Iterator<String> it = json.keys(); it.hasNext(); ) {
                    String key = it.next();
                    try {
                        findItemsRecursively(json.get(key), list);
                    } catch (JSONException ignored) {}
                }
            }
        } else if (obj instanceof JSONArray) {
            JSONArray array = (JSONArray) obj;
            for (int i = 0; i < array.length(); i++) {
                try {
                    findItemsRecursively(array.get(i), list);
                } catch (JSONException ignored) {}
            }
        }
    }

    private Track parseItem(JSONObject renderer) {
        try {
            String videoId = "";
            
            // Tenta pegar o videoId de múltiplos lugares possíveis
            if (renderer.has("playlistItemData")) {
                videoId = renderer.getJSONObject("playlistItemData").getString("videoId");
            } else if (renderer.has("navigationEndpoint")) {
                JSONObject endpoint = renderer.getJSONObject("navigationEndpoint");
                if (endpoint.has("watchEndpoint")) {
                    videoId = endpoint.getJSONObject("watchEndpoint").getString("videoId");
                }
            }
            
            // Se não achou no topo, tenta no overlay (botão de play)
            if (videoId.isEmpty() && renderer.has("overlay")) {
                try {
                    videoId = renderer.getJSONObject("overlay")
                            .getJSONObject("musicItemThumbnailOverlayRenderer")
                            .getJSONObject("content")
                            .getJSONObject("musicPlayButtonRenderer")
                            .getJSONObject("playNavigationEndpoint")
                            .getJSONObject("watchEndpoint")
                            .getString("videoId");
                } catch (Exception ignored) {}
            }

            if (videoId.isEmpty()) return null;

            JSONArray flexColumns = renderer.getJSONArray("flexColumns");
            
            // Título: Primeira coluna, primeiro run
            String title = flexColumns.getJSONObject(0)
                    .getJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    .getJSONObject("text").getJSONArray("runs")
                    .getJSONObject(0).getString("text");

            // Artista: Segunda coluna, procurar por runs que não sejam separadores ou tipos
            String artist = "YouTube Music";
            if (flexColumns.length() > 1) {
                JSONArray runs = flexColumns.getJSONObject(1)
                        .getJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        .getJSONObject("text").getJSONArray("runs");
                
                for (int i = 0; i < runs.length(); i++) {
                    JSONObject run = runs.getJSONObject(i);
                    String text = run.getString("text");
                    
                    // Se o run tem um browseId de artista, é definitivamente o artista
                    if (run.has("navigationEndpoint")) {
                        JSONObject nav = run.getJSONObject("navigationEndpoint");
                        if (nav.has("browseEndpoint")) {
                            String pageType = "";
                            try {
                                pageType = nav.getJSONObject("browseEndpoint")
                                        .getJSONObject("browseEndpointContextSupportedConfigs")
                                        .getJSONObject("browseEndpointContextMusicConfig")
                                        .getString("pageType");
                            } catch (Exception ignored) {}
                            
                            if ("MUSIC_PAGE_TYPE_ARTIST".equals(pageType)) {
                                artist = text;
                                break;
                            }
                        }
                    }
                    
                    // Fallback: primeiro texto que não seja lixo
                    if (artist.equals("YouTube Music") && !text.contains("•") && !text.equals("Música") && !text.equals("Vídeo")) {
                        artist = text;
                    }
                }
            }

            // Thumbnail
            String thumb = "";
            if (renderer.has("thumbnail")) {
                JSONArray thumbnails = renderer.getJSONObject("thumbnail")
                        .getJSONObject("musicThumbnailRenderer")
                        .getJSONObject("thumbnail").getJSONArray("thumbnails");
                thumb = thumbnails.getJSONObject(thumbnails.length() - 1).getString("url");
            }

            return new Track(videoId, title, artist, thumb);
        } catch (Exception e) {
            return null;
        }
    }
}
