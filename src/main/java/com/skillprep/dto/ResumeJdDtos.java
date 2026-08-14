package com.skillprep.dto;

import jakarta.validation.constraints.NotBlank;

public class ResumeJdDtos {

    public record JobDescriptionRequest(
            String title,
            @NotBlank String rawText
    ) {}
}
