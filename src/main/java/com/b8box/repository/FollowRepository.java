package com.b8box.repository;

import com.b8box.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerIdAndFollowedId(Long followerId, Long followedId);
    boolean existsByFollowerIdAndFollowedId(Long followerId, Long followedId);
    long countByFollowedId(Long followedId);   // nº de seguidores de um usuário
    long countByFollowerId(Long followerId);   // nº de pessoas que um usuário segue
    void deleteByFollowerIdAndFollowedId(Long followerId, Long followedId);
}