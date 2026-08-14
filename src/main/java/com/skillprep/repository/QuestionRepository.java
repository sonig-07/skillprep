package com.skillprep.repository;

import com.skillprep.model.Question;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends MongoRepository<Question, String> {

    List<Question> findByUserIdOrSeedTrue(String userId);

    List<Question> findByUserId(String userId);

    @Query("{ 'userId': ?0, 'skillTopic': ?1, 'dedupKey': ?2 }")
    Optional<Question> findPossibleDuplicate(String userId, String skillTopic, String dedupKey);

    List<Question> findByUserIdAndFlaggedTrue(String userId);

    List<Question> findByIdIn(List<String> ids);
}
