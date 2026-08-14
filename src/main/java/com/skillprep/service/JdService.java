package com.skillprep.service;

import com.skillprep.dto.ResumeJdDtos.JobDescriptionRequest;
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

    public JobDescription save(String userId, JobDescriptionRequest req) {
        jdRepository.findByUserIdAndActiveTrue(userId).forEach(jd -> {
            jd.setActive(false);
            jdRepository.save(jd);
        });

        JobDescription jd = new JobDescription();
        jd.setUserId(userId);
        jd.setTitle(req.title() != null && !req.title().isBlank() ? req.title() : "Untitled role");
        jd.setRawText(req.rawText());
        jd.setActive(true);
        return jdRepository.save(jd);
    }

    public List<JobDescription> listForUser(String userId) {
        return jdRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public JobDescription getActiveOrThrow(String userId) {
        return jdRepository.findByUserIdAndActiveTrue(userId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No job description on file. Add one first."));
    }

    public JobDescription getByIdOrThrow(String userId, String jdId) {
        JobDescription jd = jdRepository.findById(jdId)
                .orElseThrow(() -> new IllegalArgumentException("Job description not found."));
        if (!jd.getUserId().equals(userId)) throw new SecurityException("Not your job description.");
        return jd;
    }

    public JobDescription setActive(String userId, String jdId) {
        jdRepository.findByUserIdAndActiveTrue(userId).forEach(jd -> {
            jd.setActive(false);
            jdRepository.save(jd);
        });
        JobDescription jd = getByIdOrThrow(userId, jdId);
        jd.setActive(true);
        return jdRepository.save(jd);
    }
}
