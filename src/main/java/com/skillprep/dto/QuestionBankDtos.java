package com.skillprep.dto;

import java.util.List;

public class QuestionBankDtos {

    public record QuestionBankItem(
            String questionId,
            String questionText,
            String questionType,
            String difficulty,
            Integer experienceLevel,
            String skillTopic,
            String relatedProject,
            boolean seed,
            boolean flagged,
            boolean answered,
            int attemptCount,
            Double latestScore,
            List<AttemptView> attempts
    ) {}

    public record AttemptView(
            String answerText,
            int score,
            List<String> strengths,
            List<String> improvements,
            String idealAnswerNotes,
            String answeredAt
    ) {}
}
