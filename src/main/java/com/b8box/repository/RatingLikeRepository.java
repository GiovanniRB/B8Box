package com.b8box.repository;

import com.b8box.model.RatingLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatingLikeRepository extends JpaRepository<RatingLike, Long> {

    Optional<RatingLike> findByUserIdAndRatingId(Long userId, Long ratingId);

    boolean existsByUserIdAndRatingId(Long userId, Long ratingId);

    long countByRatingId(Long ratingId);

    // Usado ao apagar uma conta (precisa de transação ativa no chamador)
    void deleteByUserId(Long userId);

    // Quantidade de curtidas de VÁRIAS avaliações em uma consulta só.
    // Cada linha: [ratingId (Long), quantidade (Long)].
    @Query("select l.rating.id, count(l) from RatingLike l where l.rating.id in :ratingIds group by l.rating.id")
    List<Object[]> countByRatingIds(@Param("ratingIds") Collection<Long> ratingIds);

    // Home: avaliações de álbuns feitas desde uma data, das mais curtidas
    // para as menos (só as que têm pelo menos 1 curtida).
    // Cada linha: [ratingId (Long), quantidade (Long)].
    @Query("select l.rating.id, count(l) from RatingLike l " +
           "where l.rating.album is not null and l.rating.createdAt >= :since " +
           "group by l.rating.id order by count(l) desc")
    List<Object[]> findMostLikedRatingsSince(@Param("since") LocalDateTime since, Pageable pageable);

    // Quais dessas avaliações o usuário já curtiu
    @Query("select l.rating.id from RatingLike l where l.user.id = :userId and l.rating.id in :ratingIds")
    List<Long> findLikedRatingIds(@Param("userId") Long userId, @Param("ratingIds") Collection<Long> ratingIds);
}
