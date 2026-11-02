package com.b8box.dto;

import com.b8box.model.Album;
import com.b8box.model.Playlist;
import com.b8box.model.Rating;
import com.b8box.service.AvatarService;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * Um item do feed (ou da lista de avaliações de um perfil).
 * type = "rating"   -> alguém avaliou um álbum (campos rating* e album*)
 * type = "playlist" -> alguém criou uma playlist pública (campos playlist*)
 */
public class FeedItemDTO {

    private String type;
    private String createdAt;

    // quem fez
    private Long userId;
    private String username;
    private String avatarUrl;

    // avaliação
    private Long ratingId;
    private Double score;
    private String review;
    private long likesCount;
    private boolean likedByMe;
    private long commentsCount;

    // álbum avaliado
    private Long albumId;
    private String albumTitle;
    private String albumArtist;
    private String albumCoverUrl;
    private Integer albumReleaseYear;

    // playlist criada
    private Long playlistId;
    private String playlistTitle;
    private String playlistDescription;

    // usado só para ordenar no servidor; não vai no JSON
    @JsonIgnore
    private LocalDateTime sortDate;

    public static FeedItemDTO fromRating(Rating r) {
        FeedItemDTO dto = new FeedItemDTO();
        dto.type = "rating";
        dto.sortDate = r.getCreatedAt();
        dto.createdAt = r.getCreatedAt() != null ? r.getCreatedAt().toString() : null;
        dto.userId = r.getUser().getId();
        dto.username = r.getUser().getUsername();
        dto.avatarUrl = AvatarService.avatarUrl(r.getUser());
        dto.ratingId = r.getId();
        dto.score = r.getScore();
        dto.review = r.getReview();
        Album a = r.getAlbum();
        if (a != null) {
            dto.albumId = a.getId();
            dto.albumTitle = a.getTitle();
            dto.albumArtist = a.getArtist();
            dto.albumCoverUrl = a.getCoverUrl();
            dto.albumReleaseYear = a.getReleaseYear();
        }
        return dto;
    }

    public static FeedItemDTO fromPlaylist(Playlist p) {
        FeedItemDTO dto = new FeedItemDTO();
        dto.type = "playlist";
        dto.sortDate = p.getCreatedAt();
        dto.createdAt = p.getCreatedAt() != null ? p.getCreatedAt().toString() : null;
        dto.userId = p.getUser().getId();
        dto.username = p.getUser().getUsername();
        dto.avatarUrl = AvatarService.avatarUrl(p.getUser());
        dto.playlistId = p.getId();
        dto.playlistTitle = p.getTitle();
        dto.playlistDescription = p.getDescription();
        return dto;
    }

    public String getType() { return type; }
    public String getCreatedAt() { return createdAt; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getAvatarUrl() { return avatarUrl; }
    public Long getRatingId() { return ratingId; }
    public Double getScore() { return score; }
    public String getReview() { return review; }
    public long getLikesCount() { return likesCount; }
    public void setLikesCount(long likesCount) { this.likesCount = likesCount; }
    public boolean isLikedByMe() { return likedByMe; }
    public void setLikedByMe(boolean likedByMe) { this.likedByMe = likedByMe; }
    public long getCommentsCount() { return commentsCount; }
    public void setCommentsCount(long commentsCount) { this.commentsCount = commentsCount; }
    public Long getAlbumId() { return albumId; }
    public String getAlbumTitle() { return albumTitle; }
    public String getAlbumArtist() { return albumArtist; }
    public String getAlbumCoverUrl() { return albumCoverUrl; }
    public Integer getAlbumReleaseYear() { return albumReleaseYear; }
    public Long getPlaylistId() { return playlistId; }
    public String getPlaylistTitle() { return playlistTitle; }
    public String getPlaylistDescription() { return playlistDescription; }
    public LocalDateTime getSortDate() { return sortDate; }
}
