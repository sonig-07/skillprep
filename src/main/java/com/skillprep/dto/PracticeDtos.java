package com.skillprep.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class PracticeDtos {

    /** Starts a fresh AI-generated practice session */
    public record GenerateSessionRequest(
            @Min(1) @Max(20) int numQuestions,
            @NotBlank String difficulty,      // EASY, MEDIUM, HARD
            Integer experienceLevel,          // overrides profile default if present
            String focusSkill,                // optional
            String focusProject,              // optional
            String resumeId,                  // which resume to ground on (defaults to active)
            String jdId                       // which JD to ground on (defaults to active)
    ) {}

    /** Assembles a session from existing bank question ids */
    public record BuildFromBankRequest(
            @NotEmpty List<String> questionIds
    ) {}

    public record SessionResponse(
            String sessionId,
            List<QuestionView> questions,
            String difficulty,
            Integer experienceLevel,
            String focus
    ) {}

    public record QuestionView(
            String questionId,
            String questionText,
            String questionType,
            String difficulty,
            Integer experienceLevel,
            String skillTopic,
            String relatedProject,
            boolean answered,
            Double latestScore
    ) {}

    public record SubmitAnswerRequest(
            @NotBlank String questionId,
            @NotBlank String answerText,
            String sessionId
    ) {}

    public record FeedbackResponse(
            String questionId,
            int score,
            List<String> strengths,
            List<String> improvements,
            String idealAnswerNotes
    ) {}
}
