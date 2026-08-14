package com.skillprep.controller;

import com.skillprep.model.Resume;
import com.skillprep.security.JwtAuthFilter.AuthenticatedUser;
import com.skillprep.service.ResumeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public Resume upload(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam("file") MultipartFile file) {
        return resumeService.upload(user.userId(), file);
    }

    @GetMapping
    public List<Resume> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return resumeService.listForUser(user.userId());
    }

    @PutMapping("/{id}/active")
    public Resume setActive(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        return resumeService.setActive(user.userId(), id);
    }
}
