package com.skillprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.skillprep.ai.GroqService;
import com.skillprep.ai.PromptFactory;
import com.skillprep.dto.PracticeDtos.*;
import com.skillprep.model.JobDescription;
import com.skillprep.model.PracticeSession;
import com.skillprep.model.Question;
import com.skillprep.model.Resume;
import com.skillprep.model.User;
import com.skillprep.repository.PracticeSessionRepository;
import com.skillprep.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PracticeService {

    private final PracticeSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final ResumeService resumeService;
    private final JdService jdService;
    private final AuthService authService;
    private final GroqService groqService;
    private final PromptFactory promptFactory;

    public PracticeService(PracticeSessionRepository sessionRepository, QuestionRepository questionRepository,
                            ResumeService resumeService, JdService jdService, AuthService authService,
                            GroqService groqService, PromptFactory promptFactory) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.resumeService = resumeService;
        this.jdService = jdService;
        this.authService = authService;
        this.groqService = groqService;
        this.promptFactory = promptFactory;
    }

    public SessionResponse generateSession(String userId, GenerateSessionRequest req) {
        User user = authService.getUserOrThrow(userId);
        Resume resume = req.resumeId() != null ? resumeService.getByIdOrThrow(userId, req.resumeId())
                                                : resumeService.getActiveOrThrow(userId);
        JobDescription jd = req.jdId() != null ? jdService.getByIdOrThrow(userId, req.jdId())
                                                : jdService.getActiveOrThrow(userId);

        int experienceLevel = req.experienceLevel() != null ? req.experienceLevel() : user.getExperienceLevel();
        String difficulty = req.difficulty().toUpperCase(Locale.ROOT);

        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setDifficulty(difficulty);
        session.setExperienceLevel(experienceLevel);
        if (req.focusSkill() != null && !req.focusSkill().isBlank()) {
            session.setFocus("skill:" + req.focusSkill());
        } else if (req.focusProject() != null && !req.focusProject().isBlank()) {
            session.setFocus("project:" + req.focusProject());
        }
        session = sessionRepository.save(session);

        String prompt = promptFactory.questionGenerationPrompt(
                resume.getRawText(), jd.getRawText(), req.numQuestions(), difficulty,
                experienceLevel, req.focusSkill(), req.focusProject());

        JsonNode result = groqService.completeJson(prompt);

        List<Question> saved = new ArrayList<>();
        for (JsonNode qNode : result.path("questions")) {
            String text = qNode.path("questionText").asText("").trim();
            if (text.isBlank()) continue;

            String skillTopic = qNode.path("skillTopic").asText("General");
            String dedupKey = dedupKey(text);

            boolean isDuplicate = questionRepository
                    .findPossibleDuplicate(userId, skillTopic, dedupKey)
                    .isPresent();
            if (isDuplicate) continue; // dedup safeguard: skip near-duplicate generated within the same skill/topic

            Question q = new Question();
            q.setUserId(userId);
            q.setQuestionText(text);
            q.setQuestionType(qNode.path("questionType").asText("GENERAL"));
            q.setDifficulty(difficulty);
            q.setExperienceLevel(experienceLevel);
            q.setSkillTopic(skillTopic);
            String relatedProject = qNode.path("relatedProject").isNull() ? null : qNode.path("relatedProject").asText(null);
            q.setRelatedProject(relatedProject);
            q.setSourceSessionId(session.getId());
            q.setDedupKey(dedupKey);
            saved.add(questionRepository.save(q));
        }

        session.setQuestionIds(saved.stream().map(Question::getId).collect(Collectors.toList()));
        session = sessionRepository.save(session);

        return toSessionResponse(session, saved);
    }

    public SessionResponse buildFromBank(String userId, BuildFromBankRequest req) {
        List<Question> questions = questionRepository.findByIdIn(req.questionIds()).stream()
                .filter(q -> q.getUserId() == null || q.getUserId().equals(userId)) // own questions or global seeds
                .collect(Collectors.toList());

        if (questions.isEmpty()) {
            throw new IllegalArgumentException("None of the requested questions could be found.");
        }

        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setFromBank(true);
        session.setQuestionIds(questions.stream().map(Question::getId).collect(Collectors.toList()));
        session = sessionRepository.save(session);

        return toSessionResponse(session, questions);
    }

    public FeedbackResponse submitAnswer(String userId, SubmitAnswerRequest req) {
        Question question = questionRepository.findById(req.questionId())
                .orElseThrow(() -> new IllegalArgumentException("Question not found."));

        // seed (global) questions get cloned into a user-owned copy on first answer so attempt
        // history stays per-user without mutating the shared curated set
        if (question.getUserId() == null) {
            Question clone = new Question();
            clone.setUserId(userId);
            clone.setQuestionText(question.getQuestionText());
            clone.setQuestionType(question.getQuestionType());
            clone.setDifficulty(question.getDifficulty());
            clone.setExperienceLevel(question.getExperienceLevel());
            clone.setSkillTopic(question.getSkillTopic());
            clone.setRelatedProject(question.getRelatedProject());
            clone.setSeed(false);
            clone.setDedupKey(question.getDedupKey());
            question = questionRepository.save(clone);
        } else if (!question.getUserId().equals(userId)) {
            throw new SecurityException("Not your question.");
        }

        String prompt = promptFactory.answerFeedbackPrompt(
                question.getQuestionText(), question.getQuestionType(), question.getSkillTopic(),
                question.getRelatedProject(), req.answerText(), question.getDifficulty(), question.getExperienceLevel());

        JsonNode result = groqService.completeJson(prompt);

        Question.Attempt attempt = new Question.Attempt();
        attempt.setAnswerText(req.answerText());
        attempt.setScore(result.path("score").asInt(0));
        List<String> strengths = new ArrayList<>();
        result.path("strengths").forEach(n -> strengths.add(n.asText()));
        List<String> improvements = new ArrayList<>();
        result.path("improvements").forEach(n -> improvements.add(n.asText()));
        attempt.setStrengths(strengths);
        attempt.setImprovements(improvements);
        attempt.setIdealAnswerNotes(result.path("idealAnswerNotes").asText(""));
        attempt.setSessionId(req.sessionId());
        attempt.setDifficultyUsed(question.getDifficulty());
        attempt.setExperienceLevelUsed(question.getExperienceLevel());

        question.getAttempts().add(attempt);
        question = questionRepository.save(question);

        if (req.sessionId() != null) {
            updateSessionAverage(req.sessionId());
        }

        return new FeedbackResponse(question.getId(), attempt.getScore(), attempt.getStrengths(),
                attempt.getImprovements(), attempt.getIdealAnswerNotes());
    }

    private void updateSessionAverage(String sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            List<Question> qs = questionRepository.findByIdIn(session.getQuestionIds());
            double avg = qs.stream().filter(Question::isAnswered)
                    .mapToDouble(q -> q.latestScore()).average().orElse(0);
            long answeredCount = qs.stream().filter(Question::isAnswered).count();
            session.setAverageScore(answeredCount > 0 ? avg : null);
            if (answeredCount == qs.size() && !qs.isEmpty()) {
                session.setCompletedAt(java.time.Instant.now());
            }
            sessionRepository.save(session);
        });
    }

    public List<PracticeSession> sessionHistory(String userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public SessionResponse getSession(String userId, String sessionId) {
        PracticeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found."));
        if (!session.getUserId().equals(userId)) throw new SecurityException("Not your session.");
        List<Question> qs = questionRepository.findByIdIn(session.getQuestionIds());
        return toSessionResponse(session, qs);
    }

    private SessionResponse toSessionResponse(PracticeSession session, List<Question> questions) {
        List<QuestionView> views = questions.stream().map(q -> new QuestionView(
                q.getId(), q.getQuestionText(), q.getQuestionType(), q.getDifficulty(), q.getExperienceLevel(),
                q.getSkillTopic(), q.getRelatedProject(), q.isAnswered(), q.latestScore()
        )).collect(Collectors.toList());

        return new SessionResponse(session.getId(), views, session.getDifficulty(),
                session.getExperienceLevel(), session.getFocus());
    }

    /** Normalized hash of a question's text, used to catch near-duplicate generations. */
    private String dedupKey(String text) {
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return normalized;
        }
    }
}
