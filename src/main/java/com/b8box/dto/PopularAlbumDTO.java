package com.b8box.dto;

import com.b8box.model.Album;

/**
 * Álbum da seção "Populares" da home: dados básicos + quantas avaliações
 * recebeu no período e a média delas.
 */
public class PopularAlbumDTO {

    private Long id;
    private String title;
    private String artist;
    private String coverUrl;
    private Integer releaseYear;
    private long ratingsCount;
    private Double averageRating;

    public PopularAlbumDTO(Album a, long ratingsCount, Double averageRating) {
        this.id = a.getId();
        this.title = a.getTitle();
        this.artist = a.getArtist();
        this.coverUrl = a.getCoverUrl();
        this.releaseYear = a.getReleaseYear();
        this.ratingsCount = ratingsCount;
        this.averageRating = averageRating;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getCoverUrl() { return coverUrl; }
    public Integer getReleaseYear() { return releaseYear; }
    public long getRatingsCount() { return ratingsCount; }
    public Double getAverageRating() { return averageRating; }
}
