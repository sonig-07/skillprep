package com.skillprep.controller;

import com.skillprep.dto.Dtos.*;
import com.skillprep.service.PracticeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    /** Make new AI questions and start a session. */
    @PostMapping("/sessions")
    public SessionResponse generate(@RequestAttribute("userId") String userId, @RequestBody GenerateRequest req) {
        return practiceService.generate(userId, req);
    }

    /** Start a session from questions already in the bank. */
    @PostMapping("/sessions/from-bank")
    public SessionResponse fromBank(@RequestAttribute("userId") String userId, @RequestBody BankSessionRequest req) {
        return practiceService.fromBank(userId, req);
    }

    @GetMapping("/sessions/{id}")
    public SessionResponse getSession(@RequestAttribute("userId") String userId, @PathVariable String id) {
        return practiceService.getSession(userId, id);
    }

    /** Submit an answer and get the AI score + feedback. */
    @PostMapping("/answers")
    public Feedback submitAnswer(@RequestAttribute("userId") String userId, @RequestBody AnswerRequest req) {
        return practiceService.submitAnswer(userId, req);
    }
}
