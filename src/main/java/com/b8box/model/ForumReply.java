package com.b8box.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

// Resposta em uma discussão. parent vazio = responde à discussão; preenchido =
// responde a outra resposta, o que forma as threads, sem limite de profundidade.
@Entity
@Table(name = "forum_replies", indexes = {
        @Index(name = "idx_forum_replies_thread", columnList = "thread_id"),
        @Index(name = "idx_forum_replies_parent", columnList = "parent_id")
})
public class ForumReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    @JsonIgnore
    private ForumThread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private ForumReply parent;

    // Quem respondeu. Fica vazio se a pessoa excluir a conta.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @JsonIgnore
    private User author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    // Apagada, mas com respostas abaixo: a linha fica para a conversa não quebrar
    @Column(nullable = false)
    private boolean removed = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public ForumReply() {}

    public ForumReply(ForumThread thread, ForumReply parent, User author, String text) {
        this.thread = thread;
        this.parent = parent;
        this.author = author;
        this.text = text;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ForumThread getThread() { return thread; }
    public void setThread(ForumThread thread) { this.thread = thread; }

    public ForumReply getParent() { return parent; }
    public void setParent(ForumReply parent) { this.parent = parent; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isRemoved() { return removed; }
    public void setRemoved(boolean removed) { this.removed = removed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
