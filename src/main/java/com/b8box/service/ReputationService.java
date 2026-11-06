package com.b8box.service;

import com.b8box.dto.ReputationDTO;
import com.b8box.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Reputação (RF35 e RF36). Os pontos saem da contagem do que a pessoa tem hoje
 * no site, sem tabela própria: vale para quem já tinha atividade, e apagar uma
 * crítica ou perder uma curtida reduz os pontos sem nenhum passo a mais.
 */
@Service
public class ReputationService {

    private static final int POINTS_REVIEW = 10;
    private static final int POINTS_SCORE_ONLY = 2;
    private static final int POINTS_LIKE_RECEIVED = 5;
    private static final int POINTS_THREAD = 5;
    private static final int POINTS_REPLY = 2;

    private record Level(String name, long start) {}

    // Do menor para o maior
    private static final List<Level> LEVELS = List.of(
            new Level("Ouvinte", 0),
            new Level("Crítico", 60),
            new Level("Curador", 180),
            new Level("Especialista", 500));

    @Autowired
    private UserRepository userRepository;

    public ReputationDTO calculate(Long userId) {
        UserRepository.ReputationStats stats = userRepository.reputationStats(userId);

        List<ReputationDTO.Source> sources = List.of(
                new ReputationDTO.Source("Críticas escritas", stats.getReviewsCount(), POINTS_REVIEW),
                new ReputationDTO.Source("Avaliações só com nota", stats.getScoreOnlyCount(), POINTS_SCORE_ONLY),
                new ReputationDTO.Source("Curtidas recebidas", stats.getLikesReceivedCount(), POINTS_LIKE_RECEIVED),
                new ReputationDTO.Source("Discussões abertas", stats.getThreadsCount(), POINTS_THREAD),
                new ReputationDTO.Source("Respostas em discussões", stats.getRepliesCount(), POINTS_REPLY));
        long points = sources.stream().mapToLong(ReputationDTO.Source::getPoints).sum();

        ReputationDTO dto = new ReputationDTO();
        dto.setPoints(points);
        dto.setSources(sources);
        dto.setBadges(badges(stats));
        applyLevel(dto, points);
        return dto;
    }

    private void applyLevel(ReputationDTO dto, long points) {
        for (int i = 0; i < LEVELS.size(); i++) {
            Level level = LEVELS.get(i);
            Level next = i + 1 < LEVELS.size() ? LEVELS.get(i + 1) : null;
            if (next == null || points < next.start()) {
                dto.setLevel(level.name());
                dto.setLevelStart(level.start());
                dto.setNextLevel(next != null ? next.name() : null);
                dto.setNextLevelAt(next != null ? next.start() : null);
                return;
            }
        }
    }

    private List<ReputationDTO.Badge> badges(UserRepository.ReputationStats stats) {
        return List.of(
                new ReputationDTO.Badge("first-review", "Primeira Crítica",
                        "Escreveu a primeira crítica", stats.getReviewsCount(), 1),
                new ReputationDTO.Badge("dedicated-critic", "Crítico Dedicado",
                        "Escreveu 10 críticas", stats.getReviewsCount(), 10),
                new ReputationDTO.Badge("explorer", "Explorador",
                        "Avaliou 25 álbuns", stats.getAlbumsCount(), 25),
                new ReputationDTO.Badge("community-favorite", "Querido pela Comunidade",
                        "Recebeu 25 curtidas", stats.getLikesReceivedCount(), 25),
                new ReputationDTO.Badge("list-curator", "Curador de Listas",
                        "Criou 3 playlists públicas", stats.getPublicPlaylistsCount(), 3),
                new ReputationDTO.Badge("debater", "Debatedor",
                        "Escreveu 10 respostas em discussões", stats.getRepliesCount(), 10),
                new ReputationDTO.Badge("founder", "Fundador",
                        "Criou uma comunidade", stats.getCommunitiesCreatedCount(), 1),
                new ReputationDTO.Badge("influential", "Influente",
                        "Tem 10 seguidores", stats.getFollowersCount(), 10));
    }
}
