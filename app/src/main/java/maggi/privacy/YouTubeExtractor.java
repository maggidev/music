package maggi.privacy;

import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import java.util.List;

public class YouTubeExtractor {

    public interface ExtractorCallback {
        void onSuccess(String audioUrl);
        void onError(Exception e);
    }

    public static void getAudioUrl(String videoId, ExtractorCallback callback) {
        new Thread(() -> {
            try {
                StreamingService service = ServiceList.YouTube;
                String url = "https://www.youtube.com/watch?v=" + videoId;
                StreamInfo info = StreamInfo.getInfo(service, url);
                
                List<AudioStream> audioStreams = info.getAudioStreams();
                if (!audioStreams.isEmpty()) {
                    // Pega o stream de áudio com melhor qualidade (m4a/webm)
                    callback.onSuccess(audioStreams.get(0).getUrl());
                } else {
                    callback.onError(new Exception("Nenhum stream de áudio encontrado"));
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}
