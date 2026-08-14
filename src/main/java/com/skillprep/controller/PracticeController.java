package com.skillprep.controller;

import com.skillprep.dto.PracticeDtos.*;
import com.skillprep.model.PracticeSession;
import com.skillprep.security.JwtAuthFilter.AuthenticatedUser;
import com.skillprep.service.PracticeService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @PostMapping("/sessions")
    public SessionResponse generate(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody GenerateSessionRequest req) {
        return practiceService.generateSession(user.userId(), req);
    }

    @PostMapping("/sessions/from-bank")
    public SessionResponse fromBank(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody BuildFromBankRequest req) {
        return practiceService.buildFromBank(user.userId(), req);
    }

    @GetMapping("/sessions/{id}")
    public SessionResponse getSession(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        return practiceService.getSession(user.userId(), id);
    }

    @GetMapping("/sessions")
    public List<PracticeSession> history(@AuthenticationPrincipal AuthenticatedUser user) {
        return practiceService.sessionHistory(user.userId());
    }

    @PostMapping("/answers")
    public FeedbackResponse submitAnswer(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody SubmitAnswerRequest req) {
        return practiceService.submitAnswer(user.userId(), req);
    }
}
