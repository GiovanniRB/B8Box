package com.b8box.service;

import com.b8box.dto.FeedItemDTO;
import com.b8box.dto.PopularAlbumDTO;
import com.b8box.model.Album;
import com.b8box.model.Playlist;
import com.b8box.model.Rating;
import com.b8box.repository.AlbumRepository;
import com.b8box.repository.FollowRepository;
import com.b8box.repository.PlaylistRepository;
import com.b8box.repository.RatingLikeRepository;
import com.b8box.repository.RatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Monta o feed (atividade de quem o usuário segue), a lista de avaliações
 * de um perfil e as seções públicas da home.
 */
@Service
public class FeedService {

    public static final int FEED_LIMIT = 50;
    public static final int HOME_LIMIT = 20;
    public static final int POPULAR_ALBUMS_LIMIT = 12;
    public static final int POPULAR_DAYS = 30;
    private static final LocalDateTime ALL_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private RatingLikeRepository ratingLikeRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private LikeService likeService;

    @Autowired
    private CommentService commentService;

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

    // ============================================================
    // HOME PÚBLICA (meId pode ser null: visitante sem login)
    // ============================================================

    /** Últimas avaliações de álbuns de qualquer pessoa. */
    @Transactional(readOnly = true)
    public List<FeedItemDTO> getRecentReviews(Long meId) {
        return toRatingItems(ratingRepository.findTop20ByAlbumIsNotNullOrderByCreatedAtDesc(), meId);
    }

    /**
     * Avaliações mais curtidas: primeiro as dos últimos POPULAR_DAYS dias;
     * se não chegar a HOME_LIMIT, completa com as mais curtidas de sempre
     * (para a home não ficar vazia num site com pouco movimento).
     */
    @Transactional(readOnly = true)
    public List<FeedItemDTO> getPopularReviews(Long meId) {
        PageRequest top = PageRequest.of(0, HOME_LIMIT);
        List<Long> ids = recentFirst(
                ratingLikeRepository.findMostLikedRatingsSince(popularSince(), top),
                ratingLikeRepository.findMostLikedRatingsSince(ALL_TIME, top),
                HOME_LIMIT);

        // findAllById não garante a ordem: reordena pela ordem da consulta
        Map<Long, Rating> byId = ratingRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Rating::getId, r -> r));
        List<Rating> ordered = ids.stream().map(byId::get).filter(r -> r != null).collect(Collectors.toList());
        return toRatingItems(ordered, meId);
    }

    /**
     * Álbuns mais avaliados: primeiro os dos últimos POPULAR_DAYS dias, depois
     * completa com os mais avaliados de sempre. A quantidade e a média
     * mostradas são sempre as totais do álbum.
     */
    @Transactional(readOnly = true)
    public List<PopularAlbumDTO> getPopularAlbums() {
        PageRequest top = PageRequest.of(0, POPULAR_ALBUMS_LIMIT);
        List<Object[]> periodRows = ratingRepository.findMostRatedAlbumsSince(popularSince(), top);
        List<Object[]> allTimeRows = ratingRepository.findMostRatedAlbumsSince(ALL_TIME, top);
        List<Long> ids = recentFirst(periodRows, allTimeRows, POPULAR_ALBUMS_LIMIT);

        // totais por álbum (se um álbum do período não estiver no top de
        // sempre, fica com os números do período)
        Map<Long, Object[]> stats = new HashMap<>();
        for (Object[] row : periodRows) stats.put((Long) row[0], row);
        for (Object[] row : allTimeRows) stats.put((Long) row[0], row);

        Map<Long, Album> byId = albumRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Album::getId, a -> a));

        List<PopularAlbumDTO> result = new ArrayList<>();
        for (Long id : ids) {
            Album a = byId.get(id);
            if (a == null) continue;
            Object[] row = stats.get(id);
            long count = ((Number) row[1]).longValue();
            Double avg = row[2] != null ? ((Number) row[2]).doubleValue() : null;
            result.add(new PopularAlbumDTO(a, count, avg));
        }
        return result;
    }

    // Ids (coluna 0) do período primeiro, depois os de sempre que ainda não
    // apareceram, no máximo `limit`.
    private List<Long> recentFirst(List<Object[]> periodRows, List<Object[]> allTimeRows, int limit) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Object[] row : periodRows) ids.add((Long) row[0]);
        for (Object[] row : allTimeRows) {
            if (ids.size() >= limit) break;
            ids.add((Long) row[0]);
        }
        return ids.stream().limit(limit).collect(Collectors.toList());
    }

    private LocalDateTime popularSince() {
        return LocalDateTime.now().minusDays(POPULAR_DAYS);
    }

    // Converte avaliações em itens já com curtidas e comentários, usando três
    // consultas no total (em vez de três por avaliação). meId null = visitante.
    private List<FeedItemDTO> toRatingItems(List<Rating> ratings, Long meId) {
        List<Long> ids = ratings.stream().map(Rating::getId).collect(Collectors.toList());
        Map<Long, Long> counts = likeService.countFor(ids);
        Set<Long> liked = meId != null ? likeService.likedBy(meId, ids) : Set.of();
        Map<Long, Long> comments = commentService.countFor(ids);

        List<FeedItemDTO> items = new ArrayList<>();
        for (Rating r : ratings) {
            FeedItemDTO dto = FeedItemDTO.fromRating(r);
            dto.setLikesCount(counts.getOrDefault(r.getId(), 0L));
            dto.setLikedByMe(liked.contains(r.getId()));
            dto.setCommentsCount(comments.getOrDefault(r.getId(), 0L));
            items.add(dto);
        }
        return items;
    }
}
