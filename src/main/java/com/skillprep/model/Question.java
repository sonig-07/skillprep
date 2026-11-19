package com.skillprep.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** One interview question + every answer the user has given to it (attempts). */
@Data
@Document(collection = "questions")
public class Question {
    @Id
    private String id;
    @Indexed
    private String userId;
    private String questionText;
    private String questionType;                  // GENERAL, PROJECT_SPECIFIC, SKILL_GAP
    private String difficulty;                    // EASY, MEDIUM, HARD
    private Integer experienceLevel;
    private String skillTopic;
    private String relatedProject;
    private boolean flagged = false;
    private List<Attempt> attempts = new ArrayList<>();
    private Instant createdAt = Instant.now();

    public boolean isAnswered() {
        return !attempts.isEmpty();
    }

    /** Score of the most recent attempt, or null if never answered. */
    public Double latestScore() {
        return attempts.isEmpty() ? null : (double) attempts.get(attempts.size() - 1).getScore();
    }

    @Data
    public static class Attempt {
        private String answerText;
        private int score;                        // 0 to 100
        private List<String> strengths;
        private List<String> improvements;
        private String idealAnswerNotes;
        private Instant answeredAt = Instant.now();
    }
}
