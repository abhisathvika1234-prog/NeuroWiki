package com.neurowiki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResponse {
    private Long id;
    private String question;
    private String answer;
    private List<RagSourceDto> sources;
    private LocalDateTime timestamp;
    private boolean serviceConfigured;
}
