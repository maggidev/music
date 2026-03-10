package maggi.privacy;

public class Musica {
    private String videoId;
    private String titulo;
    private String artista;
    private String capaUrl;

    public Musica(String videoId, String titulo, String artista, String capaUrl) {
        this.videoId = videoId;
        this.titulo = titulo;
        this.artista = artista;
        this.capaUrl = capaUrl;
    }

    // Getters
    public String getVideoId() { return videoId; }
    public String getTitulo() { return titulo; }
    public String getArtista() { return artista; }
    public String getCapaUrl() { return capaUrl; }
}
