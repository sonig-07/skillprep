package com.skillprep.controller;

import com.skillprep.dto.Dtos.QuestionView;
import com.skillprep.service.BankService;
import com.skillprep.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank")
public class BankController {

    private final BankService bankService;
    private final ExportService exportService;

    public BankController(BankService bankService, ExportService exportService) {
        this.bankService = bankService;
        this.exportService = exportService;
    }

    /** Filters Optional. */
    @GetMapping
    public List<QuestionView> browse(@RequestAttribute("userId") String userId,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String questionType,
                                     @RequestParam(required = false) String difficulty,
                                     @RequestParam(required = false) Boolean answered,
                                     @RequestParam(required = false) Boolean flagged,
                                     @RequestParam(required = false) String sort) {
        return bankService.browse(userId, keyword, questionType, difficulty, answered, flagged, sort);
    }

    @PostMapping("/{id}/flag")
    public QuestionView toggleFlag(@RequestAttribute("userId") String userId, @PathVariable String id) {
        return bankService.toggleFlag(userId, id);
    }

    /** Download the Question Bank*/


    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestAttribute("userId") String userId, @RequestParam String format) {
        var questions = bankService.all(userId);
        boolean pdf = "pdf".equalsIgnoreCase(format);

        byte[] data = pdf ? exportService.toPdf(questions) : exportService.toCsv(questions);
        String fileName = "skillprep-question-bank." + (pdf ? "pdf" : "csv");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(pdf ? "application/pdf" : "text/csv"))
                .body(data);
    }
}
