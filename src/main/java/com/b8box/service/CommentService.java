package com.b8box.service;

import com.b8box.model.Rating;
import com.b8box.model.RatingComment;
import com.b8box.model.User;
import com.b8box.repository.RatingCommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Regras dos comentários em avaliações.
 * - Qualquer pessoa logada comenta (inclusive na própria avaliação, para responder).
 * - Apaga quem escreveu o comentário ou o dono da avaliação comentada.
 */
@Service
public class CommentService {

    @Autowired
    private RatingCommentRepository commentRepository;

    public List<Map<String, Object>> list(Long ratingId, User me) {
        return commentRepository.findByRatingIdWithUser(ratingId).stream()
                .map(c -> toMap(c, me))
                .collect(Collectors.toList());
    }

    public Map<String, Object> add(User user, Rating rating, String text) {
        RatingComment saved = commentRepository.save(new RatingComment(user, rating, text));
        return toMap(saved, user);
    }

    /** false = não existe; lança SecurityException se a pessoa não pode apagar. */
    public boolean delete(Long commentId, User me) {
        RatingComment c = commentRepository.findById(commentId).orElse(null);
        if (c == null) return false;
        if (!canDelete(c, me)) {
            throw new SecurityException("❌ Você não pode apagar este comentário.");
        }
        commentRepository.delete(c);
        return true;
    }

    /** Comentários de várias avaliações de uma vez (ratingId -> quantidade). */
    public Map<Long, Long> countFor(Collection<Long> ratingIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (ratingIds == null || ratingIds.isEmpty()) return counts;
        for (Object[] row : commentRepository.countByRatingIds(ratingIds)) {
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private boolean canDelete(RatingComment c, User me) {
        if (me == null) return false;
        return c.getUser().getId().equals(me.getId())
                || c.getRating().getUser().getId().equals(me.getId());
    }

    private Map<String, Object> toMap(RatingComment c, User me) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("text", c.getText());
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        m.put("userId", c.getUser().getId());
        m.put("username", c.getUser().getUsername());
        m.put("avatarUrl", AvatarService.avatarUrl(c.getUser()));
        m.put("canDelete", canDelete(c, me));
        return m;
    }
}
