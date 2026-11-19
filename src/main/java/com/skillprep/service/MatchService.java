package com.skillprep.service;

import com.skillprep.model.SkillMatch;
import com.skillprep.repository.SkillMatchRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


@Service
public class MatchService {

    private final SkillMatchRepository matchRepository;
    private final ResumeService resumeService;
    private final JdService jdService;
    private final SkillMatcher skillMatcher;

    public MatchService(SkillMatchRepository matchRepository, ResumeService resumeService,
                        JdService jdService, SkillMatcher skillMatcher) {
        this.matchRepository = matchRepository;
        this.resumeService = resumeService;
        this.jdService = jdService;
        this.skillMatcher = skillMatcher;
    }

    /** Runs the match, saves it, and returns it. */
    public SkillMatch run(String userId) {
        String resume = resumeService.getActive(userId).getRawText();
        String jd = jdService.getActive(userId).getRawText();

        Map<String, Integer> resumeSkills = skillMatcher.countSkills(resume);   // skill -> times in resume
        Map<String, Integer> jdSkills = skillMatcher.countSkills(jd);           // skill -> times in JD

        int mostMentioned = jdSkills.values().stream().max(Integer::compare).orElse(1);

        List<SkillMatch.Matched> matched = new ArrayList<>();
        List<SkillMatch.Missing> missing = new ArrayList<>();

        for (Map.Entry<String, Integer> jdSkill : jdSkills.entrySet()) {
            String skill = jdSkill.getKey();
            if (resumeSkills.containsKey(skill)) {
                // Confidence: the more the resume mentions it, the more sure we are (1 mention = 60, 3+ = 100)
                int confidence = Math.min(100, 40 + resumeSkills.get(skill) * 20);
                matched.add(new SkillMatch.Matched(skill, confidence));
            } else {
                // Importance: how often the JD mentions it, compared with the JD's most-mentioned skill
                int importance = jdSkill.getValue() * 100 / mostMentioned;
                missing.add(new SkillMatch.Missing(skill, importance));
            }
        }

        matched.sort(Comparator.comparingInt(SkillMatch.Matched::getConfidence).reversed());
        missing.sort(Comparator.comparingInt(SkillMatch.Missing::getImportance).reversed());

        SkillMatch match = new SkillMatch();
        match.setUserId(userId);
        match.setOverallScore(jdSkills.isEmpty() ? 0 : matched.size() * 100 / jdSkills.size());
        match.setMatchedSkills(matched);
        match.setMissingSkills(missing);
        match.setFitSummary(buildSummary(matched.size(), jdSkills.size(), missing));
        return matchRepository.save(match);
    }

    /** Names of skills the JD asks for that the resume does not show. Used to make better questions. */
    public List<String> missingSkills(String resume, String jd) {
        Map<String, Integer> resumeSkills = skillMatcher.countSkills(resume);
        List<String> missing = new ArrayList<>();
        for (String skill : skillMatcher.countSkills(jd).keySet()) {
            if (!resumeSkills.containsKey(skill)) missing.add(skill);
        }
        return missing;
    }

    /** Newest first. */
    public List<SkillMatch> history(String userId) {
        return matchRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private String buildSummary(int matchedCount, int jdCount, List<SkillMatch.Missing> missing) {
        if (jdCount == 0) {
            return "No known skills were found in this job description, so there is nothing to compare.";
        }
        String text = "You match " + matchedCount + " of the " + jdCount + " skills this job asks for.";
        if (missing.isEmpty()) {
            return text + " You cover every skill it asks for.";
        }
        List<String> names = new ArrayList<>();
        for (int i = 0; i < Math.min(5, missing.size()); i++) names.add(missing.get(i).getSkill());
        return text + " Missing: " + String.join(", ", names) + (missing.size() > 5 ? " and more." : ".");
    }
}
