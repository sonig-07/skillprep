package com.skillprep.service;

import com.skillprep.dto.Dtos.*;
import com.skillprep.model.PracticeSession;
import com.skillprep.model.Question;
import com.skillprep.repository.PracticeSessionRepository;
import com.skillprep.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/** Turns all saved attempts into the numbers shown on the dashboard. */
@Service
public class DashboardService {

    private final QuestionRepository questionRepository;
    private final PracticeSessionRepository sessionRepository;

    public DashboardService(QuestionRepository questionRepository, PracticeSessionRepository sessionRepository) {
        this.questionRepository = questionRepository;
        this.sessionRepository = sessionRepository;
    }

    public Dashboard build(String userId) {
        // Go through every attempt once and drop its score into the groups it belongs to.
        List<Question.Attempt> allAttempts = new ArrayList<>();
        Map<String, List<Integer>> byType = new LinkedHashMap<>();
        Map<String, List<Integer>> bySkill = new LinkedHashMap<>();
        Map<String, List<Integer>> byLevel = new LinkedHashMap<>();

        for (Question q : questionRepository.findByUserId(userId)) {
            for (Question.Attempt a : q.getAttempts()) {
                allAttempts.add(a);
                byType.computeIfAbsent(q.getQuestionType(), k -> new ArrayList<>()).add(a.getScore());
                bySkill.computeIfAbsent(q.getSkillTopic(), k -> new ArrayList<>()).add(a.getScore());
                if (q.getExperienceLevel() != null) {
                    byLevel.computeIfAbsent(String.valueOf(q.getExperienceLevel()), k -> new ArrayList<>()).add(a.getScore());
                }
            }
        }

        double overall = average(allAttempts.stream().map(Question.Attempt::getScore).toList());

        // A skill is "weak" if it is more than 10 points below the overall average.
        List<SkillScore> skillScores = new ArrayList<>();
        bySkill.forEach((skill, scores) -> {
            double avg = average(scores);
            skillScores.add(new SkillScore(skill, avg, avg < overall - 10));
        });
        skillScores.sort(Comparator.comparingDouble(SkillScore::averageScore));   // weakest first

        // Score trend = last 20 scores in time order.
        allAttempts.sort(Comparator.comparing(Question.Attempt::getAnsweredAt));
        List<Double> trend = new ArrayList<>();
        for (Question.Attempt a : allAttempts) trend.add((double) a.getScore());
        if (trend.size() > 20) trend = trend.subList(trend.size() - 20, trend.size());

        // 10 most recent sessions.
        List<SessionSummary> sessions = new ArrayList<>();
        for (PracticeSession s : sessionRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            if (sessions.size() == 10) break;
            sessions.add(new SessionSummary(s.getId(), s.getCreatedAt().toString(),
                    s.getQuestionIds().size(), s.getAverageScore(), s.getDifficulty()));
        }

        return new Dashboard(
                allAttempts.isEmpty() ? null : overall,
                allAttempts.size(),
                averages(byType),
                skillScores,
                averages(byLevel),
                trend,
                sessions);
    }

    /** The weakest skill (for the "Practice my weakest skill" button), or "" if none yet. */
    public String weakestSkill(String userId) {
        List<SkillScore> skills = build(userId).scoreBySkill();
        return skills.isEmpty() ? "" : skills.get(0).skillTopic();
    }

    private Map<String, Double> averages(Map<String, List<Integer>> groups) {
        Map<String, Double> result = new LinkedHashMap<>();
        groups.forEach((name, scores) -> result.put(name, average(scores)));
        return result;
    }

    /** Average rounded to 1 decimal. */
    private double average(List<Integer> scores) {
        if (scores.isEmpty()) return 0;
        double sum = 0;
        for (int s : scores) sum += s;
        return Math.round(sum / scores.size() * 10) / 10.0;
    }
}
