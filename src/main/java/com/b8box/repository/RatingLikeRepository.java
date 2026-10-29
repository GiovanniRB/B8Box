package com.b8box.repository;

import com.b8box.model.RatingLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    // Quais dessas avaliações o usuário já curtiu
    @Query("select l.rating.id from RatingLike l where l.user.id = :userId and l.rating.id in :ratingIds")
    List<Long> findLikedRatingIds(@Param("userId") Long userId, @Param("ratingIds") Collection<Long> ratingIds);
}
