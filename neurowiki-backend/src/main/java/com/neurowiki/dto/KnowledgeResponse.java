package com.neurowiki.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeResponse {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String tags;
    private boolean favorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
