package com.neurowiki.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Embedding service for NeuroWiki RAG.
 *
 * Uses Google's Gemini Embedding API with:
 *   gemini-embedding-001
 *
 * The embedding size is kept at 256 dimensions so it remains
 * compatible with NeuroWiki's existing vector storage.
 */
@Service
public class EmbeddingService {

    private static final Logger logger =
            LoggerFactory.getLogger(EmbeddingService.class);

    /**
     * Keep this aligned with the outputDimensionality sent to Gemini.
     *
     * Gemini Embedding 001 supports 256 dimensions.
     */
    private static final int VECTOR_DIM = 256;

    @Value("${ai.api.key:}")
    private String apiKey;

    /**
     * Default embedding model.
     *
     * Do NOT use text-embedding-004.
     */
    @Value("${embedding.model:gemini-embedding-001}")
    private String embeddingModel;

    @Value("${ai.base.url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public EmbeddingService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Generates an embedding for a document/chunk.
     *
     * This method is used by the ingestion pipeline.
     */
    public float[] generateEmbedding(String text) {

        if (text == null || text.isBlank()) {
            return new float[VECTOR_DIM];
        }

        if (apiKey == null || apiKey.trim().isBlank()) {
            logger.warn("Gemini API key is missing. Using local feature vector.");
            return generateLocalFeatureVector(text);
        }

        try {

            float[] geminiVector = callGeminiEmbeddingApi(
                    text,
                    "RETRIEVAL_DOCUMENT"
            );

            if (geminiVector != null && geminiVector.length > 0) {

                logger.debug(
                        "Generated Gemini document embedding with {} dimensions",
                        geminiVector.length
                );

                return geminiVector;
            }

        } catch (Exception e) {

            logger.error(
                    "Gemini Embedding API failed. Falling back to local vector. Error: {}",
                    e.getMessage()
            );
        }

        return generateLocalFeatureVector(text);
    }

    /**
     * Generates an embedding specifically for a user search query.
     *
     * This is important for RAG:
     *
     * Document:
     *     RETRIEVAL_DOCUMENT
     *
     * User question:
     *     RETRIEVAL_QUERY
     */
    public float[] generateQueryEmbedding(String query) {

        if (query == null || query.isBlank()) {
            return new float[VECTOR_DIM];
        }

        if (apiKey == null || apiKey.trim().isBlank()) {
            logger.warn("Gemini API key is missing. Using local query vector.");
            return generateLocalFeatureVector(query);
        }

        try {

            float[] geminiVector = callGeminiEmbeddingApi(
                    query,
                    "RETRIEVAL_QUERY"
            );

            if (geminiVector != null && geminiVector.length > 0) {

                logger.debug(
                        "Generated Gemini query embedding with {} dimensions",
                        geminiVector.length
                );

                return geminiVector;
            }

        } catch (Exception e) {

            logger.error(
                    "Gemini Query Embedding API failed. Falling back to local vector. Error: {}",
                    e.getMessage()
            );
        }

        return generateLocalFeatureVector(query);
    }

    /**
     * Calls Gemini's embedContent REST API.
     */
    private float[] callGeminiEmbeddingApi(
            String text,
            String taskType
    ) {

        String normalizedBaseUrl = baseUrl;

        if (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl =
                    normalizedBaseUrl.substring(
                            0,
                            normalizedBaseUrl.length() - 1
                    );
        }

        String normalizedModel = embeddingModel;

        if (normalizedModel.startsWith("models/")) {
            normalizedModel =
                    normalizedModel.substring("models/".length());
        }

        String url = String.format(
                "%s/models/%s:embedContent?key=%s",
                normalizedBaseUrl,
                normalizedModel,
                apiKey.trim()
        );

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        /*
         * Gemini embedding request:
         *
         * {
         *   "model": "models/gemini-embedding-001",
         *   "content": {
         *      "parts": [
         *          {
         *              "text": "..."
         *          }
         *      ]
         *   },
         *   "taskType": "RETRIEVAL_DOCUMENT",
         *   "outputDimensionality": 256
         * }
         */

        Map<String, Object> textPart =
                Map.of("text", text);

        Map<String, Object> content =
                Map.of(
                        "parts",
                        List.of(textPart)
                );

        Map<String, Object> requestBody =
                new LinkedHashMap<>();

        requestBody.put(
                "model",
                "models/" + normalizedModel
        );

        requestBody.put(
                "content",
                content
        );

        requestBody.put(
                "taskType",
                taskType
        );

        requestBody.put(
                "outputDimensionality",
                VECTOR_DIM
        );

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(
                        requestBody,
                        headers
                );

        logger.debug(
                "Calling Gemini Embedding API. Model={}, Task={}, Dimensions={}",
                normalizedModel,
                taskType,
                VECTOR_DIM
        );

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        url,
                        entity,
                        Map.class
                );

        if (response.getStatusCode() != HttpStatus.OK) {

            logger.warn(
                    "Gemini embedding API returned HTTP status: {}",
                    response.getStatusCode()
            );

            return null;
        }

        if (response.getBody() == null) {

            logger.warn(
                    "Gemini embedding API returned an empty response."
            );

            return null;
        }

        Map body = response.getBody();

        /*
         * Expected response:
         *
         * {
         *   "embedding": {
         *      "values": [...]
         *   }
         * }
         */

        Object embeddingObject =
                body.get("embedding");

        if (!(embeddingObject instanceof Map)) {

            logger.warn(
                    "Gemini response does not contain an embedding object."
            );

            logger.debug(
                    "Gemini response body: {}",
                    body
            );

            return null;
        }

        Map embeddingMap =
                (Map) embeddingObject;

        Object valuesObject =
                embeddingMap.get("values");

        if (!(valuesObject instanceof List)) {

            logger.warn(
                    "Gemini embedding response does not contain values."
            );

            return null;
        }

        List<?> values =
                (List<?>) valuesObject;

        if (values.isEmpty()) {

            logger.warn(
                    "Gemini returned an empty embedding vector."
            );

            return null;
        }

        float[] vector =
                new float[values.size()];

        for (int i = 0; i < values.size(); i++) {

            Object value = values.get(i);

            if (value instanceof Number) {

                vector[i] =
                        ((Number) value).floatValue();

            } else {

                vector[i] = 0.0f;
            }
        }

        /*
         * Gemini should return exactly VECTOR_DIM dimensions
         * because outputDimensionality is explicitly set.
         */
        if (vector.length != VECTOR_DIM) {

            logger.warn(
                    "Unexpected Gemini embedding dimension. Expected={}, Actual={}",
                    VECTOR_DIM,
                    vector.length
            );
        }

        /*
         * Gemini Embedding 001 requires manual normalization
         * when using reduced dimensions.
         */
        normalize(vector);

        return vector;
    }

