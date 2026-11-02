package com.b8box.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

// Comentário (resposta) de um usuário em uma avaliação (crítica).
// Um nível só: comenta-se a crítica, não outro comentário.
@Entity
@Table(name = "rating_comments", indexes = @Index(name = "idx_rating_comments_rating", columnList = "rating_id"))
public class RatingComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quem comentou
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // Avaliação comentada
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rating_id", nullable = false)
    @JsonIgnore
    private Rating rating;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public RatingComment() {}

    public RatingComment(User user, Rating rating, String text) {
        this.user = user;
        this.rating = rating;
        this.text = text;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Rating getRating() { return rating; }
    public void setRating(Rating rating) { this.rating = rating; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
