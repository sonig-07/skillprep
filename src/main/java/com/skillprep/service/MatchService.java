package com.skillprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.skillprep.ai.GroqService;
import com.skillprep.ai.PromptFactory;
import com.skillprep.model.JobDescription;
import com.skillprep.model.Resume;
import com.skillprep.model.SkillMatch;
import com.skillprep.repository.SkillMatchRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MatchService {

    private final SkillMatchRepository skillMatchRepository;
    private final ResumeService resumeService;
    private final JdService jdService;
    private final GroqService groqService;
    private final PromptFactory promptFactory;

    public MatchService(SkillMatchRepository skillMatchRepository, ResumeService resumeService,
                         JdService jdService, GroqService groqService, PromptFactory promptFactory) {
        this.skillMatchRepository = skillMatchRepository;
        this.resumeService = resumeService;
        this.jdService = jdService;
        this.groqService = groqService;
        this.promptFactory = promptFactory;
    }

    public SkillMatch computeMatch(String userId, String resumeId, String jdId) {
        Resume resume = resumeId != null ? resumeService.getByIdOrThrow(userId, resumeId)
                                          : resumeService.getActiveOrThrow(userId);
        JobDescription jd = jdId != null ? jdService.getByIdOrThrow(userId, jdId)
                                          : jdService.getActiveOrThrow(userId);

        JsonNode result = groqService.completeJson(promptFactory.skillMatchPrompt(resume.getRawText(), jd.getRawText()));

        SkillMatch match = new SkillMatch();
        match.setUserId(userId);
        match.setResumeId(resume.getId());
        match.setJdId(jd.getId());
        match.setOverallScore(result.path("overallScore").asInt(0));

        List<SkillMatch.MatchedSkill> matched = new ArrayList<>();
        result.path("matchedSkills").forEach(n -> matched.add(
                new SkillMatch.MatchedSkill(n.path("skill").asText(), n.path("confidence").asInt(0))));
        match.setMatchedSkills(matched);

        List<SkillMatch.MissingSkill> missing = new ArrayList<>();
        result.path("missingSkills").forEach(n -> missing.add(
                new SkillMatch.MissingSkill(n.path("skill").asText(), n.path("importance").asInt(0))));
        match.setMissingSkills(missing);

        match.setFitSummary(result.path("fitSummary").asText(""));

        return skillMatchRepository.save(match);
    }

    public List<SkillMatch> history(String userId) {
        return skillMatchRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
