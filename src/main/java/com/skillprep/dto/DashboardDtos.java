package com.skillprep.dto;

import java.util.List;
import java.util.Map;

public class DashboardDtos {

    public record DashboardResponse(
            Double overallAverageScore,
            int totalAnswered,
            Map<String, Double> scoreByQuestionType,
            List<SkillBreakdown> scoreBySkill,
            Map<String, Double> scoreByExperienceLevel,
            List<Double> recentScoreTrend,
            List<RecentSessionSummary> recentSessions
    ) {}

    public record SkillBreakdown(
            String skillTopic,
            double averageScore,
            int attemptCount,
            boolean weakArea
    ) {}

    public record RecentSessionSummary(
            String sessionId,
            String createdAt,
            int questionCount,
            Double averageScore,
            String difficulty,
            Integer experienceLevel
    ) {}
}
