package com.b8box.dto;

public class UserPublicProfileDTO {
    private Long id;
    private String username;
    private String createdAt;
    private long albumsCount;
    private long publicPlaylistsCount;
    private long followersCount;
    private long followingCount;
    private boolean followingByMe;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public long getAlbumsCount() { return albumsCount; }
    public void setAlbumsCount(long albumsCount) { this.albumsCount = albumsCount; }

    public long getPublicPlaylistsCount() { return publicPlaylistsCount; }
    public void setPublicPlaylistsCount(long publicPlaylistsCount) { this.publicPlaylistsCount = publicPlaylistsCount; }

    public long getFollowersCount() { return followersCount; }
    public void setFollowersCount(long followersCount) { this.followersCount = followersCount; }

    public long getFollowingCount() { return followingCount; }
    public void setFollowingCount(long followingCount) { this.followingCount = followingCount; }

    public boolean isFollowingByMe() { return followingByMe; }
    public void setFollowingByMe(boolean followingByMe) { this.followingByMe = followingByMe; }
}