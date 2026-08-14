package com.skillprep.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "job_descriptions")
public class JobDescription {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;

    private String rawText;

    private boolean active = true;

    private Instant createdAt = Instant.now();
}
