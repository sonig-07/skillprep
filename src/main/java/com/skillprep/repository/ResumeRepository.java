package com.skillprep.repository;

import com.skillprep.model.Resume;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends MongoRepository<Resume, String> {
    List<Resume> findByUserIdOrderByUploadedAtDesc(String userId);
    List<Resume> findByUserIdAndActiveTrue(String userId);
    Optional<Resume> findByIdAndUserId(String id, String userId);
}
