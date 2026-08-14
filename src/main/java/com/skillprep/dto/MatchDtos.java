package com.skillprep.dto;

public class MatchDtos {

    public record MatchRequest(
            String resumeId,
            String jdId
    ) {}
}
