package com.skillprep.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "skill_matches")
public class SkillMatch {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String resumeId;
    private String jdId;

    private int overallScore;

    private List<MatchedSkill> matchedSkills;
    private List<MissingSkill> missingSkills;

    private String fitSummary;

    private Instant createdAt = Instant.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedSkill {
        private String skill;
        private int confidence; // 0-100
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingSkill {
        private String skill;
        private int importance; // 0-100, rank by importance to JD
    }
}
