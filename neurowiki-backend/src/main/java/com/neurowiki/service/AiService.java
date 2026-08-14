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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.model:gemini-2.5-flash}")
    private String aiModel;
    @Value("${ai.base.url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    private final AiHistoryRepository aiHistoryRepository;
    private final SecurityUtils securityUtils;
    private final RagService ragService;
    private final RestTemplate restTemplate;

    public AiService(AiHistoryRepository aiHistoryRepository, SecurityUtils securityUtils, RagService ragService) {
        this.aiHistoryRepository = aiHistoryRepository;
        this.securityUtils = securityUtils;
        this.ragService = ragService;
        this.restTemplate = new RestTemplate();
    }

    public boolean isAiConfigured() {
        return apiKey != null && !apiKey.trim().isBlank();
    }

    public AiResponse askQuestion(AiQuestionRequest request) {
        User user = securityUtils.getCurrentAuthenticatedUser();
        String question = request.getQuestion().trim();

        if (!isAiConfigured()) {
            AiHistory history = AiHistory.builder()
                    .question(question)
                    .answer("AI service is not configured.")
                    .user(user)
                    .build();
            AiHistory saved = aiHistoryRepository.save(history);

            return AiResponse.builder()
                    .id(saved.getId())
                    .question(saved.getQuestion())
                    .answer("AI service is not configured.")
                    .sources(Collections.emptyList())
                    .timestamp(saved.getTimestamp())
                    .serviceConfigured(false)
                    .build();
        }

        // 1. Retrieve RAG chunks
        List<RagService.RetrievedChunk> retrievedChunks = ragService.retrieveRelevantChunks(user, question, 5);

        if (retrievedChunks.isEmpty()) {
            String defaultAnswer = "I couldn't find enough information in your NeuroWiki knowledge base to answer that.";
            AiHistory history = AiHistory.builder()
                    .question(question)
                    .answer(defaultAnswer)
                    .user(user)
                    .build();
            AiHistory saved = aiHistoryRepository.save(history);

            return AiResponse.builder()
                    .id(saved.getId())
                    .question(saved.getQuestion())
                    .answer(defaultAnswer)
                    .sources(Collections.emptyList())
                    .timestamp(saved.getTimestamp())
                    .serviceConfigured(true)
                    .build();
        }

        // 2. Build RAG prompt with retrieved context
        StringBuilder contextBuilder = new StringBuilder();
        List<RagSourceDto> sourcesList = new ArrayList<>();
        Set<String> seenSources = new HashSet<>();

        for (int i = 0; i < retrievedChunks.size(); i++) {
            RagService.RetrievedChunk rc = retrievedChunks.get(i);
            contextBuilder.append(String.format("[Source %d: %s (%s)]\n%s\n\n",
                    i + 1, rc.getSourceTitle(), rc.getSourceType(), rc.getContent()));

            String sourceKey = rc.getSourceType() + ":" + rc.getSourceId();
            if (!seenSources.contains(sourceKey)) {
                seenSources.add(sourceKey);
                sourcesList.add(RagSourceDto.builder()
                        .type(rc.getSourceType())
                        .title(rc.getSourceTitle())
                        .id(rc.getSourceId())
                        .build());
            }
        }

        String ragAnswer = callGeminiGenerateContentApi(question, contextBuilder.toString());

        AiHistory history = AiHistory.builder()
                .question(question)
                .answer(ragAnswer)
                .user(user)
                .build();
        AiHistory saved = aiHistoryRepository.save(history);

        return AiResponse.builder()
                .id(saved.getId())
                .question(saved.getQuestion())
                .answer(ragAnswer)
                .sources(sourcesList)
                .timestamp(saved.getTimestamp())
                .serviceConfigured(true)
                .build();
    }

    private String callGeminiGenerateContentApi(String question, String context) {
        String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, aiModel, apiKey.trim());

        String systemPrompt = "You are the NeuroWiki Neural AI Assistant.\n" +
                "Answer the user's question using ONLY the provided NeuroWiki context below.\n" +
                "If the context does not contain enough information to answer the question, state exactly:\n" +
                "\"I couldn't find enough information in your NeuroWiki knowledge base to answer that.\"\n" +
                "Do not hallucinate facts outside the provided context.";

        String userPrompt = String.format("CONTEXT:\n%s\nQUESTION:\n%s", context, question);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> systemPart = Map.of("text", systemPrompt);
        Map<String, Object> systemInstruction = Map.of("parts", List.of(systemPart));

        Map<String, Object> userPart = Map.of("text", userPrompt);
        Map<String, Object> contentMap = Map.of("role", "user", "parts", List.of(userPart));

        Map<String, Object> requestBody = Map.of(
                "system_instruction", systemInstruction,
                "contents", List.of(contentMap));

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map body = response.getBody();
                if (body.containsKey("candidates")) {
                    List candidates = (List) body.get("candidates");
                    if (!candidates.isEmpty()) {
                        Map firstCandidate = (Map) candidates.get(0);
                        if (firstCandidate.containsKey("content")) {
                            Map contentObj = (Map) firstCandidate.get("content");
                            if (contentObj.containsKey("parts")) {
                                List parts = (List) contentObj.get("parts");
                                if (!parts.isEmpty()) {
                                    Map firstPart = (Map) parts.get(0);
                                    if (firstPart.containsKey("text")) {
                                        return ((String) firstPart.get("text")).trim();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Gemini GenerateContent API error: {}", e.getMessage(), e);
            return "I couldn't find enough information in your NeuroWiki knowledge base to answer that.";
        }

        return "I couldn't find enough information in your NeuroWiki knowledge base to answer that.";
    }

    public List<AiResponse> getHistory() {
        User user = securityUtils.getCurrentAuthenticatedUser();
        List<AiHistory> histories = aiHistoryRepository.findByUserOrderByTimestampDesc(user);
        boolean configured = isAiConfigured();

        return histories.stream()
                .map(h -> AiResponse.builder()
                        .id(h.getId())
                        .question(h.getQuestion())
                        .answer(h.getAnswer())
                        .sources(Collections.emptyList())
                        .timestamp(h.getTimestamp())
                        .serviceConfigured(configured)
                        .build())
                .collect(Collectors.toList());
    }
}
