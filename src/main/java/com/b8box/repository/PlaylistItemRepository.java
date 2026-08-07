package com.b8box.repository;

import com.b8box.model.PlaylistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, Long> {
    List<PlaylistItem> findByPlaylistId(Long playlistId);
    List<PlaylistItem> findByMusicId(Long musicId);
    void deleteByPlaylistId(Long playlistId);
}