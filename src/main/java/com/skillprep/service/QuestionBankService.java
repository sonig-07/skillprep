package com.skillprep.service;

import com.skillprep.dto.QuestionBankDtos.AttemptView;
import com.skillprep.dto.QuestionBankDtos.QuestionBankItem;
import com.skillprep.model.Question;
import com.skillprep.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionBankService {

    private final QuestionRepository questionRepository;

    public QuestionBankService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * Returns the user's full browsable bank: their own generated questions plus the
     * global curated seed set, with optional filters and sort applied in-memory
     * (the bank is small per-user, so this keeps the query layer simple).
     */
    public List<QuestionBankItem> browse(String userId, String skillTopic, String questionType, String difficulty,
                                          Integer experienceLevel, Boolean answered, Boolean flagged,
                                          String keyword, String sort) {
        List<Question> all = questionRepository.findByUserIdOrSeedTrue(userId);

        List<Question> filtered = all.stream()
                .filter(q -> skillTopic == null || skillTopic.isBlank() || q.getSkillTopic().equalsIgnoreCase(skillTopic))
                .filter(q -> questionType == null || questionType.isBlank() || q.getQuestionType().equalsIgnoreCase(questionType))
                .filter(q -> difficulty == null || difficulty.isBlank() || q.getDifficulty().equalsIgnoreCase(difficulty))
                .filter(q -> experienceLevel == null || (q.getExperienceLevel() != null && q.getExperienceLevel().equals(experienceLevel)))
                .filter(q -> answered == null || q.isAnswered() == answered)
                .filter(q -> flagged == null || q.isFlagged() == flagged)
                .filter(q -> keyword == null || keyword.isBlank()
                        || q.getQuestionText().toLowerCase().contains(keyword.toLowerCase())
                        || q.getSkillTopic().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());

        Comparator<Question> comparator = switch (sort == null ? "recent" : sort) {
            case "lowest_score" -> Comparator.comparing(
                    (Question q) -> q.latestScore() == null ? Double.MAX_VALUE : q.latestScore());
            case "most_attempted" -> Comparator.comparing((Question q) -> q.getAttempts().size()).reversed();
            default -> Comparator.comparing(Question::getCreatedAt).reversed();
        };
        filtered.sort(comparator);

        return filtered.stream().map(this::toItem).collect(Collectors.toList());
    }

    public Question flagToggle(String userId, String questionId) {
        Question q = getOwnedOrClone(userId, questionId);
        q.setFlagged(!q.isFlagged());
        return questionRepository.save(q);
    }

    public List<Question> flaggedQueue(String userId) {
        return questionRepository.findByUserIdAndFlaggedTrue(userId);
    }

    public List<Question> allForUser(String userId) {
        return questionRepository.findByUserIdOrSeedTrue(userId);
    }

    /** Seed (global) questions can't be flagged in place — clone into the user's own bank first. */
    private Question getOwnedOrClone(String userId, String questionId) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found."));
        if (q.getUserId() != null && q.getUserId().equals(userId)) return q;
        if (q.getUserId() != null) throw new SecurityException("Not your question.");

        Question clone = new Question();
        clone.setUserId(userId);
        clone.setQuestionText(q.getQuestionText());
        clone.setQuestionType(q.getQuestionType());
        clone.setDifficulty(q.getDifficulty());
        clone.setExperienceLevel(q.getExperienceLevel());
        clone.setSkillTopic(q.getSkillTopic());
        clone.setRelatedProject(q.getRelatedProject());
        clone.setDedupKey(q.getDedupKey());
        clone.setSeed(false);
        return questionRepository.save(clone);
    }

    private QuestionBankItem toItem(Question q) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;
        List<AttemptView> attempts = q.getAttempts().stream().map(a -> new AttemptView(
                a.getAnswerText(), a.getScore(), a.getStrengths(), a.getImprovements(),
                a.getIdealAnswerNotes(), fmt.format(a.getAnsweredAt())
        )).collect(Collectors.toList());

        return new QuestionBankItem(
                q.getId(), q.getQuestionText(), q.getQuestionType(), q.getDifficulty(), q.getExperienceLevel(),
                q.getSkillTopic(), q.getRelatedProject(), q.isSeed(), q.isFlagged(), q.isAnswered(),
                q.getAttempts().size(), q.latestScore(), attempts
        );
    }
}
