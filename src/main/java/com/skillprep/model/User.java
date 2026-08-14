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
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private String name;

    /** 0, 1, 2, 3, 4, or 5 (5 means "5+") */
    private Integer experienceLevel;

    /** "dark" or "light" */
    private String themePreference = "dark";

    private Instant createdAt = Instant.now();
}
