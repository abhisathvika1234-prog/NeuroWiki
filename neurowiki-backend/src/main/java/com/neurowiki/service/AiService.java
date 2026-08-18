package com.neurowiki.service;

import com.neurowiki.dto.AiQuestionRequest;
import com.neurowiki.dto.AiResponse;
import com.neurowiki.dto.RagSourceDto;
import com.neurowiki.entity.AiHistory;
import com.neurowiki.entity.User;
import com.neurowiki.repository.AiHistoryRepository;
import com.neurowiki.security.SecurityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiService {

    private static final Logger logger =
            LoggerFactory.getLogger(AiService.class);

    /*
     * ============================================================
     * AI CONFIGURATION
     * ============================================================
     */

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.model:gemini-flash-latest}")
    private String aiModel;

    @Value("${ai.base.url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    /*
     * ============================================================
     * DEPENDENCIES
     * ============================================================
     */

    private final AiHistoryRepository aiHistoryRepository;
    private final SecurityUtils securityUtils;
    private final RagService ragService;
    private final RestTemplate restTemplate;

    public AiService(
            AiHistoryRepository aiHistoryRepository,
            SecurityUtils securityUtils,
            RagService ragService
    ) {
        this.aiHistoryRepository = aiHistoryRepository;
        this.securityUtils = securityUtils;
        this.ragService = ragService;
        this.restTemplate = new RestTemplate();
    }

    /*
     * ============================================================
     * CHECK AI CONFIGURATION
     * ============================================================
     */

    public boolean isAiConfigured() {

        boolean configured =
                apiKey != null &&
                !apiKey.trim().isBlank();

        /*
         * IMPORTANT:
         * Never log the actual API key.
         */
        logger.info(
                "AI configuration check: configured={}, model={}, baseUrl={}",
                configured,
                aiModel,
                baseUrl
        );

        return configured;
    }

    /*
     * ============================================================
     * ASK AI
     * ============================================================
     */

    public AiResponse askQuestion(AiQuestionRequest request) {

        User user = securityUtils.getCurrentAuthenticatedUser();

        if (request == null || request.getQuestion() == null) {

            return AiResponse.builder()
                    .question("")
                    .answer("Please enter a question.")
                    .sources(Collections.emptyList())
                    .serviceConfigured(isAiConfigured())
                    .build();
        }

        String question = request.getQuestion().trim();

        if (question.isBlank()) {

            return AiResponse.builder()
                    .question("")
                    .answer("Please enter a question.")
                    .sources(Collections.emptyList())
                    .serviceConfigured(isAiConfigured())
                    .build();
        }

        /*
         * ========================================================
         * CHECK GEMINI CONFIGURATION
         * ========================================================
         */

        if (!isAiConfigured()) {

            logger.error(
                    "AI service is NOT configured. ai.api.key is empty."
            );

            String message =
                    "AI service is not configured. " +
                    "Set AI_API_KEY in application.properties " +
                    "or environment variables.";

            AiHistory history = AiHistory.builder()
                    .question(question)
                    .answer(message)
                    .user(user)
                    .build();

            AiHistory saved =
                    aiHistoryRepository.save(history);

            return AiResponse.builder()
                    .id(saved.getId())
                    .question(saved.getQuestion())
                    .answer(message)
                    .sources(Collections.emptyList())
                    .timestamp(saved.getTimestamp())
                    .serviceConfigured(false)
                    .build();
        }

        /*
         * ========================================================
         * RAG RETRIEVAL
         * ========================================================
         */

        List<RagService.RetrievedChunk> retrievedChunks;

        try {

            retrievedChunks =
                    ragService.retrieveRelevantChunks(
                            user,
                            question,
                            5
                    );

        } catch (Exception e) {

            logger.error(
                    "RAG retrieval failed: {}",
                    e.getMessage(),
                    e
            );

            String message =
                    "I couldn't retrieve information from your " +
                    "NeuroWiki knowledge base.";

            AiHistory history = AiHistory.builder()
                    .question(question)
                    .answer(message)
                    .user(user)
                    .build();

            AiHistory saved =
                    aiHistoryRepository.save(history);

            return AiResponse.builder()
                    .id(saved.getId())
                    .question(saved.getQuestion())
                    .answer(message)
                    .sources(Collections.emptyList())
                    .timestamp(saved.getTimestamp())
                    .serviceConfigured(true)
                    .build();
        }

        /*
         * ========================================================
         * NO RAG RESULTS
         * ========================================================
         */

        if (retrievedChunks == null || retrievedChunks.isEmpty()) {

            String defaultAnswer =
                    "I couldn't find enough information in your " +
                    "NeuroWiki knowledge base to answer that.";

            logger.info(
                    "No RAG chunks found for question: {}",
                    question
            );

            AiHistory history = AiHistory.builder()
                    .question(question)
                    .answer(defaultAnswer)
                    .user(user)
                    .build();

            AiHistory saved =
                    aiHistoryRepository.save(history);

            return AiResponse.builder()
                    .id(saved.getId())
                    .question(saved.getQuestion())
                    .answer(defaultAnswer)
                    .sources(Collections.emptyList())
                    .timestamp(saved.getTimestamp())
                    .serviceConfigured(true)
                    .build();
        }

        /*
         * ========================================================
         * BUILD RAG CONTEXT
         * ========================================================
         */

        StringBuilder contextBuilder =
                new StringBuilder();

        List<RagSourceDto> sourcesList =
                new ArrayList<>();

        Set<String> seenSources =
                new HashSet<>();

        for (int i = 0;
             i < retrievedChunks.size();
             i++) {

            RagService.RetrievedChunk rc =
                    retrievedChunks.get(i);

            contextBuilder.append(
                    String.format(
                            "[Source %d: %s (%s)]\n%s\n\n",
                            i + 1,
                            rc.getSourceTitle(),
                            rc.getSourceType(),
                            rc.getContent()
                    )
            );

            String sourceKey =
                    rc.getSourceType()
                            + ":"
                            + rc.getSourceId();

            if (!seenSources.contains(sourceKey)) {

                seenSources.add(sourceKey);

                sourcesList.add(
                        RagSourceDto.builder()
                                .type(rc.getSourceType())
                                .title(rc.getSourceTitle())
                                .id(rc.getSourceId())
                                .build()
                );
            }
        }

        /*
         * ========================================================
         * CALL GEMINI
         * ========================================================
         */

        String aiAnswer =
                callGeminiGenerateContentApi(
                        question,
                        contextBuilder.toString()
                );

        /*
         * ========================================================
         * SAVE HISTORY
         * ========================================================
         */

        AiHistory history = AiHistory.builder()
                .question(question)
                .answer(aiAnswer)
                .user(user)
                .build();

        AiHistory saved =
                aiHistoryRepository.save(history);

        /*
         * ========================================================
         * RETURN RESPONSE
         * ========================================================
         */

        return AiResponse.builder()
                .id(saved.getId())
                .question(saved.getQuestion())
                .answer(aiAnswer)
                .sources(sourcesList)
                .timestamp(saved.getTimestamp())
                .serviceConfigured(true)
                .build();
    }

    /*
     * ============================================================
     * GEMINI GENERATE CONTENT API
     * ============================================================
     */

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("(?i)key=[^&\\s]+", "key=REDACTED");
    }

    private String callGeminiGenerateContentApi(
            String question,
            String context
    ) {

        if (!isAiConfigured()) {
            logger.error("Gemini API call skipped because API key is missing. Ensure GEMINI_API_KEY or AI_API_KEY is set in environment.");
            return "AI service is not configured. Set GEMINI_API_KEY or AI_API_KEY in environment variables.";
        }

        String cleanBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        if (cleanBaseUrl.endsWith("/")) {
            cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 1);
        }

        String cleanModel = aiModel == null ? "" : aiModel.trim();
        if (cleanModel.startsWith("models/")) {
            cleanModel = cleanModel.substring("models/".length());
        }

        String url = String.format(
                "%s/models/%s:generateContent?key=%s",
                cleanBaseUrl,
                cleanModel,
                apiKey.trim()
        );

        logger.info("Calling Gemini API. Model={}, BaseUrl={}", cleanModel, cleanBaseUrl);

        String systemPrompt =
                "You are the NeuroWiki Neural AI Assistant.\n\n" +
                "Your job is to answer questions using ONLY the provided NeuroWiki knowledge base context.\n\n" +
                "Rules:\n" +
                "1. Use only the supplied NeuroWiki context.\n" +
                "2. Do not invent facts or hallucinate information.\n" +
                "3. If the context does not contain enough information, say exactly:\n" +
                "\"I couldn't find enough information in your NeuroWiki knowledge base to answer that.\"\n";

        String userPrompt =
                "CONTEXT:\n" + context + "\n\nQUESTION:\n" + question;

        Map<String, Object> systemPart = Map.of("text", systemPrompt);
        Map<String, Object> systemInstruction = Map.of("parts", List.of(systemPart));
        Map<String, Object> userPart = Map.of("text", userPrompt);
        Map<String, Object> contentMap = Map.of("role", "user", "parts", List.of(userPart));

        Map<String, Object> requestBody = Map.of(
                "system_instruction", systemInstruction,
                "contents", List.of(contentMap)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            logger.info("Gemini API HTTP response status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map body = response.getBody();
                Object candidatesObject = body.get("candidates");

                if (candidatesObject instanceof List<?> candidates && !candidates.isEmpty()) {
                    Object candidateObject = candidates.get(0);
                    if (candidateObject instanceof Map<?, ?> candidate) {
                        Object contentObject = candidate.get("content");
                        if (contentObject instanceof Map<?, ?> content) {
                            Object partsObject = content.get("parts");
                            if (partsObject instanceof List<?> parts && !parts.isEmpty()) {
                                Object firstPartObject = parts.get(0);
                                if (firstPartObject instanceof Map<?, ?> firstPart) {
                                    Object textObject = firstPart.get("text");
                                    if (textObject instanceof String text && !text.trim().isBlank()) {
                                        logger.info("Gemini text response successfully received and parsed.");
                                        return text.trim();
                                    }
                                }
                            }
                        }
                    }
                }

                logger.warn("Gemini returned HTTP 200 OK but no answer text was found in response payload.");
                return "Gemini AI returned an empty response.";
            }

            logger.error("Gemini API non-OK status: {}", response.getStatusCode());
            return "The AI service returned HTTP " + response.getStatusCode() + ".";
        }
        catch (HttpStatusCodeException e) {
            int statusCode = e.getStatusCode().value();
            String errorMsg = sanitize(e.getResponseBodyAsString());
            logger.error("Gemini API HTTP Error. Status: {}, Model: {}, Response: {}", statusCode, cleanModel, errorMsg);

            if (statusCode == 401) {
                return "Gemini API authentication failed (401). Please check your GEMINI_API_KEY or AI_API_KEY environment variable.";
            } else if (statusCode == 403) {
                return "Gemini API access forbidden (403). Please verify your API key permissions and project quota.";
            } else if (statusCode == 400) {
                return "Invalid request sent to Gemini AI service (400). Please check model parameters.";
            } else if (statusCode == 404) {
                return "The requested Gemini model or endpoint ('" + cleanModel + "') was not found (404).";
            } else if (statusCode == 429) {
                return "Gemini API rate limit or quota exceeded (429). Please try again later.";
            } else if (statusCode >= 500) {
                return "Gemini AI server encountered an error (HTTP " + statusCode + "). Please try again later.";
            }
            return "Gemini AI request failed with HTTP status " + statusCode + ".";
        }
        catch (ResourceAccessException e) {
            logger.error("Network error / connection timeout calling Gemini API: {}", sanitize(e.getMessage()));
            return "Unable to connect to Gemini AI service due to a network connection or timeout issue.";
        }
        catch (Exception e) {
            logger.error("Unexpected error during Gemini API execution: {}", sanitize(e.getMessage()), e);
            return "An unexpected error occurred while communicating with Gemini AI.";
        }
    }

    /*
     * ============================================================
     * AI HISTORY
     * ============================================================
     */

    public List<AiResponse> getHistory() {

        User user =
                securityUtils.getCurrentAuthenticatedUser();

        List<AiHistory> histories =
                aiHistoryRepository
                        .findByUserOrderByTimestampDesc(user);

        boolean configured =
                isAiConfigured();

        return histories.stream()
                .map(h ->
                        AiResponse.builder()
                                .id(h.getId())
                                .question(h.getQuestion())
                                .answer(h.getAnswer())
                                .sources(Collections.emptyList())
                                .timestamp(h.getTimestamp())
                                .serviceConfigured(configured)
                                .build()
                )
                .collect(Collectors.toList());
    }
}