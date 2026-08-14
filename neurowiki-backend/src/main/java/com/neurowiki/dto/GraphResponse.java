package com.neurowiki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphResponse {
    private List<NodeDto> nodes;
    private List<EdgeDto> edges;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeDto {
        private String id;
        private String label;
        private String type; // CONCEPT, KNOWLEDGE, PDF
        private Long sourceId;
        private String sourceType; // KNOWLEDGE, PDF
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeDto {
        private String id;
        private String source;
        private String target;
        private String relationship; // USES, CONTAINS, RELATES_TO, EXTENDS
    }
}
