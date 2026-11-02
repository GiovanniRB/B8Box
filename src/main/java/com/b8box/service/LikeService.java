package com.b8box.service;

import com.b8box.model.Rating;
import com.b8box.model.RatingLike;
import com.b8box.model.User;
import com.b8box.repository.RatingLikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Regras das curtidas em avaliações.
 * - Curtir e descurtir são idempotentes: repetir a ação não dá erro.
 * - Ninguém curte a própria avaliação (checado no controller).
 */
@Service
public class LikeService {

    @Autowired
    private RatingLikeRepository ratingLikeRepository;

    @Autowired
    private NotificationService notificationService;

    public void like(User user, Rating rating) {
        if (ratingLikeRepository.existsByUserIdAndRatingId(user.getId(), rating.getId())) {
            return;
        }
        try {
            ratingLikeRepository.save(new RatingLike(user, rating));
            notificationService.onLike(user, rating);
        } catch (DataIntegrityViolationException e) {
            // dois cliques ao mesmo tempo: a restrição única barrou o segundo,
            // e a curtida já existe — nada a fazer
        }
    }

    public void unlike(Long userId, Long ratingId) {
        ratingLikeRepository.findByUserIdAndRatingId(userId, ratingId).ifPresent(like -> {
            ratingLikeRepository.delete(like);
            notificationService.onUnlike(userId, ratingId);
        });
    }

    /** Estado atual de uma avaliação para quem está vendo: { likesCount, likedByMe }. */
    public Map<String, Object> status(Long ratingId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("likesCount", ratingLikeRepository.countByRatingId(ratingId));
        result.put("likedByMe", ratingLikeRepository.existsByUserIdAndRatingId(userId, ratingId));
        return result;
    }

    /** Curtidas de várias avaliações de uma vez (ratingId -> quantidade). */
    public Map<Long, Long> countFor(Collection<Long> ratingIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (ratingIds == null || ratingIds.isEmpty()) return counts;
        for (Object[] row : ratingLikeRepository.countByRatingIds(ratingIds)) {
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    /** Quais dessas avaliações o usuário curtiu. */
    public Set<Long> likedBy(Long userId, Collection<Long> ratingIds) {
        if (ratingIds == null || ratingIds.isEmpty()) return new HashSet<>();
        return new HashSet<>(ratingLikeRepository.findLikedRatingIds(userId, ratingIds));
    }
}
