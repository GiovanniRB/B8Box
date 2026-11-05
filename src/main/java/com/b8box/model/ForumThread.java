package com.b8box.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

// Discussão aberta dentro de uma comunidade.
@Entity
@Table(name = "forum_threads", indexes = @Index(name = "idx_forum_threads_community", columnList = "community_id"))
public class ForumThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    @JsonIgnore
    private Community community;

    // Quem abriu. Fica vazio se a pessoa excluir a conta: a conversa continua.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @JsonIgnore
    private User author;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // Fixada por um moderador: fica no topo da lista
    @Column(nullable = false)
    private boolean pinned = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Última resposta (ou a criação): ordena a lista de discussões
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt = LocalDateTime.now();

    public ForumThread() {}

    public ForumThread(Community community, User author, String title, String content) {
        this.community = community;
        this.author = author;
        this.title = title;
        this.content = content;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Community getCommunity() { return community; }
    public void setCommunity(Community community) { this.community = community; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
}
