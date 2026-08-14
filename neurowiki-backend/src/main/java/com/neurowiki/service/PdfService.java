package com.neurowiki.service;

import com.neurowiki.dto.DocumentResponse;
import com.neurowiki.entity.KnowledgeDocument;
import com.neurowiki.entity.User;
import com.neurowiki.exception.BadRequestException;
import com.neurowiki.repository.KnowledgeDocumentRepository;
import com.neurowiki.security.SecurityUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Service
public class PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final SecurityUtils securityUtils;
    private final DocumentService documentService;
    private final RagService ragService;
    private final GraphService graphService;

    public PdfService(KnowledgeDocumentRepository knowledgeDocumentRepository,
                      SecurityUtils securityUtils,
                      DocumentService documentService,
                      RagService ragService,
                      GraphService graphService) {
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.securityUtils = securityUtils;
        this.documentService = documentService;
        this.ragService = ragService;
        this.graphService = graphService;
    }

    @Transactional
    public DocumentResponse uploadPdf(MultipartFile file) {
        User user = securityUtils.getCurrentAuthenticatedUser();

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a PDF file to upload");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Uploaded file must be a valid PDF format (.pdf)");
        }

        String extractedText;
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            extractedText = stripper.getText(document);
        } catch (IOException e) {
            logger.error("Failed to parse PDF document {}: {}", originalFilename, e.getMessage());
            throw new BadRequestException("PDF processing failed: Could not extract content from the uploaded file");
        }

        int conceptsCount = extractConceptsCount(extractedText);
        String sizeLabel = formatFileSize(file.getSize());

        KnowledgeDocument doc = KnowledgeDocument.builder()
                .title(originalFilename)
                .type("PDF")
                .content(extractedText)
                .sourceUrl(null)
                .status("PROCESSED")
                .fileSize(sizeLabel)
                .conceptsExtractedCount(conceptsCount)
                .user(user)
                .build();

        KnowledgeDocument saved = knowledgeDocumentRepository.save(doc);

        // Process PDF content for RAG vector search & Knowledge Graph
        ragService.processAndChunkContent(user, "PDF", saved.getId(), saved.getTitle(), extractedText);
        graphService.processAndExtractGraph(user, "PDF", saved.getId(), saved.getTitle(), extractedText);

        logger.info("PDF uploaded and processed successfully: ID={}, title={}", saved.getId(), saved.getTitle());
        return documentService.mapToResponse(saved);
    }

    private int extractConceptsCount(String text) {
        if (text == null || text.isBlank()) return 0;
        String cleaned = text.replaceAll("[^a-zA-Z0-9\\s]", " ").toLowerCase();
        String[] words = cleaned.split("\\s+");
        Set<String> uniqueWords = new HashSet<>();
        for (String w : words) {
            if (w.length() > 3) {
                uniqueWords.add(w);
            }
        }
        return uniqueWords.size();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.2f MB", (double) bytes / (1024 * 1024));
    }
}
