package com.b8box.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

// Comunidade temática (por gênero, década ou artista) que reúne os fóruns.
@Entity
@Table(name = "communities")
public class Community {

    public enum Category { GENRE, DECADE, ARTIST }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    // Quem criou. Fica vazio se a pessoa excluir a conta: a comunidade continua.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    @JsonIgnore
    private User creator;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Community() {}

    public Community(String name, String description, Category category, User creator) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.creator = creator;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
