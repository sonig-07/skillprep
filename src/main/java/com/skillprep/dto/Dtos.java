package com.skillprep.dto;

import com.skillprep.model.Question;

import java.util.List;
import java.util.Map;


public class Dtos {

    // ----- auth -----
    public record SignUpRequest(String name, String email, String password, Integer experienceLevel) {}
    public record SignInRequest(String email, String password) {}
    public record ProfileRequest(String themePreference) {}
    public record AuthResponse(String token, String userId, String name, String email,
                               Integer experienceLevel, String themePreference) {}

    // ----- resume / job description -----
    public record JdRequest(String title, String rawText) {}

    // ----- practice -----
    public record GenerateRequest(int numQuestions, String difficulty, Integer experienceLevel,
                                  String focusSkill, String focusProject) {}
    public record BankSessionRequest(List<String> questionIds) {}
    public record AnswerRequest(String questionId, String answerText, String sessionId) {}
    public record Feedback(String questionId, int score, List<String> strengths,
                           List<String> improvements, String idealAnswerNotes) {}
    public record SessionResponse(String sessionId, List<QuestionView> questions) {}

    /** A question as the frontend sees it (used by both the practice screen and the question bank). */
    public record QuestionView(String questionId, String questionText, String questionType, String difficulty,
                               Integer experienceLevel, String skillTopic, String relatedProject,
                               boolean flagged, boolean answered, int attemptCount, Double latestScore,
                               List<Question.Attempt> attempts) {
        public static QuestionView of(Question q) {
            return new QuestionView(q.getId(), q.getQuestionText(), q.getQuestionType(), q.getDifficulty(),
                    q.getExperienceLevel(), q.getSkillTopic(), q.getRelatedProject(),
                    q.isFlagged(), q.isAnswered(), q.getAttempts().size(), q.latestScore(), q.getAttempts());
        }
    }

    // ----- dashboard -----
    public record SkillScore(String skillTopic, double averageScore, boolean weakArea) {}
    public record SessionSummary(String sessionId, String createdAt, int questionCount,
                                 Double averageScore, String difficulty) {}
    public record Dashboard(Double overallAverageScore, int totalAnswered,
                            Map<String, Double> scoreByQuestionType,
                            List<SkillScore> scoreBySkill,
                            Map<String, Double> scoreByExperienceLevel,
                            List<Double> recentScoreTrend,
                            List<SessionSummary> recentSessions) {}
}
