package com.skillprep.controller;

import com.skillprep.dto.QuestionBankDtos.QuestionBankItem;
import com.skillprep.model.Question;
import com.skillprep.security.JwtAuthFilter.AuthenticatedUser;
import com.skillprep.service.ExportService;
import com.skillprep.service.QuestionBankService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank")
public class QuestionBankController {

    private final QuestionBankService questionBankService;
    private final ExportService exportService;

    public QuestionBankController(QuestionBankService questionBankService, ExportService exportService) {
        this.questionBankService = questionBankService;
        this.exportService = exportService;
    }

    @GetMapping
    public List<QuestionBankItem> browse(@AuthenticationPrincipal AuthenticatedUser user,
                                          @RequestParam(required = false) String skillTopic,
                                          @RequestParam(required = false) String questionType,
                                          @RequestParam(required = false) String difficulty,
                                          @RequestParam(required = false) Integer experienceLevel,
                                          @RequestParam(required = false) Boolean answered,
                                          @RequestParam(required = false) Boolean flagged,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String sort) {
        return questionBankService.browse(user.userId(), skillTopic, questionType, difficulty,
                experienceLevel, answered, flagged, keyword, sort);
    }

    @PostMapping("/{id}/flag")
    public Question toggleFlag(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        return questionBankService.flagToggle(user.userId(), id);
    }

    @GetMapping("/flagged")
    public List<Question> flaggedQueue(@AuthenticationPrincipal AuthenticatedUser user) {
        return questionBankService.flaggedQueue(user.userId());
    }

    @GetMapping(value = "/export", params = "format=csv")
    public ResponseEntity<ByteArrayResource> exportCsv(@AuthenticationPrincipal AuthenticatedUser user,
                                                         @RequestParam(required = false) String skillTopic,
                                                         @RequestParam(required = false) Boolean flagged) {
        List<Question> questions = filteredQuestions(user.userId(), skillTopic, flagged);
        byte[] data = exportService.toCsv(questions);
        return fileResponse(data, "skillprep-question-bank.csv", "text/csv");
    }

    @GetMapping(value = "/export", params = "format=pdf")
    public ResponseEntity<ByteArrayResource> exportPdf(@AuthenticationPrincipal AuthenticatedUser user,
                                                         @RequestParam(required = false) String skillTopic,
                                                         @RequestParam(required = false) Boolean flagged) {
        List<Question> questions = filteredQuestions(user.userId(), skillTopic, flagged);
        byte[] data = exportService.toPdf(questions);
        return fileResponse(data, "skillprep-question-bank.pdf", "application/pdf");
    }

    private List<Question> filteredQuestions(String userId, String skillTopic, Boolean flagged) {
        return questionBankService.allForUser(userId).stream()
                .filter(q -> skillTopic == null || skillTopic.isBlank() || q.getSkillTopic().equalsIgnoreCase(skillTopic))
                .filter(q -> flagged == null || q.isFlagged() == flagged)
                .toList();
    }

    private ResponseEntity<ByteArrayResource> fileResponse(byte[] data, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(new ByteArrayResource(data));
    }
}
