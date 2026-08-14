package com.skillprep.controller;

import com.skillprep.dto.ResumeJdDtos.JobDescriptionRequest;
import com.skillprep.model.JobDescription;
import com.skillprep.security.JwtAuthFilter.AuthenticatedUser;
import com.skillprep.service.JdService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jd")
public class JdController {

    private final JdService jdService;

    public JdController(JdService jdService) {
        this.jdService = jdService;
    }

    @PostMapping
    public JobDescription create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody JobDescriptionRequest req) {
        return jdService.save(user.userId(), req);
    }

    @GetMapping
    public List<JobDescription> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return jdService.listForUser(user.userId());
    }

    @PutMapping("/{id}/active")
    public JobDescription setActive(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        return jdService.setActive(user.userId(), id);
    }
}
