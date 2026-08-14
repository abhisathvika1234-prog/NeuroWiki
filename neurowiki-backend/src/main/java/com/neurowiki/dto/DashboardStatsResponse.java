package com.neurowiki.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    private long knowledgeCount;
    private long documentCount;
    private long favoritesCount;
    private long aiQuestionsCount;
    private List<KnowledgeResponse> recentKnowledge;
    private List<DocumentResponse> recentDocuments;
}
