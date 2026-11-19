package com.skillprep.service;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class SkillMatcher {

    
    private static final Map<String, List<String>> SKILLS = new LinkedHashMap<>();

    static {
        // Languages
        add("Java", "java", "core java", "j2ee", "jdk");
        add("Python", "python");
        add("JavaScript", "javascript", "js", "es6");
        add("TypeScript", "typescript");
        add("C++", "c++");
        add("C#", "c#");
        add("Go", "golang");
        add("Kotlin", "kotlin");
        add("PHP", "php");

        // Web / frontend
        add("HTML", "html", "html5");
        add("CSS", "css", "css3");
        add("React", "react", "reactjs", "react.js");
        add("Angular", "angular", "angularjs");
        add("Vue", "vue", "vuejs", "vue.js");
        add("Node.js", "node.js", "nodejs", "node js");
        add("Bootstrap", "bootstrap");
        add("Tailwind CSS", "tailwind", "tailwindcss");

        // Backend frameworks
        add("Spring Boot", "spring boot", "springboot", "spring framework", "spring mvc");
        add("Hibernate", "hibernate");
        add("JPA", "jpa", "spring data");
        add("Servlet & JSP", "servlet", "servlets", "jsp");
        add("Django", "django");
        add("Flask", "flask");
        add("Express.js", "express.js", "expressjs");

        // Databases
        add("SQL", "sql");
        add("MySQL", "mysql");
        add("PostgreSQL", "postgresql", "postgres");
        add("MongoDB", "mongodb", "mongo");
        add("Oracle", "oracle");
        add("Redis", "redis");

        // APIs and architecture
        add("REST API", "rest", "restful", "rest api", "rest apis", "web services");
        add("Microservices", "microservices", "microservice");
        add("JWT", "jwt", "json web token");
        add("Authentication", "authentication", "authorization", "oauth");
        add("OOP", "oop", "oops", "object oriented", "object-oriented");
        add("Data Structures", "data structures", "dsa");
        add("Algorithms", "algorithms", "algorithm");
        add("Design Patterns", "design patterns", "design pattern");
        add("System Design", "system design");
        add("Multithreading", "multithreading", "concurrency", "multi-threading");
        add("Collections", "collections framework", "arraylist", "hashmap");

        // Tools
        add("Git", "git", "github", "gitlab", "bitbucket");
        add("Maven", "maven");
        add("Gradle", "gradle");
        add("Postman", "postman");
        add("JIRA", "jira");
        add("Linux", "linux", "unix");

        // DevOps / cloud
        add("Docker", "docker", "containers", "containerization");
        add("Kubernetes", "kubernetes", "k8s");
        add("CI/CD", "ci/cd", "cicd", "jenkins", "github actions");
        add("AWS", "aws", "amazon web services");
        add("Azure", "azure");
        add("GCP", "gcp", "google cloud");

        // Testing / QA
        add("Selenium", "selenium");
        add("JUnit", "junit");
        add("TestNG", "testng");
        add("Cucumber", "cucumber", "bdd");
        add("Manual Testing", "manual testing", "test cases", "test case", "test plan");
        add("API Testing", "api testing");
        add("Automation Testing", "automation testing", "test automation");
        add("Mockito", "mockito");

        // Ways of working
        add("Agile", "agile", "scrum");
    }

    /** Skill name -> compiled patterns (made once, reused every time). */
    private static final Map<String, List<Pattern>> PATTERNS = new LinkedHashMap<>();

    static {
        SKILLS.forEach((skill, aliases) -> {
            List<Pattern> list = new ArrayList<>();
            for (String alias : aliases) {
                // "Whole word" = the characters just before and just after must not be a letter,
                // digit, + or #.  So "java" does NOT match inside "javascript",
                // and "c++" still works.
                list.add(Pattern.compile("(?<![a-z0-9+#])" + Pattern.quote(alias) + "(?![a-z0-9+#])"));
            }
            PATTERNS.put(skill, list);
        });
    }

    private static void add(String skill, String... aliases) {
        SKILLS.put(skill, List.of(aliases));
    }

    /**
     * Returns every known skill found in the text, with how many times it appears.
     * Example: {"Java" = 5, "SQL" = 2}.  Skills that are not in the text are left out.
     */
    public Map<String, Integer> countSkills(String text) {
        String lower = text.toLowerCase();
        Map<String, Integer> found = new LinkedHashMap<>();

        for (Map.Entry<String, List<Pattern>> entry : PATTERNS.entrySet()) {
            int count = 0;
            for (Pattern pattern : entry.getValue()) {
                Matcher m = pattern.matcher(lower);
                while (m.find()) count++;
            }
            if (count > 0) found.put(entry.getKey(), count);
        }
        return found;
    }
}
