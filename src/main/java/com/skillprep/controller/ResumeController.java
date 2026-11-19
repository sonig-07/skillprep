package com.skillprep.controller;

import com.skillprep.model.Resume;
import com.skillprep.service.ResumeService;
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

    @PostMapping
    public Resume upload(@RequestAttribute("userId") String userId, @RequestParam("file") MultipartFile file) {
        return resumeService.upload(userId, file);
    }

    @GetMapping
    public List<Resume> list(@RequestAttribute("userId") String userId) {
        return resumeService.list(userId);
    }

    @PutMapping("/{id}/active")
    public Resume setActive(@RequestAttribute("userId") String userId, @PathVariable String id) {
        return resumeService.setActive(userId, id);
    }
}
