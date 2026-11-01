package com.b8box.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;
 
    @Column(nullable = false)
    @JsonIgnore // o hash da senha nunca deve sair em JSON
    private String password;

    // Bio curta exibida no perfil (opcional)
    @Column(length = 300)
    private String bio;

    // Gêneros favoritos (até 5). Ficam numa tabela própria
    // (user_favorite_genres: user_id, position, genre) em vez de uma lista
    // separada por vírgulas numa coluna só — assim o banco continua
    // normalizado (1FN) e a ordem escolhida pelo usuário é preservada.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_favorite_genres", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "position")
    @Column(name = "genre", length = 30, nullable = false)
    private List<String> favoriteGenres = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Rating> ratings = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Playlist> playlists = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Quando a foto de perfil foi trocada pela última vez (null = sem foto).
    // A imagem em si fica na tabela user_avatars; aqui guardamos só a data,
    // que serve para montar a URL da foto sem precisar ler os bytes.
    @Column(name = "avatar_updated_at")
    private LocalDateTime avatarUpdatedAt;

    // Construtor vazio (OBRIGATÓRIO para JPA)
    public User() {}

    // Construtor com campos principais
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // ========== GETTERS E SETTERS ==========
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public List<String> getFavoriteGenres() {
        return favoriteGenres;
    }

    // Altera a coleção que o Hibernate já gerencia (clear + addAll) em vez
    // de trocar a referência da lista.
    public void setFavoriteGenres(List<String> genres) {
        this.favoriteGenres.clear();
        if (genres != null) {
            this.favoriteGenres.addAll(genres);
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getAvatarUpdatedAt() {
        return avatarUpdatedAt;
    }

    public void setAvatarUpdatedAt(LocalDateTime avatarUpdatedAt) {
        this.avatarUpdatedAt = avatarUpdatedAt;
    }
}