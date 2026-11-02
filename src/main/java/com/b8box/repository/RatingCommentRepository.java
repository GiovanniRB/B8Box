package com.b8box.repository;

import com.b8box.model.RatingComment;
import com.b8box.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RatingCommentRepository extends JpaRepository<RatingComment, Long> {

    // Comentários de uma avaliação, do mais antigo para o mais novo
    // (o autor vem junto, para não buscar um usuário por comentário).
    @Query("select c from RatingComment c join fetch c.user where c.rating.id = :ratingId order by c.createdAt asc, c.id asc")
    List<RatingComment> findByRatingIdWithUser(@Param("ratingId") Long ratingId);

    // Quantidade de comentários de VÁRIAS avaliações em uma consulta só.
    // Cada linha: [ratingId (Long), quantidade (Long)].
    @Query("select c.rating.id, count(c) from RatingComment c where c.rating.id in :ratingIds group by c.rating.id")
    List<Object[]> countByRatingIds(@Param("ratingIds") Collection<Long> ratingIds);

    // Quem comentou numa avaliação, sem repetir e sem contar o dono dela
    // (são avisados quando o dono responde).
    @Query("select distinct c.user from RatingComment c where c.rating.id = :ratingId and c.user.id <> :ownerId")
    List<User> findOtherCommenters(@Param("ratingId") Long ratingId, @Param("ownerId") Long ownerId);

    // Usado ao apagar uma conta (precisa de transação ativa no chamador)
    void deleteByUserId(Long userId);
}
