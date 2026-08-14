package com.skillprep.service;

import com.skillprep.dto.DashboardDtos.DashboardResponse;
import com.skillprep.dto.DashboardDtos.RecentSessionSummary;
import com.skillprep.dto.DashboardDtos.SkillBreakdown;
import com.skillprep.model.PracticeSession;
import com.skillprep.model.Question;
import com.skillprep.repository.PracticeSessionRepository;
import com.skillprep.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final double WEAK_AREA_THRESHOLD_BELOW_OVERALL = 10.0;

    private final QuestionRepository questionRepository;
    private final PracticeSessionRepository sessionRepository;

    public DashboardService(QuestionRepository questionRepository, PracticeSessionRepository sessionRepository) {
        this.questionRepository = questionRepository;
        this.sessionRepository = sessionRepository;
    }

    public DashboardResponse build(String userId) {
        List<Question> questions = questionRepository.findByUserId(userId);

        // Flatten to (question, attempt) pairs for per-attempt breakdowns
        record Scored(Question q, Question.Attempt a) {}
        List<Scored> scored = new ArrayList<>();
        for (Question q : questions) {
            for (Question.Attempt a : q.getAttempts()) {
                scored.add(new Scored(q, a));
            }
        }

        double overallAvg = scored.stream().mapToInt(s -> s.a().getScore()).average().orElse(0);

        Map<String, Double> byType = scored.stream()
                .collect(Collectors.groupingBy(s -> s.q().getQuestionType(),
                        Collectors.averagingInt(s -> s.a().getScore())));

        Map<String, List<Scored>> bySkillRaw = scored.stream()
                .collect(Collectors.groupingBy(s -> s.q().getSkillTopic()));
        List<SkillBreakdown> bySkill = bySkillRaw.entrySet().stream().map(e -> {
            double avg = e.getValue().stream().mapToInt(s -> s.a().getScore()).average().orElse(0);
            boolean weak = !scored.isEmpty() && avg < (overallAvg - WEAK_AREA_THRESHOLD_BELOW_OVERALL) && e.getValue().size() >= 1;
            return new SkillBreakdown(e.getKey(), round1(avg), e.getValue().size(), weak);
        }).sorted(Comparator.comparingDouble(SkillBreakdown::averageScore)).collect(Collectors.toList());

        Map<String, Double> byExperience = scored.stream()
                .filter(s -> s.a().getExperienceLevelUsed() != null)
                .collect(Collectors.groupingBy(s -> String.valueOf(s.a().getExperienceLevelUsed()),
                        Collectors.averagingInt(s -> s.a().getScore())));

        List<Double> trend = scored.stream()
                .sorted(Comparator.comparing(s -> s.a().getAnsweredAt()))
                .map(s -> (double) s.a().getScore())
                .collect(Collectors.toList());
        List<Double> recentTrend = trend.size() > 20 ? trend.subList(trend.size() - 20, trend.size()) : trend;

        List<PracticeSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;
        List<RecentSessionSummary> recentSessions = sessions.stream().limit(10).map(s -> new RecentSessionSummary(
                s.getId(), fmt.format(s.getCreatedAt()),
                s.getQuestionIds() == null ? 0 : s.getQuestionIds().size(),
                s.getAverageScore(), s.getDifficulty(), s.getExperienceLevel()
        )).collect(Collectors.toList());

        return new DashboardResponse(
                scored.isEmpty() ? null : round1(overallAvg),
                scored.size(),
                byType.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> round1(e.getValue()))),
                bySkill,
                byExperience.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> round1(e.getValue()))),
                recentTrend,
                recentSessions
        );
    }

    /** The skill/topic with the lowest average score, for the "practice my weakest skill" quick link. */
    public Optional<String> weakestSkill(String userId) {
        return build(userId).scoreBySkill().stream()
                .min(Comparator.comparingDouble(SkillBreakdown::averageScore))
                .map(SkillBreakdown::skillTopic);
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
