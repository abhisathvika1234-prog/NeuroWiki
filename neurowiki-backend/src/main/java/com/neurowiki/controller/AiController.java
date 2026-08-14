package com.neurowiki.controller;

import com.neurowiki.dto.AiQuestionRequest;
import com.neurowiki.dto.AiResponse;
import com.neurowiki.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/ask")
    public ResponseEntity<AiResponse> askQuestion(@Valid @RequestBody AiQuestionRequest request) {
        AiResponse response = aiService.askQuestion(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<AiResponse>> getHistory() {
        List<AiResponse> response = aiService.getHistory();
        return ResponseEntity.ok(response);
    }
}
