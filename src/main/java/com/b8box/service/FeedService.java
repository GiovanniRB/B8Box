package com.b8box.service;

import com.b8box.dto.FeedItemDTO;
import com.b8box.model.Playlist;
import com.b8box.model.Rating;
import com.b8box.repository.FollowRepository;
import com.b8box.repository.PlaylistRepository;
import com.b8box.repository.RatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Monta o feed (atividade de quem o usuário segue) e a lista de avaliações
 * de um perfil.
 */
@Service
public class FeedService {

    public static final int FEED_LIMIT = 50;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private LikeService likeService;

    /**
     * Feed de quem o usuário segue: avaliações de álbuns + playlists públicas,
     * do mais recente para o mais antigo, no máximo FEED_LIMIT itens.
     */
    @Transactional(readOnly = true)
    public List<FeedItemDTO> getFeed(Long meId) {
        List<Long> followedIds = followRepository.findFollowedIds(meId);
        if (followedIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Rating> ratings = ratingRepository
                .findTop50ByUserIdInAndAlbumIsNotNullOrderByCreatedAtDesc(followedIds);
        List<Playlist> playlists = playlistRepository
                .findTop20ByUserIdInAndIsPublicTrueOrderByCreatedAtDesc(followedIds);

        List<FeedItemDTO> items = new ArrayList<>(toRatingItems(ratings, meId));
        for (Playlist p : playlists) {
            items.add(FeedItemDTO.fromPlaylist(p));
        }

        items.sort(Comparator.comparing(FeedItemDTO::getSortDate,
                Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())));
        return items.size() > FEED_LIMIT ? new ArrayList<>(items.subList(0, FEED_LIMIT)) : items;
    }

    /** Últimas avaliações de álbuns de um usuário (perfil público). */
    @Transactional(readOnly = true)
    public List<FeedItemDTO> getUserRatings(Long userId, Long meId) {
        return toRatingItems(
                ratingRepository.findTop30ByUserIdAndAlbumIsNotNullOrderByCreatedAtDesc(userId), meId);
    }

    // Converte avaliações em itens já com as curtidas, usando duas consultas
    // no total (em vez de duas por avaliação).
    private List<FeedItemDTO> toRatingItems(List<Rating> ratings, Long meId) {
        List<Long> ids = ratings.stream().map(Rating::getId).collect(Collectors.toList());
        Map<Long, Long> counts = likeService.countFor(ids);
        Set<Long> liked = likeService.likedBy(meId, ids);

        List<FeedItemDTO> items = new ArrayList<>();
        for (Rating r : ratings) {
            FeedItemDTO dto = FeedItemDTO.fromRating(r);
            dto.setLikesCount(counts.getOrDefault(r.getId(), 0L));
            dto.setLikedByMe(liked.contains(r.getId()));
            items.add(dto);
        }
        return items;
    }
}
