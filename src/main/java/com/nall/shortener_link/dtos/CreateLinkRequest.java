package com.nall.shortener_link.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLinkRequest(
        @NotBlank(message = "A URL é obrigatória")
        @Size(max = 2048, message = "A URL deve ter no máximo 2048 caracteres")
        @Pattern(regexp = "^https?://\\S+$", message = "A URL deve começar com http:// ou https://")
        String url
) {
}
