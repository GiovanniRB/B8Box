package com.b8box.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

// Aviso para um usuário sobre algo que outra pessoa fez:
// FOLLOW  - passou a seguir você
// LIKE    - curtiu a sua avaliação
// COMMENT - comentou na sua avaliação
// REPLY   - o dono da avaliação respondeu onde você tinha comentado, ou
//           alguém respondeu você em uma discussão (aí thread_id vem preenchido)
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notifications_recipient", columnList = "recipient_id"))
public class Notification {

    public enum Type { FOLLOW, LIKE, COMMENT, REPLY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quem recebe o aviso
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    @JsonIgnore
    private User recipient;

    // Quem fez a ação
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    @JsonIgnore
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    // Avaliação envolvida (vazio em FOLLOW)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rating_id")
    @JsonIgnore
    private Rating rating;

    // Comentário que gerou o aviso (COMMENT/REPLY). Só o id, sem chave
    // estrangeira: apagar o comentário apaga o aviso pelo serviço.
    @Column(name = "comment_id")
    private Long commentId;

    // Discussão e resposta do fórum que geraram o aviso. Só os ids, pelo mesmo motivo.
    @Column(name = "thread_id")
    private Long threadId;

    @Column(name = "reply_id")
    private Long replyId;

    // Começo do comentário, para mostrar na lista sem buscar o comentário
    @Column(length = 140)
    private String preview;

    // "read" é palavra reservada em alguns bancos
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {}

    public Notification(User recipient, User actor, Type type, Rating rating) {
        this.recipient = recipient;
        this.actor = actor;
        this.type = type;
        this.rating = rating;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }

    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public Rating getRating() { return rating; }
    public void setRating(Rating rating) { this.rating = rating; }

    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }

    public Long getThreadId() { return threadId; }
    public void setThreadId(Long threadId) { this.threadId = threadId; }

    public Long getReplyId() { return replyId; }
    public void setReplyId(Long replyId) { this.replyId = replyId; }

    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
