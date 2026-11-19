package com.skillprep.controller;

import com.skillprep.dto.Dtos.JdRequest;
import com.skillprep.model.JobDescription;
import com.skillprep.service.JdService;
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
    public JobDescription save(@RequestAttribute("userId") String userId, @RequestBody JdRequest req) {
        return jdService.save(userId, req);
    }

    @GetMapping
    public List<JobDescription> list(@RequestAttribute("userId") String userId) {
        return jdService.list(userId);
    }

    @PutMapping("/{id}/active")
    public JobDescription setActive(@RequestAttribute("userId") String userId, @PathVariable String id) {
        return jdService.setActive(userId, id);
    }
}
