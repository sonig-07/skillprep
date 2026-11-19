package com.skillprep.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/** A set of questions practised together. */
@Data
@Document(collection = "practice_sessions")
public class PracticeSession {
    @Id
    private String id;
    @Indexed
    private String userId;
    private List<String> questionIds;
    private String difficulty;
    private Integer experienceLevel;
    private Double averageScore;
    private Instant createdAt = Instant.now();
}
