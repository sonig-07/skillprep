package com.skillprep.repository;

import com.skillprep.model.PracticeSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PracticeSessionRepository extends MongoRepository<PracticeSession, String> {
    List<PracticeSession> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<PracticeSession> findByIdAndUserId(String id, String userId);
}
