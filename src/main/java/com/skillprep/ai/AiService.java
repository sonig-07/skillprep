package com.skillprep.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;


@Service
public class AiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public AiService(RestTemplate restTemplate, ObjectMapper objectMapper,
                     @Value("${ai.api-key}") String apiKey,
                     @Value("${ai.base-url}") String baseUrl,
                     @Value("${ai.model}") String model) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }


    /** Resume text -> skills and projects */
    public JsonNode extractResume(String resume) {
        return ask("""
                Read this RESUME and list the skills and the named projects in it.

                RESUME:
                %s

                Reply with JSON only, in exactly this shape:
                {"skills": ["Java", "SQL"], "projects": [{"name": "project name", "description": "one short sentence"}]}
                """.formatted(cut(resume)));
    }

    /** Resume + JD -> interview questions */
    public JsonNode generateQuestions(String resume, String jd, int count, String difficulty, int level,
                                      String focusSkill, String focusProject, List<String> missingSkills) {
        String focus = "";
        if (focusSkill != null && !focusSkill.isBlank()) {
            focus += "All questions must be about the skill: " + focusSkill + ". ";
        }
        if (focusProject != null && !focusProject.isBlank()) {
            focus += "Project questions must be about the project: " + focusProject + ". ";
        }

        //note
        String gaps = missingSkills.isEmpty() ? ""
                : "Skills the job needs but the resume does not show: " + String.join(", ", missingSkills)
                  + ". Ask the SKILL_GAP questions about these skills.";

        return ask("""
                You are a technical interviewer. Write exactly %d interview questions based only on the
                RESUME and JOB DESCRIPTION below.
                The candidate has %s of experience. Difficulty: %s. For 0-1 years keep questions on fundamentals.
                %s
                %s
                Use a mix of these types:
                - GENERAL: a concept question about a skill from the resume or JD
                - PROJECT_SPECIFIC: a question about one named project from the resume (use its name)
                - SKILL_GAP: a question about a skill the JD needs but the resume does not show

                RESUME:
                %s

                JOB DESCRIPTION:
                %s

                Reply with JSON only, in exactly this shape:
                {"questions": [{"questionText": "...", "questionType": "GENERAL or PROJECT_SPECIFIC or SKILL_GAP",
                                "skillTopic": "short topic like Java", "relatedProject": "project name or null"}]}
                """.formatted(count, years(level), difficulty, focus, gaps, cut(resume), cut(jd)));
    }

    /** Question + answer -> score and feedback */
    public JsonNode evaluateAnswer(String question, String topic, String difficulty, Integer level, String answer) {
        return ask("""
                You are a technical interviewer. Score the candidate's answer to the question.
                The candidate has %s of experience, so judge fairly for that level.

                QUESTION (topic: %s, difficulty: %s):
                %s

                CANDIDATE'S ANSWER:
                %s

                Reply with JSON only, in exactly this shape (score is a whole number from 0 to 100):
                {"score": 70,
                 "strengths": ["one good point"],
                 "improvements": ["one thing to improve"],
                 "idealAnswerNotes": "2-3 sentences on what a strong answer would include"}
                """.formatted(years(level == null ? 0 : level), topic, difficulty, question, answer));
    }

    // ---------- helpers ----------

    /** Sends one prompt to the AI and returns the JSON reply. */
    private JsonNode ask(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);                      // only needed for online services like Groq
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.4,
                "response_format", Map.of("type", "json_object"),   // ask the AI for valid JSON only
                "messages", List.of(
                        Map.of("role", "system", "content", "You reply with valid JSON only. No extra text."),
                        Map.of("role", "user", "content", prompt)));

        try {
            JsonNode response = restTemplate.postForObject(baseUrl, new HttpEntity<>(body, headers), JsonNode.class);
            String content = response.at("/choices/0/message/content").asText();
            return objectMapper.readTree(onlyJson(content));
        } catch (ResourceAccessException e) {
            throw new IllegalStateException("Could not reach the AI at " + baseUrl
                    + ". If you use Ollama, start it first (run: ollama serve).");
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalStateException("The AI model '" + model
                    + "' was not found. If you use Ollama, download it first (run: ollama pull " + model + ").");
        } catch (Exception e) {
            throw new IllegalStateException("AI request failed: " + e.getMessage());
        }
    }

    /** Small local models sometimes add text or ```json fences around the JSON. Keep only the { ... } part. */
    static String onlyJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : text;
    }

    private String years(int level) {
        return level >= 5 ? "5+ years" : level + (level == 1 ? " year" : " years");
    }

    /** Keeps very long texts from going over the AI's size limit. */
    private String cut(String text) {
        return text.length() > 6000 ? text.substring(0, 6000) : text;
    }
}
