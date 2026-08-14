package com.skillprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.skillprep.ai.GroqService;
import com.skillprep.ai.PromptFactory;
import com.skillprep.model.Resume;
import com.skillprep.repository.ResumeRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final GroqService groqService;
    private final PromptFactory promptFactory;

    public ResumeService(ResumeRepository resumeRepository, GroqService groqService, PromptFactory promptFactory) {
        this.resumeRepository = resumeRepository;
        this.groqService = groqService;
        this.promptFactory = promptFactory;
    }

    public Resume upload(String userId, MultipartFile file) {
        String text = extractText(file);
        if (text.isBlank()) {
            throw new IllegalArgumentException("Could not extract any text from the uploaded file.");
        }

        // deactivate previous resumes so there's one clear "active" resume to ground questions on,
        // while keeping history viewable/re-selectable
        resumeRepository.findByUserIdAndActiveTrue(userId).forEach(r -> {
            r.setActive(false);
            resumeRepository.save(r);
        });

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setFileName(file.getOriginalFilename());
        resume.setRawText(text);
        resume.setActive(true);

        try {
            JsonNode parsed = groqService.completeJson(promptFactory.resumeExtractionPrompt(text));
            List<String> skills = new ArrayList<>();
            parsed.path("skills").forEach(n -> skills.add(n.asText()));
            List<Resume.ProjectSummary> projects = new ArrayList<>();
            parsed.path("projects").forEach(n -> projects.add(
                    new Resume.ProjectSummary(n.path("name").asText(), n.path("description").asText())));
            resume.setExtractedSkills(skills);
            resume.setExtractedProjects(projects);
        } catch (Exception e) {
            // best-effort: extraction failing should never block the resume upload itself
            log.warn("Resume AI extraction failed for user {}: {}", userId, e.getMessage());
        }

        return resumeRepository.save(resume);
    }

    public List<Resume> listForUser(String userId) {
        return resumeRepository.findByUserIdOrderByUploadedAtDesc(userId);
    }

    public Resume getActiveOrThrow(String userId) {
        return resumeRepository.findByUserIdAndActiveTrue(userId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No resume on file. Upload a resume first."));
    }

    public Resume getByIdOrThrow(String userId, String resumeId) {
        Resume r = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found."));
        if (!r.getUserId().equals(userId)) throw new SecurityException("Not your resume.");
        return r;
    }

    public Resume setActive(String userId, String resumeId) {
        resumeRepository.findByUserIdAndActiveTrue(userId).forEach(r -> {
            r.setActive(false);
            resumeRepository.save(r);
        });
        Resume r = getByIdOrThrow(userId, resumeId);
        r.setActive(true);
        return resumeRepository.save(r);
    }

    private String extractText(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            if (filename.endsWith(".pdf")) {
                try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
                    return new PDFTextStripper().getText(doc);
                }
            } else {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded file: " + e.getMessage());
        }
    }
}
