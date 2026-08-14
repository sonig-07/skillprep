package com.skillprep.controller;

import com.skillprep.dto.DashboardDtos.DashboardResponse;
import com.skillprep.security.JwtAuthFilter.AuthenticatedUser;
import com.skillprep.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse dashboard(@AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.build(user.userId());
    }

    @GetMapping("/weakest-skill")
    public Map<String, String> weakestSkill(@AuthenticationPrincipal AuthenticatedUser user) {
        return Map.of("skillTopic", dashboardService.weakestSkill(user.userId()).orElse(""));
    }
}
