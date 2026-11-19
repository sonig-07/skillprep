package com.skillprep.controller;

import com.skillprep.model.SkillMatch;
import com.skillprep.service.MatchService;
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
    public SkillMatch run(@RequestAttribute("userId") String userId) {
        return matchService.run(userId);
    }

    @GetMapping("/history")
    public List<SkillMatch> history(@RequestAttribute("userId") String userId) {
        return matchService.history(userId);
    }
}
