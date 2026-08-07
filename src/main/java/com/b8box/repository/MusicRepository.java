package com.b8box.repository;

import com.b8box.model.Music;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MusicRepository extends JpaRepository<Music, Long> {
    List<Music> findByAlbumId(Long albumId);
    List<Music> findByTitleContainingIgnoreCase(String title);
}