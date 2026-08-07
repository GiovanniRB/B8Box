package com.b8box.repository;

import com.b8box.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByUserId(Long userId);
    List<Rating> findByAlbumId(Long albumId);
    List<Rating> findByMusicId(Long musicId);
    List<Rating> findByUserIdAndAlbumId(Long userId, Long albumId);
    List<Rating> findByUserIdAndMusicId(Long userId, Long musicId);
}