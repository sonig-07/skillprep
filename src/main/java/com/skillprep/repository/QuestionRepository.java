package com.skillprep.repository;

import com.skillprep.model.Question;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends MongoRepository<Question, String> {
    List<Question> findByUserId(String userId);
    Optional<Question> findByIdAndUserId(String id, String userId);
    boolean existsByUserIdAndQuestionText(String userId, String questionText);   // duplicate check
}
