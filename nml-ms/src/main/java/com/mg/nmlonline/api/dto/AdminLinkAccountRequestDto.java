package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLinkAccountRequestDto(
        @NotBlank @Size(max = 50) String username) {
}
