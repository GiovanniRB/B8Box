package com.b8box.dto;

import java.util.List;

public class AlbumResponseDTO {
    private Long id;
    private String title;
    private String artist;
    private String coverUrl;
    private Integer releaseYear;
    private Double averageRating;
    private List<RatingResponseDTO> ratings;
    
    public AlbumResponseDTO() {}
    
	public AlbumResponseDTO(Long id, String title, String artist, String coverUrl, Integer releaseYear,
			Double averageRating, List<RatingResponseDTO> ratings) {
		super();
		this.id = id;
		this.title = title;
		this.artist = artist;
		this.coverUrl = coverUrl;
		this.releaseYear = releaseYear;
		this.averageRating = averageRating;
		this.ratings = ratings;
		
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getCoverUrl() {
		return coverUrl;
	}

	public void setCoverUrl(String coverUrl) {
		this.coverUrl = coverUrl;
	}

	public Integer getReleaseYear() {
		return releaseYear;
	}

	public void setReleaseYear(Integer releaseYear) {
		this.releaseYear = releaseYear;
	}

	public Double getAverageRating() {
		return averageRating;
	}

	public void setAverageRating(Double averageRating) {
		this.averageRating = averageRating;
	}

	public List<RatingResponseDTO> getRatings() {
		return ratings;
	}

	public void setRatings(List<RatingResponseDTO> ratings) {
		this.ratings = ratings;
	}   
    
}