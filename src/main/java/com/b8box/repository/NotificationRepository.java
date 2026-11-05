package com.b8box.repository;

import com.b8box.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Avisos de um usuário, dos mais novos para os mais antigos
    // (quem fez a ação, a avaliação e o álbum vêm juntos na mesma consulta).
    @Query("select n from Notification n join fetch n.actor left join fetch n.rating r left join fetch r.album " +
           "where n.recipient.id = :userId order by n.createdAt desc, n.id desc")
    List<Notification> findLatest(@Param("userId") Long userId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    @Modifying
    @Query("update Notification n set n.read = true where n.recipient.id = :userId and n.read = false")
    int markAllRead(@Param("userId") Long userId);

    // Desfazer a ação apaga o aviso (precisam de transação ativa no chamador)
    void deleteByActorIdAndRecipientIdAndType(Long actorId, Long recipientId, Notification.Type type);
    void deleteByActorIdAndRatingIdAndType(Long actorId, Long ratingId, Notification.Type type);
    void deleteByCommentId(Long commentId);
    void deleteByReplyId(Long replyId);
    void deleteByThreadId(Long threadId);

    @Modifying
    @Query("delete from Notification n where n.threadId in " +
           "(select t.id from ForumThread t where t.community.id = :communityId)")
    void deleteByCommunityId(@Param("communityId") Long communityId);

    // Usado ao apagar uma conta: avisos que a pessoa recebeu ou gerou
    void deleteByRecipientIdOrActorId(Long recipientId, Long actorId);
}
