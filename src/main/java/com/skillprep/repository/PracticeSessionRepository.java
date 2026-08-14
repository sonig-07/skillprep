package com.skillprep.repository;

import com.skillprep.model.PracticeSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PracticeSessionRepository extends MongoRepository<PracticeSession, String> {
    List<PracticeSession> findByUserIdOrderByCreatedAtDesc(String userId);
}
