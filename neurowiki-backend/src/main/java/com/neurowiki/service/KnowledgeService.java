package com.neurowiki.service;

import com.neurowiki.dto.KnowledgeRequest;
import com.neurowiki.dto.KnowledgeResponse;
import com.neurowiki.entity.KnowledgePage;
import com.neurowiki.entity.User;
import com.neurowiki.exception.ResourceNotFoundException;
import com.neurowiki.repository.KnowledgePageRepository;
import com.neurowiki.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    private final KnowledgePageRepository knowledgePageRepository;
    private final SecurityUtils securityUtils;
    private final RagService ragService;
    private final GraphService graphService;

    public KnowledgeService(KnowledgePageRepository knowledgePageRepository,
                            SecurityUtils securityUtils,
                            RagService ragService,
                            GraphService graphService) {
        this.knowledgePageRepository = knowledgePageRepository;
        this.securityUtils = securityUtils;
        this.ragService = ragService;
        this.graphService = graphService;
    }

    public List<KnowledgeResponse> getAllKnowledge(String category, Boolean favorite) {
        User user = securityUtils.getCurrentAuthenticatedUser();
        List<KnowledgePage> pages;

        if (Boolean.TRUE.equals(favorite)) {
            pages = knowledgePageRepository.findByUserAndFavoriteTrueOrderByUpdatedAtDesc(user);
        } else if (category != null && !category.isBlank()) {
            pages = knowledgePageRepository.findByUserAndCategoryOrderByUpdatedAtDesc(user, category.trim());
        } else {
            pages = knowledgePageRepository.findByUserOrderByUpdatedAtDesc(user);
        }

        return pages.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public KnowledgeResponse getKnowledgeById(Long id) {
        User user = securityUtils.getCurrentAuthenticatedUser();
        KnowledgePage page = knowledgePageRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge page not found with ID: " + id));
        return mapToResponse(page);
    }

    @Transactional
    public KnowledgeResponse createKnowledge(KnowledgeRequest request) {
        User user = securityUtils.getCurrentAuthenticatedUser();

        KnowledgePage page = KnowledgePage.builder()
                .title(request.getTitle().trim())
                .content(request.getContent())
                .category(request.getCategory() != null ? request.getCategory().trim() : "General")
                .tags(request.getTags() != null ? request.getTags().trim() : "")
                .favorite(request.isFavorite())
                .user(user)
                .build();

        KnowledgePage saved = knowledgePageRepository.save(page);

        // Process for RAG vector search & Knowledge Graph
        ragService.processAndChunkContent(user, "KNOWLEDGE", saved.getId(), saved.getTitle(), saved.getContent());
        graphService.processAndExtractGraph(user, "KNOWLEDGE", saved.getId(), saved.getTitle(), saved.getContent());

        return mapToResponse(saved);
    }

    @Transactional
    public KnowledgeResponse updateKnowledge(Long id, KnowledgeRequest request) {
        User user = securityUtils.getCurrentAuthenticatedUser();
        KnowledgePage page = knowledgePageRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge page not found with ID: " + id));

        page.setTitle(request.getTitle().trim());
        page.setContent(request.getContent());
        if (request.getCategory() != null) {
            page.setCategory(request.getCategory().trim());
        }
        if (request.getTags() != null) {
            page.setTags(request.getTags().trim());
        }
        page.setFavorite(request.isFavorite());

        KnowledgePage updated = knowledgePageRepository.save(page);

        // Re-process RAG vector search & Knowledge Graph
        ragService.processAndChunkContent(user, "KNOWLEDGE", updated.getId(), updated.getTitle(), updated.getContent());
        graphService.processAndExtractGraph(user, "KNOWLEDGE", updated.getId(), updated.getTitle(), updated.getContent());

        return mapToResponse(updated);
    }

    public KnowledgeResponse toggleFavorite(Long id) {
        User user = securityUtils.getCurrentAuthenticatedUser();
        KnowledgePage page = knowledgePageRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge page not found with ID: " + id));

        page.setFavorite(!page.isFavorite());
        KnowledgePage updated = knowledgePageRepository.save(page);
        return mapToResponse(updated);
    }

    public void deleteKnowledge(Long id) {
        User user = securityUtils.getCurrentAuthenticatedUser();
        KnowledgePage page = knowledgePageRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge page not found with ID: " + id));

        knowledgePageRepository.delete(page);
    }

    public KnowledgeResponse mapToResponse(KnowledgePage page) {
        return KnowledgeResponse.builder()
                .id(page.getId())
                .title(page.getTitle())
                .content(page.getContent())
                .category(page.getCategory())
                .tags(page.getTags())
                .favorite(page.isFavorite())
                .createdAt(page.getCreatedAt())
                .updatedAt(page.getUpdatedAt())
                .build();
    }
}
