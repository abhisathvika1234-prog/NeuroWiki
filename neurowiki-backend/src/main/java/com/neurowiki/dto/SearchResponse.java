package com.neurowiki.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {
    private List<KnowledgeResponse> knowledge;
    private List<DocumentResponse> documents;
}
