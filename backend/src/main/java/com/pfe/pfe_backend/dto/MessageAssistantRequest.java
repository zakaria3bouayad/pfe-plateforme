package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corps de POST /api/assistant/messages (Lot 8, etape 8.8). */
public record MessageAssistantRequest(

        @NotBlank(message = "La question est obligatoire")
        @Size(max = 2000, message = "La question est limitee a 2000 caracteres")
        String question
) {}
