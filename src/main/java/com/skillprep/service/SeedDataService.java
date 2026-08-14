package com.skillprep.service;

import com.skillprep.model.Question;
import com.skillprep.repository.QuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Populates a small curated set of well-known general questions per common skill/topic
 * so the Question Bank isn't empty before the user has generated anything with AI.
 * These are global (userId = null, seed = true) and get cloned into a user's own bank
 * the first time that user answers or flags one.
 */
@Service
public class SeedDataService implements CommandLineRunner {

    private final QuestionRepository questionRepository;

    public SeedDataService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public void run(String... args) {
        if (questionRepository.count() > 0) return; // only seed an empty bank

        List<Question> seeds = List.of(
                seed("What is the difference between a process and a thread?", "GENERAL", "Operating Systems", "EASY"),
                seed("Explain the difference between SQL and NoSQL databases, and when you'd choose each.", "GENERAL", "Databases", "EASY"),
                seed("What is REST, and what makes an API RESTful?", "GENERAL", "APIs", "EASY"),
                seed("Explain the concept of Big-O notation and why it matters.", "GENERAL", "Data Structures & Algorithms", "EASY"),
                seed("What is the difference between == and .equals() in Java?", "GENERAL", "Java", "EASY"),
                seed("Explain how garbage collection works in the JVM.", "GENERAL", "Java", "MEDIUM"),
                seed("What is dependency injection and why is it useful?", "GENERAL", "Spring / Frameworks", "MEDIUM"),
                seed("Explain the CAP theorem and its implications for distributed systems.", "GENERAL", "System Design", "HARD"),
                seed("What is the difference between horizontal and vertical scaling?", "GENERAL", "System Design", "MEDIUM"),
                seed("Explain how indexing works in a relational database and its trade-offs.", "GENERAL", "Databases", "MEDIUM"),
                seed("What is the virtual DOM in React and why does it improve performance?", "GENERAL", "React", "EASY"),
                seed("Explain closures in JavaScript with an example.", "GENERAL", "JavaScript", "EASY"),
                seed("What is the difference between authentication and authorization?", "GENERAL", "Security", "EASY"),
                seed("Explain how JWT-based authentication works end-to-end.", "GENERAL", "Security", "MEDIUM"),
                seed("What are the SOLID principles of object-oriented design?", "GENERAL", "Software Design", "MEDIUM"),
                seed("Explain the differences between microservices and a monolithic architecture.", "GENERAL", "System Design", "MEDIUM"),
                seed("What is a race condition, and how can you prevent one?", "GENERAL", "Concurrency", "MEDIUM"),
                seed("Explain the difference between TCP and UDP.", "GENERAL", "Networking", "EASY"),
                seed("What is Docker, and how does containerization differ from virtualization?", "GENERAL", "DevOps", "EASY"),
                seed("Explain eventual consistency and where it's an acceptable trade-off.", "GENERAL", "System Design", "HARD")
        );

        questionRepository.saveAll(seeds);
    }

    private Question seed(String text, String type, String skillTopic, String difficulty) {
        Question q = new Question();
        q.setUserId(null);
        q.setQuestionText(text);
        q.setQuestionType(type);
        q.setSkillTopic(skillTopic);
        q.setDifficulty(difficulty);
        q.setExperienceLevel(null);
        q.setSeed(true);
        return q;
    }
}
