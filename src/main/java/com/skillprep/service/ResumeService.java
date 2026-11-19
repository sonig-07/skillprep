package com.skillprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.skillprep.ai.AiService;
import com.skillprep.model.Resume;
import com.skillprep.repository.ResumeRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final AiService aiService;

    public ResumeService(ResumeRepository resumeRepository, AiService aiService) {
        this.resumeRepository = resumeRepository;
        this.aiService = aiService;
    }

    public Resume upload(String userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a file.");
        }
        String text = readText(file);
        if (text.isBlank()) {
            throw new IllegalArgumentException("Could not read any text from this file.");
        }

        deactivateAll(userId);                       

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setFileName(file.getOriginalFilename());
        resume.setRawText(text);

     
        try {
            JsonNode parsed = aiService.extractResume(text);
            List<String> skills = new ArrayList<>();
            parsed.path("skills").forEach(n -> skills.add(n.asText()));
            List<Resume.Project> projects = new ArrayList<>();
            parsed.path("projects").forEach(n ->
                    projects.add(new Resume.Project(n.path("name").asText(), n.path("description").asText())));
            resume.setExtractedSkills(skills);
            resume.setExtractedProjects(projects);
        } catch (Exception e) {
            System.out.println("Resume AI extraction skipped: " + e.getMessage());
        }

        return resumeRepository.save(resume);
    }

    public List<Resume> list(String userId) {
        return resumeRepository.findByUserIdOrderByUploadedAtDesc(userId);
    }

    public Resume getActive(String userId) {
        return resumeRepository.findByUserIdAndActiveTrue(userId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No resume found. Upload a resume first."));
    }

    public Resume setActive(String userId, String id) {
        Resume resume = resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found."));
        deactivateAll(userId);
        resume.setActive(true);
        return resumeRepository.save(resume);
    }

    private void deactivateAll(String userId) {
        List<Resume> active = resumeRepository.findByUserIdAndActiveTrue(userId);
        active.forEach(r -> r.setActive(false));
        resumeRepository.saveAll(active);
    }

    
    private String readText(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            if (name.endsWith(".pdf")) {
                try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
                    return new PDFTextStripper().getText(doc);
                }
            }
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the file: " + e.getMessage());
        }
    }
}
