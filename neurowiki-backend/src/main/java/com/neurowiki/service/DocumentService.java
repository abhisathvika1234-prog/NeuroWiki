package com.neurowiki.service;

import com.neurowiki.dto.DocumentResponse;
import com.neurowiki.entity.KnowledgeDocument;
import com.neurowiki.entity.User;
import com.neurowiki.exception.ResourceNotFoundException;
import com.neurowiki.repository.KnowledgeDocumentRepository;
import com.neurowiki.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final SecurityUtils securityUtils;

    public DocumentService(KnowledgeDocumentRepository knowledgeDocumentRepository, SecurityUtils securityUtils) {
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.securityUtils = securityUtils;
    }

    public List<DocumentResponse> getAllDocuments() {
        User user = securityUtils.getCurrentAuthenticatedUser();
        List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByUserOrderByAddedAtDesc(user);
        return documents.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public DocumentResponse getDocumentById(Long id) {
        User user = securityUtils.getCurrentAuthenticatedUser();
        KnowledgeDocument document = knowledgeDocumentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));
        return mapToResponse(document);
    }

    public void deleteDocument(Long id) {
        User user = securityUtils.getCurrentAuthenticatedUser();
        KnowledgeDocument document = knowledgeDocumentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));

        knowledgeDocumentRepository.delete(document);
    }

    public DocumentResponse mapToResponse(KnowledgeDocument doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .type(doc.getType())
                .content(doc.getContent())
                .sourceUrl(doc.getSourceUrl())
                .status(doc.getStatus())
                .fileSize(doc.getFileSize())
                .addedAt(doc.getAddedAt())
                .conceptsExtractedCount(doc.getConceptsExtractedCount())
                .build();
    }
}
