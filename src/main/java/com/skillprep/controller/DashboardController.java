package com.skillprep.controller;

import com.skillprep.dto.Dtos.Dashboard;
import com.skillprep.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public Dashboard dashboard(@RequestAttribute("userId") String userId) {
        return dashboardService.build(userId);
    }

    @GetMapping("/weakest-skill")
    public Map<String, String> weakestSkill(@RequestAttribute("userId") String userId) {
        return Map.of("skillTopic", dashboardService.weakestSkill(userId));
    }
}
