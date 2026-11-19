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
@Document(collection = "skill_matches")
public class SkillMatch {
    @Id
    private String id;
    @Indexed
    private String userId;
    private int overallScore;                     // 0 to 100
    private List<Matched> matchedSkills;
    private List<Missing> missingSkills;
    private String fitSummary;
    private Instant createdAt = Instant.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Matched {
        private String skill;
        private int confidence;                   // 0 to 100
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Missing {
        private String skill;
        private int importance;                   // 0 to 100
    }
}
