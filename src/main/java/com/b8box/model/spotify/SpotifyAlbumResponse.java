/*package com.b8box.model.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SpotifyAlbumResponse {

    @JsonProperty("albums")
    private Albums albums;

    public Albums getAlbums() { return albums; }
    public void setAlbums(Albums albums) { this.albums = albums; }

    public static class Albums {
        @JsonProperty("items")
        private List<AlbumItem> items;

        public List<AlbumItem> getItems() { return items; }
        public void setItems(List<AlbumItem> items) { this.items = items; }
    }

    public static class AlbumItem {
        private String id;
        private String name;

        @JsonProperty("release_date")
        private String releaseDate;

        @JsonProperty("total_tracks")
        private int totalTracks;

        @JsonProperty("artists")
        private List<Artist> artists;

        @JsonProperty("images")
        private List<Image> images;

        @JsonProperty("tracks")
        private Tracks tracks;

        // Getters e Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getReleaseDate() { return releaseDate; }
        public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

        public int getTotalTracks() { return totalTracks; }
        public void setTotalTracks(int totalTracks) { this.totalTracks = totalTracks; }

        public List<Artist> getArtists() { return artists; }
        public void setArtists(List<Artist> artists) { this.artists = artists; }

        public List<Image> getImages() { return images; }
        public void setImages(List<Image> images) { this.images = images; }

        public Tracks getTracks() { return tracks; }
        public void setTracks(Tracks tracks) { this.tracks = tracks; }
    }

    public static class Artist {
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class Image {
        private String url;
        private int height;
        private int width;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }

        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
    }

    public static class Tracks {
        @JsonProperty("items")
        private List<TrackItem> items;

        public List<TrackItem> getItems() { return items; }
        public void setItems(List<TrackItem> items) { this.items = items; }
    }

    public static class TrackItem {
        private String name;
        @JsonProperty("track_number")
        private int trackNumber;
        @JsonProperty("duration_ms")
        private int durationMs;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getTrackNumber() { return trackNumber; }
        public void setTrackNumber(int trackNumber) { this.trackNumber = trackNumber; }

        public int getDurationMs() { return durationMs; }
        public void setDurationMs(int durationMs) { this.durationMs = durationMs; }
    }
}*/