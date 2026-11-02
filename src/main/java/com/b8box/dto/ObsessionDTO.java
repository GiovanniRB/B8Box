package com.b8box.dto;

import com.b8box.model.Album;
import com.b8box.model.User;

import java.time.LocalDateTime;

/**
 * "Obsessão do momento": o álbum que o usuário destacou no perfil.
 * Vale por 7 dias a partir da escolha; depois disso deixa de aparecer
 * (não é preciso apagar nada — a data já diz que expirou).
 */
public class ObsessionDTO {

    public static final int DAYS = 7;

    private Long albumId;
    private String title;
    private String artist;
    private String coverUrl;
    private String setAt;
    private String expiresAt;

    /** A obsessão atual do usuário, ou null se não houver (ou já tiver expirado). */
    public static ObsessionDTO of(User user) {
        Album album = user.getObsessionAlbum();
        LocalDateTime setAt = user.getObsessionSetAt();
        if (album == null || setAt == null) return null;
        LocalDateTime expiresAt = setAt.plusDays(DAYS);
        if (!expiresAt.isAfter(LocalDateTime.now())) return null;

        ObsessionDTO dto = new ObsessionDTO();
        dto.albumId = album.getId();
        dto.title = album.getTitle();
        dto.artist = album.getArtist();
        dto.coverUrl = album.getCoverUrl();
        // sem frações de segundo: o JavaScript lê a data sem surpresas
        dto.setAt = setAt.withNano(0).toString();
        dto.expiresAt = expiresAt.withNano(0).toString();
        return dto;
    }

    public Long getAlbumId() { return albumId; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getCoverUrl() { return coverUrl; }
    public String getSetAt() { return setAt; }
    public String getExpiresAt() { return expiresAt; }
}
