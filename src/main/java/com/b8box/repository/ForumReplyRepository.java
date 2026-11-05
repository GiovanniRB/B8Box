package com.b8box.repository;

import com.b8box.model.ForumReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumReplyRepository extends JpaRepository<ForumReply, Long> {

    // Lista plana, da mais antiga para a mais nova; a tela monta a árvore pelo parentId
    @Query("SELECT r FROM ForumReply r LEFT JOIN FETCH r.author LEFT JOIN FETCH r.parent " +
           "WHERE r.thread.id = :threadId ORDER BY r.createdAt ASC, r.id ASC")
    List<ForumReply> findByThread(@Param("threadId") Long threadId);

    boolean existsByParentId(Long parentId);

    boolean existsByThreadId(Long threadId);

    List<ForumReply> findByAuthorIdOrderByIdDesc(Long authorId);

    long countByThreadIdAndRemovedFalse(Long threadId);

    // Pares [id da discussão, nº de respostas visíveis]
    @Query("SELECT r.thread.id, COUNT(r) FROM ForumReply r " +
           "WHERE r.thread.community.id = :communityId AND r.removed = false GROUP BY r.thread.id")
    List<Object[]> countByCommunity(@Param("communityId") Long communityId);

    // Um único DELETE: o PostgreSQL confere a chave parent_id só no fim do comando
    @Modifying
    @Query("DELETE FROM ForumReply r WHERE r.thread.id = :threadId")
    void deleteByThreadId(@Param("threadId") Long threadId);

    @Modifying
    @Query("DELETE FROM ForumReply r WHERE r.thread.id IN " +
           "(SELECT t.id FROM ForumThread t WHERE t.community.id = :communityId)")
    void deleteByCommunityId(@Param("communityId") Long communityId);
}
