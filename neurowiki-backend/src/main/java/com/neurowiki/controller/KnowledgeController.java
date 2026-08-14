package com.neurowiki.controller;

import com.neurowiki.dto.KnowledgeRequest;
import com.neurowiki.dto.KnowledgeResponse;
import com.neurowiki.service.KnowledgeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    public ResponseEntity<List<KnowledgeResponse>> getAllKnowledge(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean favorite) {
        List<KnowledgeResponse> response = knowledgeService.getAllKnowledge(category, favorite);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeResponse> getKnowledgeById(@PathVariable Long id) {
        KnowledgeResponse response = knowledgeService.getKnowledgeById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<KnowledgeResponse> createKnowledge(@Valid @RequestBody KnowledgeRequest request) {
        KnowledgeResponse response = knowledgeService.createKnowledge(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeResponse> updateKnowledge(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeRequest request) {
        KnowledgeResponse response = knowledgeService.updateKnowledge(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/favorite")
    public ResponseEntity<KnowledgeResponse> toggleFavorite(@PathVariable Long id) {
        KnowledgeResponse response = knowledgeService.toggleFavorite(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return ResponseEntity.ok(Map.of("message", "Knowledge page deleted successfully"));
    }
}
