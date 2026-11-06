package com.b8box.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Reputação de um usuário: pontos, nível e badges. Nada disso fica guardado;
 * o ReputationService calcula tudo a partir da atividade da pessoa.
 */
public class ReputationDTO {

    private long points;
    private String level;
    private long levelStart;
    private String nextLevel;       // null no nível mais alto
    private Long nextLevelAt;       // null no nível mais alto
    private List<Source> sources = new ArrayList<>();
    private List<Badge> badges = new ArrayList<>();

    /** De onde vieram os pontos: a tela usa isso para explicar a conta. */
    public static class Source {
        private final String label;
        private final long count;
        private final int pointsEach;

        public Source(String label, long count, int pointsEach) {
            this.label = label;
            this.count = count;
            this.pointsEach = pointsEach;
        }

        public String getLabel() { return label; }
        public long getCount() { return count; }
        public int getPointsEach() { return pointsEach; }
        public long getPoints() { return count * pointsEach; }
    }

    public static class Badge {
        private final String key;
        private final String name;
        private final String description;
        private final long progress;
        private final long goal;

        public Badge(String key, String name, String description, long count, long goal) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.progress = Math.min(count, goal);
            this.goal = goal;
        }

        public String getKey() { return key; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public long getProgress() { return progress; }
        public long getGoal() { return goal; }
        public boolean isEarned() { return progress >= goal; }
    }

    public long getPoints() { return points; }
    public void setPoints(long points) { this.points = points; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public long getLevelStart() { return levelStart; }
    public void setLevelStart(long levelStart) { this.levelStart = levelStart; }

    public String getNextLevel() { return nextLevel; }
    public void setNextLevel(String nextLevel) { this.nextLevel = nextLevel; }

    public Long getNextLevelAt() { return nextLevelAt; }
    public void setNextLevelAt(Long nextLevelAt) { this.nextLevelAt = nextLevelAt; }

    public List<Source> getSources() { return sources; }
    public void setSources(List<Source> sources) { this.sources = sources; }

    public List<Badge> getBadges() { return badges; }
    public void setBadges(List<Badge> badges) { this.badges = badges; }
}
