package com.neurowiki.service;

import com.neurowiki.dto.DocumentResponse;
import com.neurowiki.dto.KnowledgeResponse;
import com.neurowiki.dto.SearchResponse;
import com.neurowiki.entity.KnowledgeDocument;
import com.neurowiki.entity.KnowledgePage;
import com.neurowiki.entity.User;
import com.neurowiki.repository.KnowledgeDocumentRepository;
import com.neurowiki.repository.KnowledgePageRepository;
import com.neurowiki.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final KnowledgePageRepository knowledgePageRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final SecurityUtils securityUtils;
    private final KnowledgeService knowledgeService;
    private final DocumentService documentService;

    public SearchService(KnowledgePageRepository knowledgePageRepository,
                         KnowledgeDocumentRepository knowledgeDocumentRepository,
                         SecurityUtils securityUtils,
                         KnowledgeService knowledgeService,
                         DocumentService documentService) {
        this.knowledgePageRepository = knowledgePageRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.securityUtils = securityUtils;
        this.knowledgeService = knowledgeService;
        this.documentService = documentService;
    }

    public SearchResponse search(String query) {
        User user = securityUtils.getCurrentAuthenticatedUser();

        if (query == null || query.trim().isBlank()) {
            return new SearchResponse(Collections.emptyList(), Collections.emptyList());
        }

        String q = query.trim();
        List<KnowledgePage> pages = knowledgePageRepository.searchByUserAndQuery(user, q);
        List<KnowledgeDocument> docs = knowledgeDocumentRepository.searchByUserAndQuery(user, q);

        List<KnowledgeResponse> knowledgeResponses = pages.stream()
                .map(knowledgeService::mapToResponse)
                .collect(Collectors.toList());

        List<DocumentResponse> documentResponses = docs.stream()
                .map(documentService::mapToResponse)
                .collect(Collectors.toList());

        return new SearchResponse(knowledgeResponses, documentResponses);
    }
}
