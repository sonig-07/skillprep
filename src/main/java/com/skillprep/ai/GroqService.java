package com.skillprep.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around Groq's OpenAI-compatible chat completions endpoint.
 * Always requests JSON-mode structured output so callers get a parseable
 * JsonNode back rather than free text to regex-scrape.
 */
@Service
public class GroqService {

    private static final Logger log = LoggerFactory.getLogger(GroqService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public GroqService(RestTemplate restTemplate,
                        ObjectMapper objectMapper,
                        @Value("${groq.api-key}") String apiKey,
                        @Value("${groq.base-url}") String baseUrl,
                        @Value("${groq.model}") String model) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    /**
     * Sends a single-turn prompt and returns the parsed JSON body of the model's reply.
     * The prompt itself must instruct the model to respond with strict JSON.
     */
    public JsonNode completeJson(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "GROQ_API_KEY is not configured. Set the GROQ_API_KEY environment variable.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.4,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            JsonNode response = restTemplate.postForObject(baseUrl, request, JsonNode.class);
            if (response == null) {
                throw new IllegalStateException("Empty response from Groq API");
            }
            String content = response.at("/choices/0/message/content").asText();
            return objectMapper.readTree(content);
        } catch (Exception e) {
            log.error("Groq API call failed", e);
            throw new IllegalStateException("AI request failed: " + e.getMessage(), e);
        }
    }

    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are a precise, honest technical interview assistant. Always respond with strict, valid JSON " +
            "only — no markdown code fences, no preamble, no trailing commentary. If a field has no value, use " +
            "null or an empty array as appropriate, never omit required fields.";

    public JsonNode completeJson(String userPrompt) {
        return completeJson(DEFAULT_SYSTEM_PROMPT, userPrompt);
    }
}
