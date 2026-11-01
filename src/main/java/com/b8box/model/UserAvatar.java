package com.b8box.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Foto de perfil de um usuário (no máximo uma por pessoa).
// Fica numa tabela separada de "users" de propósito: assim os bytes da imagem
// só são lidos do banco quando alguém pede a foto, e não em toda consulta de
// usuário (login, feed, busca...).
// A imagem guardada já vem tratada pelo AvatarService: JPEG quadrado 256x256,
// sem metadados (EXIF/GPS) da foto original.
@Entity
@Table(name = "user_avatars")
public class UserAvatar {

    // Mesmo id do usuário dono da foto (chave 1:1)
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "image", nullable = false)
    private byte[] image;

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserAvatar() {}

    public UserAvatar(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public byte[] getImage() { return image; }
    public void setImage(byte[] image) { this.image = image; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
