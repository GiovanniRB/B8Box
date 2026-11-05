package com.b8box.repository;

import com.b8box.model.ForumThread;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumThreadRepository extends JpaRepository<ForumThread, Long> {

    // Fixadas primeiro, depois as com atividade mais recente
    @Query("SELECT t FROM ForumThread t LEFT JOIN FETCH t.author " +
           "WHERE t.community.id = :communityId " +
           "ORDER BY t.pinned DESC, t.lastActivityAt DESC")
    List<ForumThread> findLatest(@Param("communityId") Long communityId, Pageable pageable);

    @Query("SELECT t FROM ForumThread t JOIN FETCH t.community LEFT JOIN FETCH t.author WHERE t.id = :id")
    Optional<ForumThread> findWithDetails(@Param("id") Long id);

    // Pares [id da comunidade, nº de discussões]
    @Query("SELECT t.community.id, COUNT(t) FROM ForumThread t GROUP BY t.community.id")
    List<Object[]> countAllByCommunity();

    long countByCommunityId(Long communityId);

    List<ForumThread> findByAuthorId(Long authorId);

    @Modifying
    @Query("DELETE FROM ForumThread t WHERE t.community.id = :communityId")
    void deleteByCommunityId(@Param("communityId") Long communityId);
}
