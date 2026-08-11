package com.b8box.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "playlist_items")
public class PlaylistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer position;

    @ManyToOne
    @JoinColumn(name = "playlist_id", nullable = false)
    @JsonIgnore
    private Playlist playlist;

    @ManyToOne
    @JoinColumn(name = "music_id", nullable = false)
    private Music music;

    // Construtor vazio (OBRIGATÓRIO para JPA)
    public PlaylistItem() {}

    // Construtor com campos principais
    public PlaylistItem(Playlist playlist, Music music) {
        this.playlist = playlist;
        this.music = music;
    }

    public PlaylistItem(Integer position, Playlist playlist, Music music) {
        this.position = position;
        this.playlist = playlist;
        this.music = music;
    }

    // ========== GETTERS E SETTERS ==========
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    public Music getMusic() {
        return music;
    }

    public void setMusic(Music music) {
        this.music = music;
    }
}