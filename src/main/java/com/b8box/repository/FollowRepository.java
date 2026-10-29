package com.b8box.repository;

import com.b8box.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerIdAndFollowedId(Long followerId, Long followedId);
    boolean existsByFollowerIdAndFollowedId(Long followerId, Long followedId);
    long countByFollowedId(Long followedId);   // nº de seguidores de um usuário
    long countByFollowerId(Long followerId);   // nº de pessoas que um usuário segue
    void deleteByFollowerIdAndFollowedId(Long followerId, Long followedId);
    void deleteByFollowerId(Long followerId);   // usados ao apagar uma conta
    void deleteByFollowedId(Long followedId);
    List<Follow> findByFollowedId(Long followedId);   // quem segue o usuário X
    List<Follow> findByFollowerId(Long followerId);   // quem o usuário X segue

    // Só os ids de quem o usuário segue (usado pelo feed, sem carregar os usuários)
    @Query("select f.followed.id from Follow f where f.follower.id = :userId")
    List<Long> findFollowedIds(@Param("userId") Long userId);
}