package com.neurowiki.controller;

import com.neurowiki.dto.GraphResponse;
import com.neurowiki.service.GraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    /**
     * Get the complete graph for the current user.
     *
     * GET /api/graph
     */
    @GetMapping
    public ResponseEntity<GraphResponse> getGraph() {

        GraphResponse response =
                graphService.getGraph();

        return ResponseEntity.ok(response);
    }

    /**
     * Get graph for one specific source.
     *
     * Example:
     *
     * GET /api/graph/PDF/15
     * GET /api/graph/URL/20
     * GET /api/graph/KNOWLEDGE/30
     */
    @GetMapping("/{sourceType}/{sourceId}")
    public ResponseEntity<GraphResponse> getGraphBySource(
            @PathVariable String sourceType,
            @PathVariable Long sourceId
    ) {

        GraphResponse response =
                graphService.getGraphBySource(
                        sourceType,
                        sourceId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Delete graph for one source.
     *
     * Example:
     *
     * DELETE /api/graph/PDF/15
     */
    @DeleteMapping("/{sourceType}/{sourceId}")
    public ResponseEntity<Void> deleteGraphBySource(
            @PathVariable String sourceType,
            @PathVariable Long sourceId
    ) {

        graphService.deleteGraphBySource(
                sourceType,
                sourceId
        );

        return ResponseEntity.noContent().build();
    }
}