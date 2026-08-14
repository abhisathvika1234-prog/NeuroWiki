package com.neurowiki.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String content;
    private String url;

    @NotBlank(message = "Source type is required")
    private String sourceType; // URL, TEXT
}
