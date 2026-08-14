package com.neurowiki.controller;

import com.neurowiki.dto.DocumentResponse;
import com.neurowiki.dto.IngestionRequest;
import com.neurowiki.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> ingest(@Valid @RequestBody IngestionRequest request) {
        DocumentResponse response = ingestionService.ingest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
