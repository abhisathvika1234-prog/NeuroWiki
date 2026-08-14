package com.neurowiki.service;

import com.neurowiki.dto.DashboardStatsResponse;
import com.neurowiki.dto.DocumentResponse;
import com.neurowiki.dto.KnowledgeResponse;
import com.neurowiki.entity.KnowledgeDocument;
import com.neurowiki.entity.KnowledgePage;
import com.neurowiki.entity.User;
import com.neurowiki.repository.AiHistoryRepository;
import com.neurowiki.repository.KnowledgeDocumentRepository;
import com.neurowiki.repository.KnowledgePageRepository;
import com.neurowiki.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final KnowledgePageRepository knowledgePageRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final AiHistoryRepository aiHistoryRepository;
    private final SecurityUtils securityUtils;
    private final KnowledgeService knowledgeService;
    private final DocumentService documentService;

    public DashboardService(KnowledgePageRepository knowledgePageRepository,
                            KnowledgeDocumentRepository knowledgeDocumentRepository,
                            AiHistoryRepository aiHistoryRepository,
                            SecurityUtils securityUtils,
                            KnowledgeService knowledgeService,
                            DocumentService documentService) {
        this.knowledgePageRepository = knowledgePageRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.aiHistoryRepository = aiHistoryRepository;
        this.securityUtils = securityUtils;
        this.knowledgeService = knowledgeService;
        this.documentService = documentService;
    }

    public DashboardStatsResponse getStats() {
        User user = securityUtils.getCurrentAuthenticatedUser();

        long knowledgeCount = knowledgePageRepository.countByUser(user);
        long documentCount = knowledgeDocumentRepository.countByUser(user);
        long favoritesCount = knowledgePageRepository.countByUserAndFavoriteTrue(user);
        long aiQuestionsCount = aiHistoryRepository.countByUser(user);

        List<KnowledgePage> recentKnowledgeList = knowledgePageRepository.findTop5ByUserOrderByUpdatedAtDesc(user);
        List<KnowledgeDocument> recentDocumentList = knowledgeDocumentRepository.findTop5ByUserOrderByAddedAtDesc(user);

        List<KnowledgeResponse> recentKnowledge = recentKnowledgeList.stream()
                .map(knowledgeService::mapToResponse)
                .collect(Collectors.toList());

        List<DocumentResponse> recentDocuments = recentDocumentList.stream()
                .map(documentService::mapToResponse)
                .collect(Collectors.toList());

        return DashboardStatsResponse.builder()
                .knowledgeCount(knowledgeCount)
                .documentCount(documentCount)
                .favoritesCount(favoritesCount)
                .aiQuestionsCount(aiQuestionsCount)
                .recentKnowledge(recentKnowledge)
                .recentDocuments(recentDocuments)
                .build();
    }
}
