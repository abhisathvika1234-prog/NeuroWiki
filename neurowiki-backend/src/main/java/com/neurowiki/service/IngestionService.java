package com.neurowiki.service;

import com.neurowiki.dto.DocumentResponse;
import com.neurowiki.dto.IngestionRequest;
import com.neurowiki.entity.KnowledgeDocument;
import com.neurowiki.entity.User;
import com.neurowiki.exception.BadRequestException;
import com.neurowiki.repository.KnowledgeDocumentRepository;
import com.neurowiki.security.SecurityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Service
public class IngestionService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final SecurityUtils securityUtils;
    private final DocumentService documentService;
    private final RagService ragService;
    private final GraphService graphService;

    public IngestionService(
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            SecurityUtils securityUtils,
            DocumentService documentService,
            RagService ragService,
            GraphService graphService
    ) {
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.securityUtils = securityUtils;
        this.documentService = documentService;
        this.ragService = ragService;
        this.graphService = graphService;
    }

    @Transactional
    public DocumentResponse ingest(IngestionRequest request) {

        if (request == null) {
            throw new BadRequestException("Ingestion request is required");
        }

        User user =
                securityUtils.getCurrentAuthenticatedUser();

        String type =
                request.getSourceType() != null
                        ? request.getSourceType().trim().toUpperCase()
                        : "TEXT";

        String content =
                request.getContent() != null
                        ? request.getContent().trim()
                        : "";

        String url =
                request.getUrl() != null
                        ? request.getUrl().trim()
                        : null;

        String title =
                request.getTitle() != null &&
                        !request.getTitle().trim().isBlank()
                        ? request.getTitle().trim()
                        : "Untitled Document";

        /*
         * URL validation.
         */
        if ("URL".equals(type)) {

            if (url == null || url.isBlank()) {
                throw new BadRequestException(
                        "URL is required for URL source type"
                );
            }

            /*
             * Fetch the actual webpage.
             */
            content = fetchWebPageContent(url);

            if (content == null || content.isBlank()) {

                throw new BadRequestException(
                        "Could not extract readable content from the URL: "
                                + url
                );
            }
        }

        /*
         * Normal text ingestion.
         */
        if (!"URL".equals(type) &&
                content.isBlank()) {

            throw new BadRequestException(
                    "Content is required"
            );
        }

        /*
         * Calculate metadata AFTER actual webpage
         * content has been extracted.
         */
        int conceptsCount =
                extractConceptsCount(content);

        String sizeLabel =
                formatContentSize(content);

        /*
         * Save the actual content in KnowledgeDocument.
         */
        KnowledgeDocument document =
                KnowledgeDocument.builder()
                        .title(title)
                        .type(type)
                        .content(content)
                        .sourceUrl(url)
                        .status("PROCESSED")
                        .fileSize(sizeLabel)
                        .conceptsExtractedCount(conceptsCount)
                        .user(user)
                        .build();

        KnowledgeDocument saved =
                knowledgeDocumentRepository.save(document);

        /*
         * Send the REAL webpage content to RAG.
         *
         * This is the important part.
         */
        ragService.processAndChunkContent(
                user,
                type,
                saved.getId(),
                saved.getTitle(),
                content
        );

        /*
         * Send the REAL webpage content to
         * Knowledge Graph extraction.
         */
        graphService.processAndExtractGraph(
                user,
                type,
                saved.getId(),
                saved.getTitle(),
                content
        );

        return documentService.mapToResponse(saved);
    }

    /**
     * Downloads a webpage and extracts readable text.
     *
     * Uses Jsoup to:
     *
     * URL
     *   ↓
     * HTML
     *   ↓
     * Remove scripts/styles
     *   ↓
     * Find article content
     *   ↓
     * Return clean text
     */
    private String fetchWebPageContent(String url) {

        try {

            if (!url.startsWith("http://") &&
                    !url.startsWith("https://")) {

                throw new BadRequestException(
                        "URL must start with http:// or https://"
                );
            }

            System.out.println(
                    "Fetching webpage: " + url
            );

            Document document =
                    Jsoup.connect(url)
                            .userAgent(
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                            + "AppleWebKit/537.36 "
                                            + "(KHTML, like Gecko) "
                                            + "Chrome/151.0.0.0 Safari/537.36"
                            )
                            .referrer("https://www.google.com/")
                            .timeout(30000)
                            .followRedirects(true)
                            .get();

            /*
             * Remove elements that don't contain
             * useful knowledge for RAG.
             */
            document.select(
                    "script, style, noscript, iframe, "
                            + "svg, canvas, nav, footer, header"
            ).remove();

            /*
             * Try to find the main article.
             *
             * GFG and many educational websites use
             * article/main/content containers.
             */
            Element article =
                    document.selectFirst(
                            "article"
                    );

            String text;

            if (article != null &&
                    !article.text().isBlank()) {

                text = article.text();

            } else {

                Element main =
                        document.selectFirst(
                                "main"
                        );

                if (main != null &&
                        !main.text().isBlank()) {

                    text = main.text();

                } else {

                    /*
                     * Fallback:
                     * use the entire body.
                     */
                    text =
                            document.body() != null
                                    ? document.body().text()
                                    : "";
                }
            }

            /*
             * Clean excessive whitespace.
             */
            text =
                    text
                            .replaceAll(
                                    "\\s+",
                                    " "
                            )
                            .trim();

            /*
             * Add page title at the beginning.
             */
            String pageTitle =
                    document.title() != null
                            ? document.title().trim()
                            : "";

            if (!pageTitle.isBlank()) {

                text =
                        "Page Title: "
                                + pageTitle
                                + "\n\n"
                                + text;
            }

            System.out.println(
                    "Successfully extracted "
                            + text.length()
                            + " characters from URL."
            );

            return text;

        } catch (IOException e) {

            throw new BadRequestException(
                    "Failed to fetch webpage: "
                            + e.getMessage()
            );

        } catch (BadRequestException e) {

            throw e;

        } catch (Exception e) {

            throw new BadRequestException(
                    "Failed to process URL: "
                            + e.getMessage()
            );
        }
    }

    /**
     * Counts unique meaningful words as a simple
     * concept estimate.
     */
    private int extractConceptsCount(String text) {

        if (text == null ||
                text.isBlank()) {

            return 0;
        }

        String cleaned =
                text
                        .replaceAll(
                                "[^a-zA-Z0-9\\s]",
                                " "
                        )
                        .toLowerCase();

        String[] words =
                cleaned.split("\\s+");

        Set<String> uniqueWords =
                new HashSet<>();

        for (String word : words) {

            if (word.length() > 3) {
                uniqueWords.add(word);
            }
        }

        return uniqueWords.size();
    }

    /**
     * Calculates approximate content size.
     */
    private String formatContentSize(
            String content
    ) {

        if (content == null) {
            return "0 KB";
        }

        int bytes =
                content.getBytes()
                        .length;

        if (bytes < 1024) {
            return bytes + " B";
        }

        return (bytes / 1024) + " KB";
    }
}