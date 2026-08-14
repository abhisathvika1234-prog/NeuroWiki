package com.neurowiki.controller;

import com.neurowiki.dto.SearchResponse;
import com.neurowiki.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<SearchResponse> search(@RequestParam(value = "q", required = false) String query) {
        SearchResponse response = searchService.search(query);
        return ResponseEntity.ok(response);
    }
}
