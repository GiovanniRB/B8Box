package com.b8box.repository;

import com.b8box.model.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    List<Album> findByArtistContainingIgnoreCase(String artist);
    List<Album> findByTitleContainingIgnoreCase(String title);
    Optional<Album> findBySpotifyId(String spotifyId);
}