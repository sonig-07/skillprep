package com.skillprep.repository;

import com.skillprep.model.JobDescription;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface JobDescriptionRepository extends MongoRepository<JobDescription, String> {
    List<JobDescription> findByUserIdOrderByCreatedAtDesc(String userId);
    List<JobDescription> findByUserIdAndActiveTrue(String userId);
    Optional<JobDescription> findByIdAndUserId(String id, String userId);
}
