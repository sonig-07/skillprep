package com.skillprep.service;

import com.skillprep.dto.Dtos.QuestionView;
import com.skillprep.model.Question;
import com.skillprep.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** The question bank: browse, search, filter, sort and flag saved questions. */
@Service
public class BankService {

    private final QuestionRepository questionRepository;

    public BankService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /** Loads the user's questions, then filters and sorts them in Java (a user has only a few). */
    public List<QuestionView> browse(String userId, String keyword, String questionType, String difficulty,
                                     Boolean answered, Boolean flagged, String sort) {
        Comparator<Question> order;
        if ("lowest_score".equals(sort)) {
            order = Comparator.comparing((Question q) -> q.latestScore() == null ? Double.MAX_VALUE : q.latestScore());
        } else if ("most_attempted".equals(sort)) {
            order = Comparator.comparingInt((Question q) -> q.getAttempts().size()).reversed();
        } else {
            order = Comparator.comparing(Question::getCreatedAt).reversed();   // most recent first
        }

        return questionRepository.findByUserId(userId).stream()
                .filter(q -> isEmpty(keyword)
                        || q.getQuestionText().toLowerCase().contains(keyword.toLowerCase())
                        || q.getSkillTopic().toLowerCase().contains(keyword.toLowerCase()))
                .filter(q -> isEmpty(questionType) || q.getQuestionType().equalsIgnoreCase(questionType))
                .filter(q -> isEmpty(difficulty) || q.getDifficulty().equalsIgnoreCase(difficulty))
                .filter(q -> answered == null || q.isAnswered() == answered)
                .filter(q -> flagged == null || q.isFlagged() == flagged)
                .sorted(order)
                .map(QuestionView::of)
                .toList();
    }

    /** Star / un-star a question. */
    public QuestionView toggleFlag(String userId, String questionId) {
        Question q = questionRepository.findByIdAndUserId(questionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found."));
        q.setFlagged(!q.isFlagged());
        return QuestionView.of(questionRepository.save(q));
    }

    public List<Question> all(String userId) {
        return questionRepository.findByUserId(userId);
    }

    private boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }
}