    /**
     * L2-normalizes an embedding vector.
     */
    private void normalize(float[] vector) {

        if (vector == null || vector.length == 0) {
            return;
        }

        double norm = 0.0;

        for (float value : vector) {
            norm += value * value;
        }

        norm = Math.sqrt(norm);

        if (norm == 0.0) {
            return;
        }

        for (int i = 0; i < vector.length; i++) {
            vector[i] =
                    (float) (vector[i] / norm);
        }
    }

    /**
     * Local deterministic fallback.
     *
     * This is only used when Gemini embedding generation fails.
     */
    public float[] generateLocalFeatureVector(String text) {

        float[] vector =
                new float[VECTOR_DIM];

        if (text == null || text.isBlank()) {
            return vector;
        }

        String cleaned =
                text
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9\\s]", " ");

        String[] words =
                cleaned.split("\\s+");

        for (String word : words) {

            if (word.isBlank()) {
                continue;
            }

            int hash =
                    Math.floorMod(
                            word.hashCode(),
                            VECTOR_DIM
                    );

            vector[hash] += 1.0f;

            /*
             * Character trigrams help preserve
             * some contextual similarity.
             */
            if (word.length() >= 3) {

                for (int i = 0;
                     i <= word.length() - 3;
                     i++) {

                    String sub =
                            word.substring(i, i + 3);

                    int subHash =
                            Math.floorMod(
                                    sub.hashCode(),
                                    VECTOR_DIM
                            );

                    vector[subHash] += 0.5f;
                }
            }
        }

        normalize(vector);

        return vector;
    }

    /**
     * Calculates cosine similarity between two vectors.
     */
    public double calculateCosineSimilarity(
            float[] vecA,
            float[] vecB
    ) {

        if (vecA == null ||
                vecB == null ||
                vecA.length == 0 ||
                vecB.length == 0) {

            return 0.0;
        }

        /*
         * Different embedding models/dimensions should
         * never be compared.
         */
        if (vecA.length != vecB.length) {

            logger.warn(
                    "Cannot calculate cosine similarity for vectors with different dimensions. A={}, B={}",
                    vecA.length,
                    vecB.length
            );

            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0;
             i < vecA.length;
             i++) {

            dotProduct +=
                    vecA[i] * vecB[i];

            normA +=
                    vecA[i] * vecA[i];

            normB +=
                    vecB[i] * vecB[i];
        }

        if (normA == 0.0 ||
                normB == 0.0) {

            return 0.0;
        }

        return dotProduct /
                (Math.sqrt(normA) *
                        Math.sqrt(normB));
    }

    /**
     * Converts a vector to a comma-separated string
     * for storage in the database.
     */
    public String vectorToString(float[] vector) {

        if (vector == null ||
                vector.length == 0) {

            return "";
        }

        StringBuilder sb =
                new StringBuilder();

        for (int i = 0;
             i < vector.length;
             i++) {

            sb.append(vector[i]);

            if (i < vector.length - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    /**
     * Converts a comma-separated database string
     * back into a float vector.
     */
    public float[] stringToVector(String str) {

        if (str == null ||
                str.isBlank()) {

            return new float[0];
        }

        String[] parts =
                str.split(",");

        float[] vector =
                new float[parts.length];

        for (int i = 0;
             i < parts.length;
             i++) {

            try {

                vector[i] =
                        Float.parseFloat(
                                parts[i].trim()
                        );

            } catch (NumberFormatException e) {

                vector[i] = 0.0f;
            }
        }

        return vector;
    }
}