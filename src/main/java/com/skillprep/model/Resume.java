package com.skillprep.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "resumes")
public class Resume {
    @Id
    private String id;
    @Indexed
    private String userId;
    private String fileName;
    @JsonIgnore                                   // big text, no need to send it to the browser
    private String rawText;
    private List<String> extractedSkills;         // filled by AI
    private List<Project> extractedProjects;      // filled by AI
    private boolean active = true;                // the resume used for questions
    private Instant uploadedAt = Instant.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Project {
        private String name;
        private String description;
    }
}
