package com.skillprep.repository;

import com.skillprep.model.SkillMatch;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SkillMatchRepository extends MongoRepository<SkillMatch, String> {
    List<SkillMatch> findByUserIdOrderByCreatedAtDesc(String userId);
}
