package com.neurowiki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSourceDto {
    private String type; // KNOWLEDGE, PDF, URL, TEXT
    private String title;
    private Long id;
}
