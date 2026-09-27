package com.b8box.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "follows", uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "followed_id"}))
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quem está seguindo
    @ManyToOne
    @JoinColumn(name = "follower_id", nullable = false)
    @JsonIgnore
    private User follower;

    // Quem é seguido
    @ManyToOne
    @JoinColumn(name = "followed_id", nullable = false)
    @JsonIgnore
    private User followed;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Follow() {}

    public Follow(User follower, User followed) {
        this.follower = follower;
        this.followed = followed;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getFollower() { return follower; }
    public void setFollower(User follower) { this.follower = follower; }

    public User getFollowed() { return followed; }
    public void setFollowed(User followed) { this.followed = followed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}