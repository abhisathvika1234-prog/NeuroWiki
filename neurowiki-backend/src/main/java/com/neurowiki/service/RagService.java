package com.neurowiki.service;

import com.neurowiki.entity.RagChunk;
import com.neurowiki.entity.User;
import com.neurowiki.repository.RagChunkRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger logger =
            LoggerFactory.getLogger(RagService.class);

    /*
     * Maximum number of words in one RAG chunk.
     */
    private static final int CHUNK_SIZE_WORDS = 500;

    /*
     * Number of overlapping words between chunks.
     */
    private static final int OVERLAP_WORDS = 75;

    /*
     * Minimum similarity required for a chunk
     * to be considered relevant.
     */
    private static final double MIN_SIMILARITY = 0.10;

    private final RagChunkRepository ragChunkRepository;
    private final EmbeddingService embeddingService;

    public RagService(
            RagChunkRepository ragChunkRepository,
            EmbeddingService embeddingService
    ) {
        this.ragChunkRepository = ragChunkRepository;
        this.embeddingService = embeddingService;
    }

    /**
     * Processes a document/webpage and creates RAG chunks.
     *
     * Flow:
     *
     * Webpage/PDF text
     *       ↓
     * Smart chunks
     *       ↓
     * Gemini RETRIEVAL_DOCUMENT embedding
     *       ↓
     * Save chunks in database
     */
    @Transactional
    public void processAndChunkContent(
            User user,
            String sourceType,
            Long sourceId,
            String sourceTitle,
            String fullText
    ) {

        if (user == null) {
            logger.warn("Cannot process RAG content: user is null.");
            return;
        }

        if (fullText == null || fullText.isBlank()) {
            logger.warn(
                    "Cannot process RAG content: empty content. Source={}",
                    sourceTitle
            );
            return;
        }

        logger.info(
                "Starting RAG processing. User={}, sourceType={}, sourceId={}, title={}, characters={}",
                user.getUsername(),
                sourceType,
                sourceId,
                sourceTitle,
                fullText.length()
        );

        /*
         * Delete previous chunks for the same source.
         *
         * This prevents duplicate embeddings when a document
         * or webpage is uploaded again.
         */
        try {

            ragChunkRepository
                    .deleteByUserAndSourceTypeAndSourceId(
                            user,
                            sourceType,
                            sourceId
                    );

        } catch (Exception e) {

            logger.warn(
                    "Could not delete previous RAG chunks. sourceId={}, error={}",
                    sourceId,
                    e.getMessage()
            );
        }

        /*
         * Split the complete document into manageable chunks.
         */
        List<String> textChunks =
                createSmartChunks(fullText);

        if (textChunks.isEmpty()) {

            logger.warn(
                    "No chunks were created for source={}",
                    sourceTitle
            );

            return;
        }

        logger.info(
                "Created {} chunks for source={}",
                textChunks.size(),
                sourceTitle
        );

        List<RagChunk> ragChunks =
                new ArrayList<>();

        /*
         * Generate an embedding for every document chunk.
         *
         * EmbeddingService.generateEmbedding()
         * internally uses:
         *
         * RETRIEVAL_DOCUMENT
         */
        for (int i = 0;
             i < textChunks.size();
             i++) {

            String chunkText =
                    textChunks.get(i);

            if (chunkText == null ||
                    chunkText.isBlank()) {

                continue;
            }

            try {

                float[] vector =
                        embeddingService.generateEmbedding(
                                chunkText
                        );

                if (vector == null ||
                        vector.length == 0) {

                    logger.warn(
                            "Empty embedding generated for chunk {} of source={}",
                            i,
                            sourceTitle
                    );

                    continue;
                }

                String vectorStr =
                        embeddingService.vectorToString(
                                vector
                        );

                RagChunk chunk =
                        RagChunk.builder()
                                .user(user)
                                .sourceType(sourceType)
                                .sourceId(sourceId)
                                .sourceTitle(
                                        sourceTitle != null &&
                                                !sourceTitle.isBlank()
                                                ? sourceTitle
                                                : "Untitled Document"
                                )
                                .chunkIndex(i)
                                .content(chunkText)
                                .embeddingData(vectorStr)
                                .build();

                ragChunks.add(chunk);

                logger.debug(
                        "Created RAG chunk {} for source={} with vector dimensions={}",
                        i,
                        sourceTitle,
                        vector.length
                );

            } catch (Exception e) {

                logger.error(
                        "Failed to generate embedding for chunk {} of source={}: {}",
                        i,
                        sourceTitle,
                        e.getMessage()
                );
            }
        }

        /*
         * Save all successfully generated chunks.
         */
        if (!ragChunks.isEmpty()) {

            ragChunkRepository.saveAll(
                    ragChunks
            );

            logger.info(
                    "Saved {} RAG chunks for user={}, sourceType={}, sourceId={}, title={}",
                    ragChunks.size(),
                    user.getUsername(),
                    sourceType,
                    sourceId,
                    sourceTitle
            );

        } else {

            logger.warn(
                    "No RAG chunks were saved for source={}",
                    sourceTitle
            );
        }
    }

    /**
     * Retrieves the most relevant chunks for a user question.
     *
     * Important:
     *
     * The QUESTION uses RETRIEVAL_QUERY.
     *
     * Previously this method incorrectly called:
     *
     *     generateEmbedding(question)
     *
     * which is intended for documents.
     *
     * Now it calls:
     *
     *     generateQueryEmbedding(question)
     */
    public List<RetrievedChunk> retrieveRelevantChunks(
            User user,
            String question,
            int topK
    ) {

        if (user == null) {

            logger.warn(
                    "Cannot retrieve RAG chunks: user is null."
            );

            return Collections.emptyList();
        }

        if (question == null ||
                question.isBlank()) {

            logger.warn(
                    "Cannot retrieve RAG chunks: question is empty."
            );

            return Collections.emptyList();
        }

        /*
         * Protect against invalid topK values.
         */
        if (topK <= 0) {
            topK = 5;
        }

        logger.info(
                "RAG retrieval started. User={}, question={}, topK={}",
                user.getUsername(),
                question,
                topK
        );

        /*
         * Get all chunks belonging to the current user.
         */
        List<RagChunk> userChunks =
                ragChunkRepository.findByUser(user);

        if (userChunks.isEmpty()) {

            logger.warn(
                    "No RAG chunks found for user={}",
                    user.getUsername()
            );

            return Collections.emptyList();
        }

        logger.info(
                "Found {} RAG chunks for user={}",
                userChunks.size(),
                user.getUsername()
        );

        /*
         * IMPORTANT:
         *
         * User question → RETRIEVAL_QUERY
         */
        float[] questionVector =
                embeddingService.generateQueryEmbedding(
                        question
                );

        if (questionVector == null ||
                questionVector.length == 0) {

            logger.warn(
                    "Could not generate question embedding."
            );

            return Collections.emptyList();
        }

        logger.debug(
                "Question embedding generated with {} dimensions.",
                questionVector.length
        );

        List<RetrievedChunk> rankedChunks =
                new ArrayList<>();

        /*
         * Compare the question embedding against
         * every document chunk.
         */
        for (RagChunk chunk : userChunks) {

            if (chunk == null) {
                continue;
            }

            String embeddingData =
                    chunk.getEmbeddingData();

            if (embeddingData == null ||
                    embeddingData.isBlank()) {

                logger.debug(
                        "Skipping chunk {} because embedding is empty.",
                        chunk.getChunkIndex()
                );

                continue;
            }

            float[] chunkVector =
                    embeddingService.stringToVector(
                            embeddingData
                    );

            if (chunkVector.length == 0) {
                continue;
            }

            /*
             * Do not compare vectors generated from
             * incompatible dimensions.
             */
            if (questionVector.length !=
                    chunkVector.length) {

                logger.warn(
                        "Skipping chunk {} from {} because vector dimensions differ. Question={}, Chunk={}",
                        chunk.getChunkIndex(),
                        chunk.getSourceTitle(),
                        questionVector.length,
                        chunkVector.length
                );

                continue;
            }

            double similarity =
                    embeddingService.calculateCosineSimilarity(
                            questionVector,
                            chunkVector
                    );

            rankedChunks.add(
                    RetrievedChunk.builder()
                            .chunk(chunk)
                            .similarityScore(similarity)
                            .sourceType(chunk.getSourceType())
                            .sourceId(chunk.getSourceId())
                            .sourceTitle(chunk.getSourceTitle())
                            .content(chunk.getContent())
                            .build()
            );
        }

        /*
         * Highest similarity first.
         */
        rankedChunks.sort(
                (a, b) ->
                        Double.compare(
                                b.getSimilarityScore(),
                                a.getSimilarityScore()
                        )
        );

        /*
         * Log the top retrieved chunks.
         *
         * This is extremely useful for debugging
         * your GFG RAG problem.
         */
        logger.info(
                "Top RAG results for question: {}",
                question
        );

        rankedChunks.stream()
                .limit(Math.min(topK, 5))
                .forEach(result ->
                        logger.info(
                                "RAG RESULT -> source={}, chunk={}, similarity={}",
                                result.getSourceTitle(),
                                result.getChunk().getChunkIndex(),
                                String.format(
                                        Locale.US,
                                        "%.4f",
                                        result.getSimilarityScore()
                                )
                        )
                );

        /*
         * Only return reasonably relevant chunks.
         */
        List<RetrievedChunk> results =
                rankedChunks.stream()
                        .filter(
                                chunk ->
                                        chunk.getSimilarityScore()
                                                >= MIN_SIMILARITY
                        )
                        .limit(topK)
                        .collect(Collectors.toList());

        logger.info(
                "RAG retrieval completed. Returning {} relevant chunks.",
                results.size()
        );

        /*
         * Print the actual sources being returned.
         */
        results.forEach(result ->
                logger.info(
                        "RETRIEVED SOURCE -> {} | similarity={}",
                        result.getSourceTitle(),
                        String.format(
                                Locale.US,
                                "%.4f",
                                result.getSimilarityScore()
                        )
                )
        );

        return results;
    }

    /**
     * Splits long content into overlapping chunks.
     *
     * Example:
     *
     * Chunk 1:
     * words 1 - 500
     *
     * Chunk 2:
     * words 426 - 925
     *
     * Chunk 3:
     * words 851 - 1350
     *
     * The overlap helps preserve context across
     * chunk boundaries.
     */
    private List<String> createSmartChunks(
            String text
    ) {

        if (text == null ||
                text.isBlank()) {

            return Collections.emptyList();
        }

        String cleanedText =
                text.trim();

        String[] words =
                cleanedText.split("\\s+");

        List<String> chunks =
                new ArrayList<>();

        /*
         * Small document.
         */
        if (words.length <=
                CHUNK_SIZE_WORDS) {

            chunks.add(
                    cleanedText
            );

            return chunks;
        }

        int step =
                CHUNK_SIZE_WORDS -
                        OVERLAP_WORDS;

        for (
                int start = 0;
                start < words.length;
                start += step
        ) {

            int end =
                    Math.min(
                            start +
                                    CHUNK_SIZE_WORDS,
                            words.length
                    );

            StringBuilder sb =
                    new StringBuilder();

            for (
                    int i = start;
                    i < end;
                    i++
            ) {

                sb.append(
                        words[i]
                );

                if (i < end - 1) {
                    sb.append(" ");
                }
            }

            String chunk =
                    sb.toString().trim();

            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            /*
             * Stop when we reach the end.
             */
            if (end >= words.length) {
                break;
            }
        }

        return chunks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievedChunk {

        private RagChunk chunk;

        private double similarityScore;

        private String sourceType;

        private Long sourceId;

        private String sourceTitle;

        private String content;
    }
}