package com.skillprep.controller;

import com.skillprep.dto.MatchDtos.MatchRequest;
import com.skillprep.model.SkillMatch;
import com.skillprep.security.JwtAuthFilter.AuthenticatedUser;
import com.skillprep.service.MatchService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public SkillMatch compute(@AuthenticationPrincipal AuthenticatedUser user, @RequestBody(required = false) MatchRequest req) {
        String resumeId = req != null ? req.resumeId() : null;
        String jdId = req != null ? req.jdId() : null;
        return matchService.computeMatch(user.userId(), resumeId, jdId);
    }

    @GetMapping("/history")
    public List<SkillMatch> history(@AuthenticationPrincipal AuthenticatedUser user) {
        return matchService.history(user.userId());
    }
}
