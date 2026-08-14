package com.neurowiki.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {
    private Long id;
    private String title;
    private String type;
    private String content;
    private String sourceUrl;
    private String status;
    private String fileSize;
    private LocalDateTime addedAt;
    private int conceptsExtractedCount;
}
