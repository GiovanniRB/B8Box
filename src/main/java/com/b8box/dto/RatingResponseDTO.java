package com.b8box.dto;

public class RatingResponseDTO {
    private Long id;
    private Double score;
    private String review;
    private String username;
    private String createdAt;
    
    public RatingResponseDTO() {}
    
	public RatingResponseDTO(Long id, Double score, String review, String username, String createdAt) {
		super();
		this.id = id;
		this.score = score;
		this.review = review;
		this.username = username;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public String getReview() {
		return review;
	}

	public void setReview(String review) {
		this.review = review;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

}