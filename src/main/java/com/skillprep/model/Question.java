package com.skillprep.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "questions")
public class Question {

    @Id
    private String id;

    /** null for global curated seed questions, otherwise owning user's id */
    @Indexed
    private String userId;

    private String questionText;

    /** GENERAL, PROJECT_SPECIFIC, SKILL_GAP */
    private String questionType;

    /** EASY, MEDIUM, HARD */
    private String difficulty;

    /** experience level this question was generated/calibrated for (0-5) */
    private Integer experienceLevel;

    @Indexed
    private String skillTopic;

    /** populated for PROJECT_SPECIFIC questions */
    private String relatedProject;

    /** true for the curated starter set, false for AI-generated */
    private boolean seed = false;

    private boolean flagged = false;

    /** normalized text hash used for near-duplicate detection within a skill/topic window */
    private String dedupKey;

    private String sourceSessionId;

    private List<Attempt> attempts = new ArrayList<>();

    private Instant createdAt = Instant.now();

    public boolean isAnswered() {
        return attempts != null && !attempts.isEmpty();
    }

    public Double latestScore() {
        if (attempts == null || attempts.isEmpty()) return null;
        return (double) attempts.get(attempts.size() - 1).getScore();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attempt {
        private String answerText;
        private int score; // 0-100
        private List<String> strengths;
        private List<String> improvements;
        private String idealAnswerNotes;
        private String sessionId;
        private String difficultyUsed;
        private Integer experienceLevelUsed;
        private Instant answeredAt = Instant.now();
    }
}
