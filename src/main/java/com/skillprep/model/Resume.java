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
@Document(collection = "resumes")
public class Resume {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String fileName;

    private String rawText;

    /** Extracted structured highlights, populated after AI parsing (optional, best-effort) */
    private List<String> extractedSkills;

    private List<ProjectSummary> extractedProjects;

    private boolean active = true;

    private Instant uploadedAt = Instant.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectSummary {
        private String name;
        private String description;
    }
}
