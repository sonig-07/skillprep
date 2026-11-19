package com.skillprep.service;

import com.skillprep.dto.Dtos.JdRequest;
import com.skillprep.model.JobDescription;
import com.skillprep.repository.JobDescriptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JdService {

    private final JobDescriptionRepository jdRepository;

    public JdService(JobDescriptionRepository jdRepository) {
        this.jdRepository = jdRepository;
    }

    public JobDescription save(String userId, JdRequest req) {
        if (req.rawText() == null || req.rawText().isBlank()) {
            throw new IllegalArgumentException("Paste the job description text.");
        }
        deactivateAll(userId);                       
        JobDescription jd = new JobDescription();
        jd.setUserId(userId);
        jd.setTitle(req.title() == null || req.title().isBlank() ? "Untitled role" : req.title());
        jd.setRawText(req.rawText());
        return jdRepository.save(jd);
    }

    public List<JobDescription> list(String userId) {
        return jdRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public JobDescription getActive(String userId) {
        return jdRepository.findByUserIdAndActiveTrue(userId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No job description found. Add one first."));
    }

    public JobDescription setActive(String userId, String id) {
        JobDescription jd = jdRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Job description not found."));
        deactivateAll(userId);
        jd.setActive(true);
        return jdRepository.save(jd);
    }

    private void deactivateAll(String userId) {
        List<JobDescription> active = jdRepository.findByUserIdAndActiveTrue(userId);
        active.forEach(j -> j.setActive(false));
        jdRepository.saveAll(active);
    }
}
