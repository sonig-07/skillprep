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
@Document(collection = "practice_sessions")
public class PracticeSession {

    @Id
    private String id;

    @Indexed
    private String userId;

    private List<String> questionIds;

    private String difficulty;

    private Integer experienceLevel;

    /** "skill:<name>" or "project:<name>" or null for a fresh mixed session */
    private String focus;

    /** true if this session was assembled from the existing bank rather than freshly generated */
    private boolean fromBank = false;

    private Double averageScore;

    private Instant createdAt = Instant.now();

    private Instant completedAt;
}
