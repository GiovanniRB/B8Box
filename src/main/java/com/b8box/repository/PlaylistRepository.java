package com.b8box.repository;

import com.b8box.model.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByUserId(Long userId);
    List<Playlist> findByIsPublicTrue();
    List<Playlist> findByUserIdAndIsPublicTrue(Long userId);

    // Feed: playlists PÚBLICAS criadas por quem eu sigo, mais recentes primeiro.
    List<Playlist> findTop20ByUserIdInAndIsPublicTrueOrderByCreatedAtDesc(Collection<Long> userIds);
}