package com.skillprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.skillprep.ai.AiService;
import com.skillprep.dto.Dtos.*;
import com.skillprep.model.PracticeSession;
import com.skillprep.model.Question;
import com.skillprep.repository.PracticeSessionRepository;
import com.skillprep.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class PracticeService {

    private final QuestionRepository questionRepository;
    private final PracticeSessionRepository sessionRepository;
    private final AuthService authService;
    private final ResumeService resumeService;
    private final JdService jdService;
    private final AiService aiService;
    private final MatchService matchService;

    public PracticeService(QuestionRepository questionRepository, PracticeSessionRepository sessionRepository,
                           AuthService authService, ResumeService resumeService, JdService jdService,
                           AiService aiService, MatchService matchService) {
        this.questionRepository = questionRepository;
        this.sessionRepository = sessionRepository;
        this.authService = authService;
        this.resumeService = resumeService;
        this.jdService = jdService;
        this.aiService = aiService;
        this.matchService = matchService;
    }

    /** 1. Ask the AI for questions, 2. save them, 3. start a session with them. */
    public SessionResponse generate(String userId, GenerateRequest req) {
        String resume = resumeService.getActive(userId).getRawText();
        String jd = jdService.getActive(userId).getRawText();

        int count = Math.max(1, Math.min(req.numQuestions(), 20));
        String difficulty = req.difficulty() == null || req.difficulty().isBlank() ? "MEDIUM" : req.difficulty().toUpperCase();
        int level = req.experienceLevel() != null ? req.experienceLevel() : authService.getUser(userId).getExperienceLevel();

        // Our own code finds the skill gaps first; the AI then writes questions using them.
        List<String> missingSkills = matchService.missingSkills(resume, jd);

        JsonNode result = aiService.generateQuestions(resume, jd, count, difficulty, level,
                req.focusSkill(), req.focusProject(), missingSkills);

        List<Question> saved = new ArrayList<>();
        for (JsonNode node : result.path("questions")) {
            String text = node.path("questionText").asText("").trim();
            if (text.isEmpty()) continue;
            if (questionRepository.existsByUserIdAndQuestionText(userId, text)) continue;   // skip duplicates

            Question q = new Question();
            q.setUserId(userId);
            q.setQuestionText(text);
            q.setQuestionType(textOr(node, "questionType", "GENERAL"));
            q.setSkillTopic(textOr(node, "skillTopic", "General"));
            q.setRelatedProject(textOr(node, "relatedProject", null));
            q.setDifficulty(difficulty);
            q.setExperienceLevel(level);
            saved.add(questionRepository.save(q));
        }
        if (saved.isEmpty()) {
            throw new IllegalStateException("The AI did not return any new questions. Please try again.");
        }
        return startSession(userId, saved, difficulty, level);
    }

    /** Start a session from questions already saved in the question bank. */
    public SessionResponse fromBank(String userId, BankSessionRequest req) {
        if (req.questionIds() == null || req.questionIds().isEmpty()) {
            throw new IllegalArgumentException("Select at least one question.");
        }
        List<Question> questions = loadQuestions(userId, req.questionIds());
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("Those questions were not found.");
        }
        return startSession(userId, questions, null, null);
    }

    public SessionResponse getSession(String userId, String sessionId) {
        PracticeSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found."));
        return toResponse(session, loadQuestions(userId, session.getQuestionIds()));
    }

    /** Ask the AI to score the answer, then save it as a new attempt on the question. */
    public Feedback submitAnswer(String userId, AnswerRequest req) {
        if (req.answerText() == null || req.answerText().isBlank()) {
            throw new IllegalArgumentException("Write an answer first.");
        }
        Question q = questionRepository.findByIdAndUserId(req.questionId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found."));

        JsonNode result = aiService.evaluateAnswer(q.getQuestionText(), q.getSkillTopic(),
                q.getDifficulty(), q.getExperienceLevel(), req.answerText());

        Question.Attempt attempt = new Question.Attempt();
        attempt.setAnswerText(req.answerText());
        attempt.setScore(Math.max(0, Math.min(100, result.path("score").asInt(0))));
        attempt.setStrengths(toList(result.path("strengths")));
        attempt.setImprovements(toList(result.path("improvements")));
        attempt.setIdealAnswerNotes(result.path("idealAnswerNotes").asText(""));

        q.getAttempts().add(attempt);
        questionRepository.save(q);

        updateSessionAverage(userId, req.sessionId());

        return new Feedback(q.getId(), attempt.getScore(), attempt.getStrengths(),
                attempt.getImprovements(), attempt.getIdealAnswerNotes());
    }

    // ---------- helpers ----------

    private SessionResponse startSession(String userId, List<Question> questions, String difficulty, Integer level) {
        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setDifficulty(difficulty);
        session.setExperienceLevel(level);
        session.setQuestionIds(questions.stream().map(Question::getId).toList());
        session = sessionRepository.save(session);
        return toResponse(session, questions);
    }

    /** Session average = average of the latest score of each answered question. */
    private void updateSessionAverage(String userId, String sessionId) {
        if (sessionId == null) return;
        sessionRepository.findByIdAndUserId(sessionId, userId).ifPresent(session -> {
            double sum = 0;
            int answered = 0;
            for (Question q : loadQuestions(userId, session.getQuestionIds())) {
                if (q.isAnswered()) {
                    sum += q.latestScore();
                    answered++;
                }
            }
            session.setAverageScore(answered == 0 ? null : Math.round(sum / answered * 10) / 10.0);
            sessionRepository.save(session);
        });
    }

    /** Loads the user's questions and keeps them in the same order as the ids. */
    private List<Question> loadQuestions(String userId, List<String> ids) {
        List<Question> found = new ArrayList<>();
        for (Question q : questionRepository.findAllById(ids)) {
            if (userId.equals(q.getUserId())) found.add(q);
        }
        found.sort((a, b) -> Integer.compare(ids.indexOf(a.getId()), ids.indexOf(b.getId())));
        return found;
    }

    private SessionResponse toResponse(PracticeSession session, List<Question> questions) {
        return new SessionResponse(session.getId(), questions.stream().map(QuestionView::of).toList());
    }

    private String textOr(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("");
        return value.isBlank() || value.equals("null") ? fallback : value;
    }

    private List<String> toList(JsonNode array) {
        List<String> list = new ArrayList<>();
        array.forEach(n -> list.add(n.asText()));
        return list;
    }
}
