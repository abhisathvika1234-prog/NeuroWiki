package com.neurowiki.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiQuestionRequest {

    @NotBlank(message = "Question cannot be empty")
    private String question;
}
