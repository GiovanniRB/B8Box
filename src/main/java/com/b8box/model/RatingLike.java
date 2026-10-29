package com.b8box.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

// Curtida de um usuário em uma avaliação (crítica) de outro usuário.
// A restrição única impede que a mesma pessoa curta a mesma avaliação duas
// vezes, mesmo com dois cliques simultâneos.
@Entity
@Table(name = "rating_likes", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "rating_id"}))
public class RatingLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quem curtiu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // Avaliação curtida
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rating_id", nullable = false)
    @JsonIgnore
    private Rating rating;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public RatingLike() {}

    public RatingLike(User user, Rating rating) {
        this.user = user;
        this.rating = rating;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Rating getRating() { return rating; }
    public void setRating(Rating rating) { this.rating = rating; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
